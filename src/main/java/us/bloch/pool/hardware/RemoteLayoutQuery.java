package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 *  Message sent to the pool controller to request the current layout for the QuickTouch remote.
 */
public class RemoteLayoutQuery extends Message {
    RemoteLayoutQuery(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == RLQ;
    }

    /** Returns a string form for this message. */
    public String toString() {
        return "Remote layout requested";
    }
}
