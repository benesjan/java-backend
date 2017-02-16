package cz.docta.bookingtimes.generator;

import com.google.firebase.database.DataSnapshot;
import org.joda.time.DateTime;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class Holidays {
    private DateTime startAt;
    private DateTime endAt;

    Holidays(DataSnapshot holidays) {
        this.startAt = new DateTime(Long.parseLong(holidays.getKey()));
        this.endAt = new DateTime(holidays.getValue(Long.class));
    }

    @Override
    public String toString() {
        return "Holidays{" +
                "startAt=" + startAt +
                ", endAt=" + endAt +
                '}';
    }
}
