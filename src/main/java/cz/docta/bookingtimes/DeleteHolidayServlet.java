package cz.docta.bookingtimes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.generator.Holidays;
import cz.docta.bookingtimes.generator.Interval;
import cz.docta.bookingtimes.gsonrequests.DeleteHolidayRequest;
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
public class DeleteHolidayServlet extends FirebaseServlet {

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
        DeleteHolidayRequest deleteHolidayRequest = gson.fromJson(postData, DeleteHolidayRequest.class);


        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (deleteHolidayRequest != null && deleteHolidayRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyTokenAndDeleteHoliday(deleteHolidayRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyTokenAndDeleteHoliday(DeleteHolidayRequest deleteHolidayRequest) {
        FirebaseAuth.getInstance(Generator.getServiceApp()).verifyIdToken(deleteHolidayRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> {
                    this.deleteHoliday(Generator.getUserDatabase(decodedToken.getUid()), deleteHolidayRequest);
                }).addOnFailureListener(e -> System.err.println("Token verification failed. deleteHolidayRequest: " + deleteHolidayRequest));
    }

    private void deleteHoliday(FirebaseDatabase database, DeleteHolidayRequest deleteHolidayRequest) {
        // TODO: generate booking times if necessary

        String officeId = deleteHolidayRequest.getOfficeId();

        Map objectToSave = new HashMap();
        objectToSave.put("/officeHolidays/" + deleteHolidayRequest.getOfficeId() + "/" + deleteHolidayRequest.getHolidayId(), null);
        objectToSave.put("/officeFullInfo/" + deleteHolidayRequest.getOfficeId() + "/holidays/" + deleteHolidayRequest.getHolidayId(), null);
        objectToSave.put("/generatorInfo/" + deleteHolidayRequest.getOfficeId() + "/holidays/" + deleteHolidayRequest.getHolidayId(), null);

        database.getReference("generatorInfo/" + officeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && Generator.areHollidaysColliding(dataSnapshot, Long.parseLong(deleteHolidayRequest.getHolidayId()))) {
                    generateTimesAndSave(database, deleteHolidayRequest, dataSnapshot, objectToSave);
                } else {
                    saveObject(database, objectToSave, officeId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("DeleteHoliday request (deleteHoliday method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    private void generateTimesAndSave(FirebaseDatabase database, DeleteHolidayRequest deleteHolidayRequest, DataSnapshot generatorSnapshot, Map objectToSave) {
        Holidays holidays = new Holidays(generatorSnapshot.child("holidays/" + deleteHolidayRequest.getHolidayId()));
        DateTime currentDate = holidays.getStartAt().minusDays(1); // Decremented because of iterations
        DateTime lastDate = holidays.getEndAt();
        DateTime lastGeneratedDate = new DateTime(
                generatorSnapshot.child("lastGeneratedDate/year").getValue(Integer.class),
                generatorSnapshot.child("lastGeneratedDate/month").getValue(Integer.class),
                generatorSnapshot.child("lastGeneratedDate/date").getValue(Integer.class),
                23,
                59
        );

        if (lastGeneratedDate.compareTo(lastDate) < 0) {
            lastDate = lastGeneratedDate;
        }

        System.out.println(holidays);

        DataSnapshot dayHours;
        while ((currentDate = currentDate.plusDays(1)).compareTo(lastDate) != 1) {
            dayHours = generatorSnapshot.child("officeHours/" + (currentDate.getDayOfWeek() - 1));
            if (dayHours.child("available").getValue(Boolean.class)) {
                Interval holidayInterval = Interval.getPenetrationOfHolidaysAndDate(holidays, currentDate);
                // TODO: get holiday office hours intersection and generate hours
            }
        }

        saveObject(database, objectToSave, deleteHolidayRequest.getOfficeId());
    }

    private synchronized void saveObject(FirebaseDatabase database, Map<String, Object> objectToSave, String officeId) {
        database.getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Holiday deleting failed " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Holiday deleted successdully.");
            }
        });
    }
}
