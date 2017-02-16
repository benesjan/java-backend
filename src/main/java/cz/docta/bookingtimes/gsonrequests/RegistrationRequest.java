package cz.docta.bookingtimes.gsonrequests;

/**
 * @author Jan Benes
 */
public class RegistrationRequest {
    private String officeId;
    private String idToken;

    public Boolean areAttributesValid() {
        return this.officeId != null && this.idToken != null;
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

    @Override
    public String toString() {
        return "RegistrationRequest{" +
                "officeId='" + officeId + '\'' +
                ", idToken='" + idToken + '\'' +
                '}';
    }
}
