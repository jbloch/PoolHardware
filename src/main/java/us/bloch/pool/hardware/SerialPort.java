package us.bloch.pool.hardware;

import java.io.*;

/**
 * A simple serial port facility. This class currently works only on Linux. It is not thread
 * safe, except that it's acceptable for one thread to use the input stream while another uses
 * the output stream. Beyond that, external synchronization is required.
 */

class SerialPort implements Closeable {
    /**
     * String to set serial port up for 8-N-1 communication with VMIN=32, VTIME=1. Got params by
     * letting pyserial set up the port, calling stty with min and time options, and then calling
     * stty -a.
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String LINUX_SERIAL_PORT_CONFIG_CMD =
            "stty -F %s %d -parenb  cs8 -cstopb min 32 time 1 " // Actual configuration
            + "-icrnl cread clocal -crtscts -ignbrk -brkint -ignpar -parmrk -inpck -istrip -inlcr "
            + "-igncr -ixon -ixoff -iuclc -ixany -imaxbel -iutf8 -opost -olcuc -ocrnl -onlcr "
            + "-onocr -onlret -ofill -ofdel nl0 cr0 tab0 bs0 vt0 ff0 -isig -icanon -iexten -echo "
            + "-echoe -echok -echonl -noflsh -xcase -tostop -echoprt -echoctl -echoke";

    private final FileInputStream inputStream;
    private final FileOutputStream outputStream;

    /**
     * Creates a SerialPort instance for the serialPort with the given port name and baud rate.
     * This method currently assumes 8-n-1 configuration, but could be easily modified
     * to allow the caller to specify these parameters, as well as the timeout parameters
     * (VMIN and VTIME).
     */
    @SuppressWarnings("SpellCheckingInspection")
    public SerialPort(String portName, int baudRate) throws IOException {
        // Set up serial port (mode, baud rate, parity, timeouts, etc.)
        String cmd = String.format(LINUX_SERIAL_PORT_CONFIG_CMD, portName, baudRate);
        Process proc = Runtime.getRuntime().exec(cmd);
        try {
            proc.waitFor();
        } catch(InterruptedException e) {
            throw new IOException("Serial port configuration command hung");
        }
        int exitValue = proc.exitValue();
        if (exitValue != 0)
            throw new IOException("Serial port configuration command failed: " + exitValue);

        inputStream = new FileInputStream(portName);
        try {
            outputStream = new FileOutputStream(portName);
        } catch (Exception e) {
            // We can't open output stream; close input stream
            try {
                inputStream.close();
            } catch(IOException ignored) {
                // Original exception will be rethrown
            }
            throw e;
        }
    }

    public void close() throws IOException {
        try {
            outputStream.close();
        } finally {
            inputStream.close();
        }
    }

    /**
     * Returns the input stream for this serial port.
     */
    public InputStream inputStream() {
        return inputStream;
    }

    /**
     * Returns the input stream for this serial port. */
    public OutputStream outputStream() {
        return outputStream;
    }
}
