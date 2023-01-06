package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 *  Message sent by pool controller to acknowledge state change request.
 */
public class StateChangeResponse extends Message {
    StateChangeResponse(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == SCR;
    }

    /** Returns a string form for this message. */
    public String toString() {
        return "State change request acknowledged";
    }
}
