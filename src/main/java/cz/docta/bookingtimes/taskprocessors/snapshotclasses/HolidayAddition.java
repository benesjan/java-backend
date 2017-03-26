package cz.docta.bookingtimes.taskprocessors.snapshotclasses;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class HolidayAddition {
    private String officeId;
    private String substituteDoctorName;
    private Long startAt; // holidayId
    private Long endAt;

    public HolidayAddition(DataSnapshot holidays, String officeId) {
        this.officeId = officeId;
        this.substituteDoctorName = (holidays.hasChild("substituteDoctorName")) ? holidays.child("substituteDoctorName").getValue(String.class) : null;
        this.startAt = Long.parseLong(holidays.getKey());
        this.endAt = holidays.child("endAt").getValue(Long.class);
    }

    public String getOfficeId() {
        return officeId;
    }

    public String getHolidayId() {
        return "" + startAt;
    }

    public Long getStartAt() {
        return startAt;
    }

    public Long getEndAt() {
        return endAt;
    }

    public Map getOfficeFullInfoMap() {
        Map toReturn = new HashMap();
        toReturn.put("endAt", this.endAt);
        toReturn.put("substituteDoctorName", this.substituteDoctorName);
        return toReturn;
    }
}
