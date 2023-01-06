package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * A Temperature (and heat) status request. This message elicits a {@link HeatStatus} message
 * from the controller that includes the pool and spa seek temperatures.
 */
public class HeatStatusQuery extends Message {
    public static final HeatStatusQuery INSTANCE
            = new HeatStatusQuery((byte) 0x1, (byte) 0x10, (byte) 0x22, (byte) 0xc8, new byte[0]);

    HeatStatusQuery(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);

        assert cmd == HSQ;
    }

    /** Returns a string form for this message. */
    public String toString() {
        return "Heat status request.";
    }
}
