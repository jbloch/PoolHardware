package us.bloch.pool.hardware;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.Set;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * A system status message, sent periodically by the pool controller.
 */
public class SystemStatus extends Message {
    /** The time (as reported by the pool controller). */
    public final LocalTime time;

    /** The enabled circuits. */
    public final Set<PentairBus.Circuit> enabledCircuits;

    /** The active body (pool, spa, or null if none) */
    public final PentairBus.Circuit activeBody;

    /** Whether the heater is on. */
    public final boolean heaterOn;

    /** Whether there is a delay currently in progress. */
    public final boolean delayInProgress;

    /**
     *  The water temperature in degrees. This is the temperature of the active body. If there is no active body,
     *  this field is of little value.
     */
    public final int waterTemp;

    /** The air temperature in degrees. */
    public final int airTemp;

    /** The temperature at the solar panels in degrees. */
    public final int solarTemp;

    /** The pool's heat source. */
    public final PentairBus.HeatSource poolHeatSource;

    /** The spa's heat source. */
    public final PentairBus.HeatSource spaHeatSource;

    SystemStatus(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == SYS;

        time = LocalTime.of(data[0], data[1]);
        enabledCircuits = PentairBus.circuits(data[2], data[3]);
        activeBody = enabledCircuits.contains(PentairBus.Circuit.SPA) ? PentairBus.Circuit.SPA : (enabledCircuits.contains(PentairBus.Circuit.POOL) ? PentairBus.Circuit.POOL : null);
        heaterOn = data[10] == 15;
        delayInProgress = (data[12] & 4) != 0;
        waterTemp = PentairBus.toInt(data[14]);
        airTemp = PentairBus.toInt(data[18]);
        solarTemp = PentairBus.toInt(data[19]);
        poolHeatSource = PentairBus.HeatSource.poolSourceFromCode(data[22]);
        spaHeatSource = PentairBus.HeatSource.spaSourceFromCode(data[22]);
    }

    final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    /** Returns the string form of this message. */
    public String toString() {
        StringBuilder result = new StringBuilder(100);
        Formatter fmt = new Formatter(result);
        fmt.format("%s: ", time.format(TIME_FORMATTER));
        if (activeBody != null) {
            fmt.format("%s %d°, ", activeBody, waterTemp);
            if (heaterOn) {
                fmt.format("Heater on, ");
            }
        }

        // TODO: circuit could know if aux or feature, instead of exporting the not quite constant
        for (PentairBus.Circuit c : PentairBus.Circuit.AUX_CIRCUITS)
            if (enabledCircuits.contains(c))
                fmt.format("%s, ", c);

        if (delayInProgress)
            fmt.format("D, ");

        for (PentairBus.Circuit c : PentairBus.Circuit.FEATURE_CIRCUITS)
            if (enabledCircuits.contains(c))
                fmt.format("%s, ", c);

        fmt.format("Air: %d°, Solar: %d°, ", airTemp, solarTemp);
        fmt.format("Pool heat source: %s, Spa heat source: %s", poolHeatSource, spaHeatSource);
        return result.toString();
    }
}
