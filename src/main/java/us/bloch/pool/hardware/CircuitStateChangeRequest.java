package us.bloch.pool.hardware;

import us.bloch.pool.hardware.PentairBus.Circuit;

import static us.bloch.pool.hardware.PentairBusCodes.Protocol.*;
import static us.bloch.pool.hardware.PentairBusCodes.Station.*;
import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * Message sent to the pool controller to request that a circuit be turned on or off.
 */
public class CircuitStateChangeRequest extends Message {
 /** The circuit for which a power state change is request */
    public final Circuit circuit;

    /** The desired power state for this circuit. */
    public final PentairBus.CircuitPowerState state;

    CircuitStateChangeRequest(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == CSC;

        circuit = Circuit.forCode(data[0]);
        state = PentairBus.CircuitPowerState.forCode(data[1]);
    }

    /** Returns a circuit state change request to effect the specified state change. */
    public CircuitStateChangeRequest(Circuit circuit, PentairBus.CircuitPowerState state) {
        this(CP, CTL, RQT, CSC, new byte[]{circuit.code(), state.code()});
    }

    /** Returns a string form for this circuit state change request. */
    public String toString() {
        return String.format("Turn circuit %s %s", circuit, state);
    }
}
