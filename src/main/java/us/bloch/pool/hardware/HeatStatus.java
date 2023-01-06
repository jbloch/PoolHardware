package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * A temperature/heat status message issued by the controller in response to a
 * {@link HeatStatusQuery} message.
 */
public class HeatStatus extends Message {
    /** Water temperature. */
    public final int waterTemp1;

    /** Another water temperature. I've never seen the value differ from waterTemp1 */
    public final int waterTemp2;

    /** Air temperature. */
    public final int airTemp;

    /* Pool seek temperature (also known as "setpoint"). */
    public final int poolSeekTemp;

    /* Spa seek temperature (also known as "setpoint"). */
    public final int spaSeekTemp;

    /** Pool heat source. */
    public final PentairBus.HeatSource poolHeatSource;

    /** Spa heat source. */
    public final PentairBus.HeatSource spaHeatSource;

    /** Temperature of the pool's solar panels. */
    public final int solarTemp;

    HeatStatus(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == HTS;

        waterTemp1 = PentairBus.toInt(data[0]);
        waterTemp2 = PentairBus.toInt(data[1]);
        airTemp = PentairBus.toInt(data[2]);
        poolSeekTemp = PentairBus.toInt(data[3]);
        spaSeekTemp = PentairBus.toInt(data[4]);
        poolHeatSource = PentairBus.HeatSource.poolSourceFromCode(data[5]);
        spaHeatSource = PentairBus.HeatSource.spaSourceFromCode(data[5]);
        solarTemp = PentairBus.toInt(data[8]);
    }

    /** Returns a string form of this message. */
    public String toString() {
        return String.format("Water: %d°, %d°; Air: %d°, Pool seek: %d°, Spa seek: %d°, "
                + "Pool heat source: %s, Spa heat source: %s, Solar: %d°",
                waterTemp1, waterTemp2, airTemp, poolSeekTemp, spaSeekTemp,
                poolHeatSource, spaHeatSource, solarTemp);
    }
}
