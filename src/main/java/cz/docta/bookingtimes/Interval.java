package cz.docta.bookingtimes;

import org.joda.time.LocalTime;

/**
 * @author Jan Benes
 */
public class Interval {
    LocalTime start;
    LocalTime end;

    public Interval(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }
}
