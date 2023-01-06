package us.bloch.pool.hardware;

import java.time.LocalDateTime;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.CLC;
import static us.bloch.pool.hardware.PentairBusCodes.Protocol.CP;
import static us.bloch.pool.hardware.PentairBusCodes.Station.CTL;
import static us.bloch.pool.hardware.PentairBusCodes.Station.RSL;

/**
 * Message sent to the pool controller to set the real-time clock / calendar.
 */
public class ClockChangeRequest extends Message {
    /** The date and time reported in this message. */
    public final LocalDateTime dateAndTime;

    ClockChangeRequest(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == CLC;

        dateAndTime = ClockStatus.localDateTimeFromBytes(data);
    }

    /** Returns a circuit state change request to effect the specified state change. */
    public ClockChangeRequest(LocalDateTime dt) {
        this(CP, CTL, RSL, CLC, new byte[]{
                (byte) dt.getHour(), (byte) dt.getMinute(), 0,
                (byte) dt.getDayOfMonth(), (byte) dt.getMonthValue(), (byte) (dt.getYear() - 2000)
        });
    }

    /** Returns a string form for this date/time change request. */
    public String toString() {
        // todo Use DateTimeFormatter to return a more reasonably string
        return String.format("Set clock to %s", dateAndTime.format(SystemStatus.TIME_FORMATTER));
    }
}

