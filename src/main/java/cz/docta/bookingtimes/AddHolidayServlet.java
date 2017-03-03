package cz.docta.bookingtimes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.gsonrequests.AddHolidayRequest;
import org.joda.time.DateTime;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class AddHolidayServlet extends FirebaseServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Gson gson = new Gson();

        String postData = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        AddHolidayRequest deleteHolidayRequest = gson.fromJson(postData, AddHolidayRequest.class);

        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (deleteHolidayRequest != null && deleteHolidayRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyTokenAndAddHoliday(deleteHolidayRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyTokenAndAddHoliday(AddHolidayRequest addHolidayRequest) {
        FirebaseAuth.getInstance(Generator.getServiceApp()).verifyIdToken(addHolidayRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> this.addHoliday(Generator.getUserDatabase(decodedToken.getUid()), addHolidayRequest))
                .addOnFailureListener(e -> System.err.println("Token verification failed. addHolidayRequest: " + addHolidayRequest));
    }

    private void addHoliday(FirebaseDatabase database, AddHolidayRequest addHolidayRequest) {
        // TODO: delete booking times if necessary
        Long holidayId = addHolidayRequest.getStartAt();
        String officeId = addHolidayRequest.getOfficeId();
        Map<String, Object> objectToSave = new HashMap<>();

        objectToSave.put("/officeHolidays/" + officeId + "/" + holidayId, addHolidayRequest.getOfficeHolidaysMap());
        objectToSave.put("/officeFullInfo/" + officeId + "/holidays/" + holidayId, addHolidayRequest.getOfficeFullInfoMap());

        database.getReference("generatorInfo/" + officeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    objectToSave.put("/generatorInfo/" + officeId + "/holidays/" + holidayId, addHolidayRequest.getEndAt());
                    if (isGenerationRequired(dataSnapshot, addHolidayRequest.getStartAt())) {
                        deleteInterferingTimes(dataSnapshot, objectToSave, addHolidayRequest.getStartAt(), addHolidayRequest.getEndAt());
                    }
                }

                saveObject(database, objectToSave, officeId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Registration request - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    private void deleteInterferingTimes(DataSnapshot generatorSnapshot, Map objectToSave, Long startAtTimestamp, Long endAtTimestamp) {
        Integer visitLength = generatorSnapshot.child("visitLength").getValue(Integer.class);
        // I have top remove all times that interfere with holiday so I am subtracting 1 visit length in milliseconds
        String startId = Generator.timestampToId(startAtTimestamp - visitLength * 60 * 1000);
        String endId = Generator.timestampToId(endAtTimestamp);

        // TODO
//        Generator.generateHours(objectToSave, visitLength, );

    }

    /**
     * @param generatorSnapshot Office generator snapshot
     * @param startAtTimestamp  Timestamp of a start of the interval
     * @return Boolean representing the comparison of the timestamp of the last generated date and the startAt timestamp.
     */
    private Boolean isGenerationRequired(DataSnapshot generatorSnapshot, Long startAtTimestamp) {
        if (generatorSnapshot.hasChild("lastGeneratedDate")) {
            // Get the timestamp in milliseconds of the end (plus 1 day) of the last generated date
            Long timestamp = new DateTime(
                    generatorSnapshot.child("lastGeneratedDate/year").getValue(Integer.class),
                    generatorSnapshot.child("lastGeneratedDate/month").getValue(Integer.class),
                    generatorSnapshot.child("lastGeneratedDate/date").getValue(Integer.class),
                    0,
                    0
            ).plusDays(1).getMillis();

            if (startAtTimestamp < timestamp) return true;
        }

        return false;
    }

    private void saveObject(FirebaseDatabase database, Map<String, Object> objectToSave, String officeId) {
        database.getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Holiday saving failed " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Holiday saved successdully.");
            }
        });
    }
}
