package cz.docta.bookingtimes.generator;

import org.joda.time.LocalTime;

/**
 * @author Jan Benes
 */
public class Interval {
    LocalTime start;
    LocalTime end;

    public Interval(String interval) {
        String[] timeVals = interval.split("-");
        String[] start = timeVals[0].split(":");
        String[] end = timeVals[1].split(":");

        this.start = new LocalTime(Integer.parseInt(start[0]), Integer.parseInt(start[1]));
        this.end = new LocalTime(Integer.parseInt(end[0]), Integer.parseInt(end[1]));
    }

    @Override
    public String toString() {
        return "Interval{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
