package cz.docta.bookingtimes.gsonrequests;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class DeleteHolidayRequest {
    private String officeId;
    private String holidayId;
    private String idToken;

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getHolidayId() {
        return holidayId;
    }

    public void setHolidayId(String holidayId) {
        this.holidayId = holidayId;
    }

    public Boolean areAttributesValid() {
        return this.officeId != null && this.holidayId != null && this.idToken != null;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    @Override
    public String toString() {
        return "DeleteHolidayRequest{" +
                "officeId='" + officeId + '\'' +
                ", holidayId='" + holidayId + '\'' +
                ", idToken='" + idToken + '\'' +
                '}';
    }
}
