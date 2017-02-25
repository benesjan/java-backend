package cz.docta.bookingtimes.gsonrequests;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class AddHolidayRequest {
    private String officeId;
    private String idToken;
    private Long startAt;
    private Long endAt;
    private String name;
    private String substituteDoctorName;

    public Boolean areAttributesValid() {
        return this.officeId != null && this.idToken != null && this.startAt != null && this.endAt != null && this.name != null;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public Long getStartAt() {
        return startAt;
    }

    public void setStartAt(Long startAt) {
        this.startAt = startAt;
    }

    public Long getEndAt() {
        return endAt;
    }

    public void setEndAt(Long endAt) {
        this.endAt = endAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubstituteDoctorName() {
        return substituteDoctorName;
    }

    public void setSubstituteDoctorName(String substituteDoctorName) {
        this.substituteDoctorName = (substituteDoctorName.trim().isEmpty()) ? null : substituteDoctorName;
    }

    @Override
    public String toString() {
        return "AddHolidayRequest{" +
                "officeId='" + officeId + '\'' +
                ", idToken='" + idToken + '\'' +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", name='" + name + '\'' +
                '}';
    }

    public Map getOfficeFullInfoMap() {
        Map toReturn = new HashMap();
        toReturn.put("endAt", this.endAt);
        toReturn.put("substituteDoctorName", this.substituteDoctorName);
        return toReturn;
    }

    public Map getOfficeHolidaysMap() {
        Map toReturn = this.getOfficeFullInfoMap();
        toReturn.put("name", this.name);
        return toReturn;
    }
}
