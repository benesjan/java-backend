package cz.docta.bookingtimes.gsonrequests;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class InfoChangedRequest {
    private String officeId;
    private String idToken;
    private Long generateSince;

    public Boolean areAttributesValid() {
        return this.officeId != null && this.idToken != null && this.generateSince != null;
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

    public Long getGenerateSince() {
        return generateSince;
    }

    public void setGenerateSince(Long generateSince) {
        this.generateSince = generateSince;
    }

    @Override
    public String toString() {
        return "InfoChangedRequest{" +
                "officeId='" + officeId + '\'' +
                ", idToken='" + idToken + '\'' +
                ", generateSince=" + generateSince +
                '}';
    }
}
