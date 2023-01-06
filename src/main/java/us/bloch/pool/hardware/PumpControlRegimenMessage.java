package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * A pump control regimen, either internal (wherein the pump acts as its own controller), or
 * external (wherein a separate pool controller directs pump operation). The unknown value is
 * provided in case we encounter some value that doesn't map to internal or external on the bus.
 * If this happens, the situation should sbe investigated, and types modified appropriately.
 */
public class PumpControlRegimenMessage extends Message {
    /** The control regimen for this pump (internal or external control). */
    public final PentairBus.PumpControlRegimen regimen;

    /** Whether this message is a request to change the regimen, or a response to such a request. */
    public final PentairBus.RequestOrResponse requestOrResponse;

    PumpControlRegimenMessage(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == PCM;

        regimen = PentairBus.PumpControlRegimen.forCode(data[0]);
        requestOrResponse = PentairBus.RequestOrResponse.forDst(dst);
    }

    /** Returns the String form for this message. */
    public String toString() {
        return String.format("Pump control regimen %s: %s", requestOrResponse, regimen);
    }
}
