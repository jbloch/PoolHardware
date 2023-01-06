package us.bloch.pool.hardware;

import static us.bloch.pool.hardware.PentairBusCodes.Protocol.*;
import static us.bloch.pool.hardware.PentairBusCodes.Station.*;
import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/** A request to change the seek temperature and heat source for the pool and spa. */
public class HeatConfigurationChangeRequest extends Message {
    /* Pool seek temperature (also known as "setpoint"). */
    public final int poolSeekTemp;

    /* Spa seek temperature (also known as "setpoint"). */
    public final int spaSeekTemp;

    /** Pool heat source. */
    public final PentairBus.HeatSource poolHeatSource;

    /** Spa heat source. */
    public final PentairBus.HeatSource spaHeatSource;

    HeatConfigurationChangeRequest(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == HCC;

        poolSeekTemp = PentairBus.toInt(data[0]);
        spaSeekTemp = PentairBus.toInt(data[1]);
        poolHeatSource = PentairBus.HeatSource.poolSourceFromCode(data[2]);
        spaHeatSource = PentairBus.HeatSource.spaSourceFromCode(data[2]);
    }

    /**
     * Returns a heat configuration change request message to reconfigure the heating system as
     * specified.
     *
     * @param poolSeekTemp the desired pool seek temperature (AKA setpoint)
     * @param spaSeekTemp the desired spa seek temperature (AKA setpoint)
     * @param poolSource the desired pool heat source
     * @param spaSource the desired spa heat source
     */
    public HeatConfigurationChangeRequest(int poolSeekTemp, int spaSeekTemp,
            PentairBus.HeatSource poolSource, PentairBus.HeatSource spaSource) {
        this(CP, CTL, RSL, HCC, new byte[]{(byte) poolSeekTemp, (byte) spaSeekTemp,
                PentairBus.HeatSource.codeFromSources(poolSource, spaSource), 0x00});

        if (poolSeekTemp > 127 || spaSeekTemp > 127)
            throw new IllegalArgumentException(
                    "Seek temperature(s) out of range: " + poolSeekTemp + ", " + spaSeekTemp);
    }

    /** Returns a string representation of this message. */
    public String toString() {
        return String.format("Heat configuration change request: Pool seek temp: %d°, "
                + "Spa seek temp: %d°, Pool heat source: %s, Spa heat source: %s",
                poolSeekTemp, spaSeekTemp, poolHeatSource, spaHeatSource);
    }
}
