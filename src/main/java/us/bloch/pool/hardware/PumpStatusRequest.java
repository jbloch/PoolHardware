package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Protocol.*;
import static us.bloch.pool.hardware.PentairBusCodes.Station.*;
import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/** Message sent to the pump to request its current status. */
public class PumpStatusRequest extends Message {
    /** An instance of this class that can be placed on the bus to request pump status. */
    public static final Message INSTANCE = new PumpStatusRequest(PP, PMP, CTL, PSM, new byte[0]);

    PumpStatusRequest(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == PSM;
    }

    /** Returns the string form of this pump status. */
    public String toString() {
        return "Pump status requested.";
    }
}
