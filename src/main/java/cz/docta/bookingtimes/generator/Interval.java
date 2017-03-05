package cz.docta.bookingtimes.generator;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

/**
 * @author Jan Benes
 */
public class Interval {
    public LocalTime start;
    public LocalTime end;

    Interval(String interval) {
        String[] timeVals = interval.split("-");
        String[] start = timeVals[0].split(":");
        String[] end = timeVals[1].split(":");

        this.start = new LocalTime(Integer.parseInt(start[0]), Integer.parseInt(start[1]));
        this.end = new LocalTime(Integer.parseInt(end[0]), Integer.parseInt(end[1]));
    }

    public Interval(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    /**
     * @param holidays   Holidays for which the penetration interval is going to be computed
     * @param startOfDay beginning of the day in which the interval is going to be computed
     * @return Interval of penetration of holidays and some particular day
     */
    public static Interval getPenetrationOfHolidaysAndDate(Holidays holidays, DateTime startOfDay) {
        DateTime endOfDay = startOfDay.plusDays(1); // 00:00 of the next day
        LocalTime start;
        LocalTime end;
        DateTime holidaysStart = holidays.getStartAt();
        DateTime holidaysEnd = holidays.getEndAt();

        if (isInInterval(startOfDay, endOfDay, holidaysStart)) {
            start = new LocalTime(holidaysStart.getHourOfDay(), holidaysStart.getMinuteOfHour());
            end = (isInInterval(startOfDay, endOfDay, holidaysEnd)) ?
                    new LocalTime(holidaysEnd.getHourOfDay(), holidaysEnd.getMinuteOfHour()) : new LocalTime(23, 59);
        } else if (isInInterval(startOfDay, endOfDay, holidaysEnd)) {
            start = new LocalTime(0, 0);
            end = new LocalTime(holidaysEnd.getHourOfDay(), holidaysEnd.getMinuteOfHour());
        } else if (startOfDay.compareTo(holidaysStart) == 1 && endOfDay.compareTo(holidaysEnd) == -1) {
            // Date is fully within holidays
            start = new LocalTime(0, 0);
            end = new LocalTime(23, 59);
        } else {
            return null;
        }

        return new Interval(start, end);
    }

    /**
     * @param startOfDay  start of the interval
     * @param endOfDay    end of the interval
     * @param dateToCheck value which is going to be checked
     * @return Boolean value which represents if the dateToCheck is in some interval
     */
    private static Boolean isInInterval(DateTime startOfDay, DateTime endOfDay, DateTime dateToCheck) {
        return startOfDay.compareTo(dateToCheck) != 1 && dateToCheck.compareTo(endOfDay) == -1;
    }

    @Override
    public String toString() {
        return "Interval{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
