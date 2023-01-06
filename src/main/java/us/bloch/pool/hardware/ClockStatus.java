package us.bloch.pool.hardware;

import java.time.LocalDateTime;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/**
 * Clock/calendar message, sent periodically by the pool controller. The SunTouch doesn't display
 * or let you set the date, and doesn't keep track of daylight savings time, so these messages
 * are pretty much useless if you have a SunTouch.
 */
public class ClockStatus extends Message {
    /** The date and time reported in this message. */
    public final LocalDateTime dateAndTime;

    ClockStatus(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == CLS;

        dateAndTime = localDateTimeFromBytes(data);
    }

    /** Translates time and date from Pentair wire-level format to LocalDateTime. */
    static LocalDateTime localDateTimeFromBytes(byte[] data) {
        int hour = data[0];
        int minute = data[1];
        int dayOfMonth = data[3];
        int month = data[4];
        int year = 2000 + data[5];
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute);
    }

    /** Returns the string representation of this message. */
    public String toString() {
        return "Date and Time: " + dateAndTime.format(SystemStatus.TIME_FORMATTER);
    }
}
