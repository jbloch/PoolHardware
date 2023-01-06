package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * Sets the pump speed (in its request form), and indicates that such a request was received (in its
 * response form).
 */
public class PumpSpeedMessage extends Message {
    /** Requested pump speed in RPM. */
    public final int speed;

    /** Whether this message is a request or response. */
    public final PentairBus.RequestOrResponse requestOrResponse;

    PumpSpeedMessage(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == PRM;

        requestOrResponse = PentairBus.RequestOrResponse.forDst(dst);
        int speedIndex = requestOrResponse.equals(PentairBus.RequestOrResponse.REQUEST) ? 2 : 0;
        speed = PentairBus.toInt(data[speedIndex], data[speedIndex + 1]);
    }

    /** Returns a string form for this message. */
    public String toString() {
        return String.format("Pump speed %s: %d", requestOrResponse, speed);
    }
}
