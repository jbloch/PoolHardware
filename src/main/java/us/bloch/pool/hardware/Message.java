package us.bloch.pool.hardware;

import java.util.Arrays;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;
import static us.bloch.pool.hardware.PentairBusCodes.Protocol.CP;
import static us.bloch.pool.hardware.PentairBusCodes.Protocol.PP;

/**
 * A message from or for the Pentair system bus. Instances of this class are obtained with the
 * static factory {@code Message.newInstance}.
 */
public class Message {
    /** The protocol of this message (0x01 except for pump messages, for which 0x00). */
    public final byte proto;

    /** The destination ID of this message. */
    public final byte dst;

    /** The source ID of this message. */
    public final byte src;

    /** The command code of this message. */
    public final byte cmd;

    /** The data of this message. */
    public final byte[] data;

    Message(byte proto, byte dst, byte src, byte cmd, byte[] data) {
        if (data == null)
            throw new NullPointerException("Data must not be null.");
        if (data.length > 0xff)
            throw new IllegalArgumentException("Data too long (" + data.length + " bytes)");

        this.proto = proto;
        this.dst = dst;
        this.src = src;
        this.cmd = cmd;
        this.data = data;
    }

    /**
     * Creates a message with the specified contents. Note that the data array is included in the
     * message directly; it is not copied. Different values of ver and cmd will yield instances
     * of different subclasses of Message, as dictated by the cmd and (rarely) dst parameters.
     *
     * @throws NullPointerException if data is null
     * @throws IllegalArgumentException if data is longer than 255 bytes
     */
    public static Message newInstance(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        if (ver == CP) {
            switch(cmd) {
                case SYS: return new SystemStatus(ver, dst, src, cmd, data);
                case CLS: return new ClockStatus(ver, dst, src, cmd, data);
                case CLC: return new ClockChangeRequest(ver, dst, src, cmd, data);
                case HSQ: return new HeatStatusQuery(ver, dst, src, cmd, data);
                case HTS: return new HeatStatus(ver, dst, src, cmd, data);
                case RLQ: return new RemoteLayoutQuery(ver, dst, src, cmd, data);
                case RLS: return new RemoteLayoutStatus(ver, dst, src, cmd, data);
                case CSC: return new CircuitStateChangeRequest(ver, dst, src, cmd, data);
                case HCC: return new HeatConfigurationChangeRequest(ver, dst, src, cmd, data);
                case SCR: return new StateChangeResponse(ver, dst, src, cmd, data);
            }
        } else if (ver == PP) {
            switch(cmd) {
                case PRM: return new PumpSpeedMessage(ver, dst, src, cmd, data);
                case PCM: return new PumpControlRegimenMessage(ver, dst, src, cmd, data);
                case PPM: return new PumpPowerStateMessage(ver, dst, src, cmd, data);
                case PSM: return PentairBusCodes.Station.isPump(dst)
                        ? new PumpStatusRequest(ver, dst, src, cmd, data)
                        : new PumpStatus(ver, dst, src, cmd, data);
            }
        }

        // If we don't recognize the message type, just return an "untyped" Message
        return new Message(ver, dst, src, cmd, data);
    }

    /**
     * Returns a string representation for this message, including all of the underlying data.
     * Subclasses override this method with one that interprets the data as appropriate.
     */
    public String toString() {
        return String.format("Ver: %02x, Dst: %02x, Src: %02x, Cmd: %02x, Data: %s",
                proto, dst, src, cmd, PentairBus.toHexString(data));
    }

    /** Two messages are equal if they contain the same bytes (ver, dst, src, cmd, and data). */
    public final boolean equals(Object o) {
        return (o instanceof Message msg) && proto == msg.proto && dst == msg.dst
                && src == msg.src && cmd == msg.cmd && Arrays.equals(data, msg.data);
    }

    /** Returns a hash code for this message (a digest of the underlying bytes). */
    public final int hashCode() {
        return 31 * (proto & dst << 8 & src << 16 & cmd << 24) + Arrays.hashCode(data);
    }
}
