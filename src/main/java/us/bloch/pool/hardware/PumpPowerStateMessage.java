package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * Turns the pump on or off (in request form), or indicates that such a request was received (in
 * response form).
 */
public class PumpPowerStateMessage extends Message {
    /** The function of this message, either request or response. */
    public final PentairBus.RequestOrResponse requestOrResponse;

    /** The power state of this pump (on or off). */
    public final PentairBus.PumpPowerState powerState;

    PumpPowerStateMessage(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == PPM;

        powerState = PentairBus.PumpPowerState.forCode(data[0]);
        requestOrResponse = PentairBus.RequestOrResponse.forDst(dst);
    }

    /** Returns a string form for this message. */
    public String toString() {
        return String.format("Pump power state %s: %s", requestOrResponse, powerState);
    }
}
