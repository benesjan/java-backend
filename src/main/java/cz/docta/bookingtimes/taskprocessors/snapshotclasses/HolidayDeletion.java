package cz.docta.bookingtimes.taskprocessors.snapshotclasses;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class HolidayDeletion {
    private String officeId;
    private Long startAt; // holidayId
    private Long endAt;

    public HolidayDeletion(DataSnapshot holidays, String officeId) {
        this.officeId = officeId;
        this.startAt = Long.parseLong(holidays.getKey());
        this.endAt = holidays.getValue(Long.class);
    }

    public String getHolidayId() {
        return "" + startAt;
    }

    public String getOfficeId() {
        return officeId;
    }
}
