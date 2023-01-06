package us.bloch.pool.hardware;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.stream.Collectors.toMap;
import static us.bloch.pool.hardware.PentairBusCodes.Station.isPump;

/**
 * A programmatic interface to the Pentair swimming pool automation bus. This RS-485 bus is
 * shared by a controller (such as the Suntouch) and compatible devices such as the Intelliflo
 * pump and the QuickTouch remote control transceiver. This class allows for packet monitoring
 * and injection on the bus, which in turn allows for monitoring and control of the connected
 * pool system (pool, pumps, lights, an so on).  The architecture of this class and its
 * associated message hierarchy is extensible, but it currently supports the devices that we have
 * in our pool system, and will likely need to be extended if it is to work for a more complex
 * system.
 *
 * <p>This class is thread-safe, subject to the limitations of the underlying hardware: Putting a
 * message on the bus with {@link #putMessage} does not guarantee that the message will be
 * received; messages can collide on the bus, causing them to be lost.
 *
 * @author Josh Bloch
 */
public class PentairBus {
    /** The Pentair bus baud rate is 9600. */
    private static final int BUS_BAUD_RATE = 9600;

    /** Serial port for the pool controller's RS-485 bus. */
    private final SerialPort busPort;

    /**
     * Input stream for pool controller's RS-485 bus. This is a BufferedOutputStream wrapping
     * busPort.inputStream().
     */
    private final InputStream busIn;

    /** The subscribers to the messages on this Pentair bus. */
    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    /** Checksum of the message being read. Maintained by getHeader, nextByte, and nextBytes. */
    private int checksum;

    /** The last message read from the pool controller's RS-485 bus. Protected by lastMessageLock. */
    private Message lastMessage;
    private final Object lastMessageLock = new Object();

    /**
     * Creates a PentairBus instance for the serialPort with the given name. This constructor
     * starts a single daemon thread to monitor the communication bus for traffic and parse the
     * traffic into messages. This is the only thread that reads from the serial port.
     *
     * <p>Attempting to use multiple concurrent instances of this class atop the same serial
     * port, whether in a single process or multiple processes, is not supported.
     *
     * @throws IOException if the named serial port cannot be opened
     */
    public PentairBus(String portName) throws IOException {
        busPort = new SerialPort(portName, BUS_BAUD_RATE);
        busIn = new BufferedInputStream(busPort.inputStream());
        Thread busMonitor = new Thread(() -> {
            while(true) {
                try {
                    Message msg = getMessageFromBus();

                    // Make message available to any threads waiting in getMessage()
                    synchronized(lastMessageLock) {
                        lastMessage = msg;
                        lastMessageLock.notifyAll();
                    }

                    // Deliver message to all subscribers
                    for (Subscription subscription : subscriptions)
                        subscription.messageQueue.add(msg);
                } catch (IOException e) {
                    System.err.println("IO error when attempting to read message from bus: " + e);
                }
            }
        }, "us.bloch.pool.PentairBus RS-485 bus monitor (serial port " + portName + ")");
        busMonitor.setDaemon(true);
        busMonitor.start();
    }

    /**
     * Subscribes to the bus and returns a subscription, which encapsulates a blocking queue to
     * which all messages on the bus will be delivered as they become available. Messages will be
     * delivered to the queue until the subscription is closed. Messages sent by this {@code
     * PentairBus} instance (via {@link #putMessage(Message)}) will <it>not</it> be delivered as
     * part of the subscription.
     */
    public Subscription subscribe() {
        return new Subscription();
    }

    /**
     * Gets the next message from the pool controller, waiting as necessary.
     */
    public Message getMessage() {
        Message previousMessage = lastMessage;
        Message result;

        // Wait for lastMessage to change, and return new value
        synchronized(lastMessageLock) {
            while((result = lastMessage) == previousMessage) {
                try {
                    lastMessageLock.wait();
                } catch (InterruptedException e) {
                    System.err.println("Interrupt ignored on Pentair bus monitor thread: " + e);
                }
            }
            return result;
        }
    }

    /** Gets a message from the RS-485 bus, waiting if necessary. */
    private Message getMessageFromBus() throws IOException {
        while(true) {
            getHeader();

            byte ver = nextByte();
            byte dst = nextByte();
            byte src = nextByte();
            byte cmd = nextByte();

            int dataLen = toInt(nextByte());
            if (dataLen >= 30) {
                System.err.printf(
                        "%s: Pentair message data length too high: %d, abandoning message%n",
                        LocalTime.now(), dataLen);
                System.err.printf(">> Ver: %02x, Dst: %02x, Src: %02x, Cmd: %02x, DataLen: %02x%n",
                        ver, dst, src, cmd, dataLen);
                continue;
            }
            byte[] data = nextBytes(dataLen);

            int actualChecksum = checksum;
            int expectedChecksum = toInt(nextByte(), nextByte());
            if (actualChecksum != expectedChecksum) {
                System.err.printf(
                      "%s: Checksum error reading message from Pentair bus. Expecting %d, got %d%n",
                      LocalTime.now(), expectedChecksum, actualChecksum);
                System.err.printf(">> Ver: %02x, Dst: %02x, Src: %02x, Cmd: %02x, Data: %s%n",
                                  ver, dst, src, cmd, toHexString(data));
                continue;
            }

            return Message.newInstance(ver, dst, src, cmd, data);
        }
    }

    /**
     * Returns the next byte from the RS-485 bus, waiting if necessary, and updates checksum.
     *
     * @throws IOException if an error is encountered attempting to read a byte from the bus
     */
    private byte nextByte() throws IOException {
        int result = busIn.read();
        if (result < 0) {
            throw new IOException("EOF on serial port");
        } else {
            checksum += result;
            return (byte) result;
        }
    }

    /**
     * Returns the specified number of bytes from the RS-485 bus and updates checksum.  This
     * method assumes that the specified number of bytes are already available on the serial port
     * (without blocking).
     *
     * @throws IOException the specified number of bytes weren't available
     */
    private byte[] nextBytes(int numBytes) throws IOException {
        byte[] result = new byte[numBytes];

        int justGot;
        for(int got = 0; got != numBytes; got += justGot) {
            justGot = busIn.read(result, got, numBytes - got);
            if (justGot == -1 || justGot == 0) {
                throw new IOException(
                        String.format("EOF on serial port after %d bytes, expecting %d",
                                      got, numBytes));
            }
        }

        for (byte b : result)
            checksum += toInt(b);

        return result;
    }

    /** The three-byte header that we look for when receiving messages from the bus. */
    private static final byte[] RCV_HEADER = new byte[]{ 0x00, (byte) 0xff, (byte) 0xa5 };

    /**
     * The headers that we generate when sending messages to the bus. Note that this has an
     * additional byte when compared to RCV_HDR, a leading 0xff byte. Some devices (such as the
     * Intelliflo pump) are unable to receive messages unless they have this 0xff byte at the
     * beginning of the header. Per Postel's principle, we accept a minimal header, which
     * accommodates all writing devices, but we generate a full four-byte header, which satisfies
     * all reading devices.
     */
    private static final byte[] SEND_HEADER = new byte[]{ (byte) 0xff, 0x00, (byte) 0xff, (byte) 0xa5 };

    /** The checksum value for the header. (Checksum computation starts at final byte of header.) */
    private static final int HEADER_CHECKSUM = 0xa5;

    /**
     * Consumes bytes from the bus until header is seen, and initializes the checksum for the
     * message in progress.
     */
    private void getHeader() throws IOException {
        int i = 0;
        while(i < RCV_HEADER.length) {
            byte b = nextByte();
            if (b == RCV_HEADER[i]) {
                i++;
            } else if (b == RCV_HEADER[0]) {
                i = 1;
            } else {
                i = 0;
            }
        }

        checksum = HEADER_CHECKSUM;
    }

    /**
     * Puts the specified message on the Pentair bus.
     *
     * @throws IOException if an error occurs while attempting to write message to serial port.
     */
    public synchronized void putMessage(Message msg) throws IOException {
        byte[] rawMsg = new byte[SEND_HEADER.length + 5 + msg.data.length + 2];
        System.arraycopy(SEND_HEADER, 0, rawMsg, 0, SEND_HEADER.length);
        int cursor = SEND_HEADER.length;
        rawMsg[cursor++] = msg.proto;
        rawMsg[cursor++] = msg.dst;
        rawMsg[cursor++] = msg.src;
        rawMsg[cursor++] = msg.cmd;
        rawMsg[cursor++] = (byte) msg.data.length;
        System.arraycopy(msg.data, 0, rawMsg, cursor, msg.data.length);
        cursor += msg.data.length;

        int checksum = 0;
        for(int i = SEND_HEADER.length - 1; i < cursor; ++i)
            checksum += toInt(rawMsg[i]);
        rawMsg[cursor++] = (byte) (checksum >> 8);
        rawMsg[cursor++] = (byte) checksum;

        assert cursor == rawMsg.length;
        busPort.outputStream().write(rawMsg);
    }

    // Enum types representing information contained in messages, and associated methods & constants

    /** A Physical or virtual circuit managed by the pool controller. */
    public enum Circuit {
        POOL((byte) 0x06),
        SPA((byte) 0x01),
        AUX1((byte) 0x02),
        AUX2((byte) 0x03),
        AUX3((byte) 0x04),
        FEATURE1((byte) 0x05),
        FEATURE2((byte) 0x07),
        FEATURE3((byte) 0x08),
        FEATURE4((byte) 0x09),
        HEAT_BOOST((byte) 0x85),
        HEAT_ENABLE((byte) 0x86);

        /** A de facto constant representing all of the auxiliary circuits. */
        static final Set<Circuit> AUX_CIRCUITS = EnumSet.of(Circuit.AUX1, Circuit.AUX2, Circuit.AUX3);

        /** A de facto constant representing all of the feature circuits. */
        static final Set<Circuit> FEATURE_CIRCUITS =
                EnumSet.of(Circuit.FEATURE1, Circuit.FEATURE2,Circuit.FEATURE3, Circuit.FEATURE4);

        /** The number used by the controller to represent this circuit. */
        private final byte number;

        Circuit(byte number) {
            this.number = number;
        }

        /** Returns the byte code used by the controller to represent this circuit. */
        public byte code() {
            return number;
        }

        /** A map from code to circuit. */
        private static final Map<Byte, Circuit> codeToCircuit
                = Arrays.stream(values()).collect(toMap(Circuit::code, circuit -> circuit));

        /** Returns the circuit with the specified code, or null if the code is invalid. */
        public static Circuit forCode(byte code) {
            return codeToCircuit.get(code);
        }
    }

    /** Returns the circuits represented by the given bytes from the status message. */
    static Set<Circuit> circuits(int byte2, int byte3) {
        Set<Circuit> result = EnumSet.noneOf(Circuit.class);
        if ((byte2 & 0x20) != 0)
            result.add(Circuit.POOL);
        if ((byte2 & 1) != 0)
            result.add(Circuit.SPA);
        if ((byte2 & 2) != 0)
            result.add(Circuit.AUX1);
        if ((byte2 & 4) != 0)
            result.add(Circuit.AUX2);
        if ((byte2 & 8) != 0)
            result.add(Circuit.AUX3);
        if ((byte2 & 0x10) != 0)
            result.add(Circuit.FEATURE1);
        if ((byte2 & 0x40) != 0)
            result.add(Circuit.FEATURE2);
        if ((byte2 & 0x80) != 0)
            result.add(Circuit.FEATURE3);
        if ((byte3 & 0x01) != 0)
            result.add(Circuit.FEATURE4);
        return result;
    }

    /** The state of a powered circuit, either on or off. */
    public enum CircuitPowerState {
        /** The circuit is switched on. */
        ON((byte)  0b1),

        /** The circuit is switched off. */
        OFF((byte) 0b0);

        private final byte code;

        CircuitPowerState(byte code) {
            this.code = code;
        }

        /**
         * Returns the Pentair byte code for this circuit power state. (As you'd expect, OFF is 0,
         * ON is 1.)
         */
        public byte code() {
            return code;
        }

        /** Returns the circuit power state for the specified Pentair code. */
        static CircuitPowerState forCode(byte code) {
            if (code != 0 && code != 1) {
                throw new IllegalArgumentException(String.format("Illegal circuit state code: %x", code));
            } else {
                return code == 0 ? OFF : ON;
            }
        }
    }


    /**
     * A heat source (or {@link HeatSource#UNHEATED}, which represents the absence of a heat
     * source).
     */
    public enum HeatSource {
        /** No heat. */
        UNHEATED(0),

        /** The gas (or electric) heater. */
        HEATER(1),

        /** The solar heater if solar temperature is high enough, otherwise gas (or electric). */
        SOLAR_PREF(2),

        /** The solar heater, or no heat if the solar temperature is not high enough. */
        SOLAR(3);

        private final int code;

        HeatSource(int code) {
            this.code = code;
        }

        /**
         * Returns the heat source corresponding to the specified code (which occupies the two
         * low-order bits of the given int).
         */
        static HeatSource poolSourceFromCode(int heatSourceCode) {
            switch(heatSourceCode & 0b11) {
                case 0:  return UNHEATED;
                case 1:  return HEATER;
                case 2:  return SOLAR_PREF;
                case 3:  return SOLAR;
                default: throw new AssertionError("Bad heat source code: " + heatSourceCode);
            }
        }

        /**
         * Returns the spa heat source indicated by the combined code in the four low-order bits of
         * the given int.
         */
        static HeatSource spaSourceFromCode(int heatSourceCode) {
            return poolSourceFromCode(heatSourceCode >> 2);
        }

        /** Returns the combined heat source code for the specified heat sources. */
        static byte codeFromSources(HeatSource poolSource, HeatSource spaSource) {
            return (byte) (poolSource.code | (spaSource.code << 2));
        }
    }

    /**
     * The power state of a pump, either stopped or running. The unknown value is provided in case
     * we encounter some value that doesn't map to stopped or running on the bus. If this happens,
     * the situation should sbe investigated, and types modified appropriately.
     */
    public enum PumpPowerState {
        /** The pump is stopped. */
        STOPPED,

        /** The pump is running. */
        RUNNING,

        /** The power state code is unknown and unexpected. */
        UNKNOWN;

        PumpPowerState() { }

        static PumpPowerState forCode(byte code) {
            switch(code) {
                case 0x04: return STOPPED;
                case 0x0a: return RUNNING;
                default:   return UNKNOWN;
            }
        }
    }

    /**
     * A pump control regimen, either internal (wherein the pump acts as its own controller), or
     * external (wherein a separate pool controller directs pump operation). The unknown value is
     * provided in case we encounter some value that doesn't map to internal or external on the bus.
     * If this happens, the situation should sbe investigated, and types modified appropriately.
     */
    public enum PumpControlRegimen {
        /** The pump is controlling itself. */
        INTERNAL,

        /** An external controller (such as the SunTouch) is controlling the pump. */
        EXTERNAL,

        /** An unexpected and unknown regimen. */
        UNKNOWN;

        PumpControlRegimen() { }

        static PumpControlRegimen forCode(byte code) {
            switch(code) {
                case -1: return EXTERNAL;
                case 0:  return INTERNAL;
                default: return UNKNOWN;
            }
        }
    }

    /**
     * Whether a message is a request or a response. Some message types (such as
     * {@link PumpSpeedMessage}) exist in request and response forms, and this type is used to
     * indicate which of the two is applicable.
     */
    public enum RequestOrResponse {
        /** This is a request message. */
        REQUEST,

        /** This is a response message. */
        RESPONSE;

        /**
         * Given the destination of a pump request/response message, this method returns whether
         * it's a request or response.
         */
        static RequestOrResponse forDst(byte dst) {
            return isPump(dst) ? REQUEST : RESPONSE;
        }
    }

    /** A subscription to Pentair bus messages (furnished by {@link PentairBus#subscribe()}}). */
    public class Subscription implements AutoCloseable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

        /**
         * Creating new subscription enters it onto the list of outstanding subscriptions, enabling
         * message delivery.
         */
        Subscription() {
            subscriptions.add(this);
        }

        /**
         * Returns the queue associated with this subscription.
         */
        public BlockingQueue<Message> queue() {
            return messageQueue;
        }

        /**
         * Terminates this subscription, ensuring that no more messages are enqueued on its queue.
         * Failing to terminate a subscription when it is no longer needed wastes computation and
         * memory.
         */
        public void close() {
            subscriptions.remove(this);
        }
    }

    // ***Utility Functions***

    /** The ith element of this array is the unicode character for the hex digit i. */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /** Translates a byte array into a string of space-separated pairs of hex digits. */
    static String toHexString(byte[] bytes) {
        if (bytes.length == 0) {
            return "";
        } else {
            StringBuilder result = new StringBuilder(3 * bytes.length);

            for (byte b : bytes) {
                result.append(HEX_CHARS[b >> 4 & 0xf]);
                result.append(HEX_CHARS[b & 0xf]);
                result.append(' ');
            }

            result.setLength(result.length() - 1);  // Remove trailing space
            return result.toString();
        }
    }

    /* Returns int with the same value as the given unsigned byte (suppresses sign extension). */
    static int toInt(byte b) {
        return b & 0xff;
    }

    /** Returns the int value described by the two specified bytes. */
    static int toInt(byte high, byte low) {
        return 0x100 * toInt(high) + toInt(low);
    }
}
