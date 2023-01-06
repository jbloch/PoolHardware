package us.bloch.pool.hardware;

import java.time.LocalTime;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * Pump status message, sent by the pump to report its status.
 */
public class PumpStatus extends Message {
    /** The time (as reported by the pump). */
    public final LocalTime time;

    /** The pump's power state (running or stopped). */
    public final PentairBus.PumpPowerState powerState;

    /** The pump's current power consumption in watts. */
    public final int powerInWatts;

    /** The pump's current speed in RPM. */
    public final int speedInRpm;

    PumpStatus(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == PSM;

        powerState = PentairBus.PumpPowerState.forCode(data[0]);
        powerInWatts = PentairBus.toInt(data[3], data[4]);
        speedInRpm = PentairBus.toInt(data[5], data[6]);
        time = LocalTime.of(data[13], data[14]);
    }

    /** Returns a string form for this message. */
    public String toString() {
        return String.format("%s: Pump %s, Speed: %d RPM, Power: %d watts",
                time.format(SystemStatus.TIME_FORMATTER), powerState, speedInRpm, powerInWatts);
    }
}
