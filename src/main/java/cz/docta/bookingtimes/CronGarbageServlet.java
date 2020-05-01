package cz.docta.bookingtimes;

import com.google.firebase.database.*;
import cz.docta.bookingtimes.generator.Generator;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class CronGarbageServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FirebaseDatabase database = Generator.getDatabase();
        Long deletionTimestamp = new DateTime().minusDays(1).getMillis(); // Getting the value of yesterday to avoid messing with time zones
        String timestampId = Generator.timestampToId(deletionTimestamp);

        resp.getWriter().append("{\"success\":true}");

        DatabaseReference ref = database.getReference("generatorInfo");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot office : dataSnapshot.getChildren()) {
                    deleteHolidaysAndPublicAndProceed(office, deletionTimestamp, timestampId, database);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    private void deleteHolidaysAndPublicAndProceed(DataSnapshot generatorSnapshot, Long deletionTimestamp, String timestampId, FirebaseDatabase database) {
        Map objectToSave = new HashMap();
        String officeId = generatorSnapshot.getKey();

        // Deleting the holidays
        DataSnapshot holidays = generatorSnapshot.child("holidays");
        for (DataSnapshot _holidays : holidays.getChildren()) {
            if (_holidays.getValue(Long.class) < deletionTimestamp) {
                objectToSave.put("/officeHolidays/" + officeId + "/" + _holidays.getKey(), null);
                objectToSave.put("/officeFullInfo/" + officeId + "/holidays/" + _holidays.getKey(), null);
                objectToSave.put("/generatorInfo/" + officeId + "/holidays/" + _holidays.getKey(), null);
            }
        }

        // Delete expired appointments in public
        database.getReference("appointmentsPublic/" + officeId).orderByKey().endAt(timestampId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot time : dataSnapshot.getChildren()) {
                    objectToSave.put("/appointmentsPublic/" + officeId + "/" + time.getKey(), null);
                }
                deletePrivateAndSave(database, objectToSave, officeId, timestampId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("CronGarbageServlet request (deleteHolidaysAndPublicAndProceed method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    private void deletePrivateAndSave(FirebaseDatabase database, Map objectToSave, String officeId, String timestampId) {
        // Delete expired appointments in private
        database.getReference("appointmentsPrivate/" + officeId).orderByKey().endAt(timestampId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String patientId;
                for (DataSnapshot time : dataSnapshot.getChildren()) {
                    patientId = time.child("patient").getValue(String.class);
                    objectToSave.put("/appointmentsPrivate/" + officeId + "/" + time.getKey(), null);
                    if (patientId != null) {
                        objectToSave.put("/userAppointments/" + patientId + "/" + time.getKey(), null);
                        objectToSave.put("/userContactedOffices/" + patientId + "/" + officeId + "/booked", false);
                    }
                }
                saveObject(database, objectToSave, officeId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("CronGarbageServlet request (deleteHolidaysAndPublicAndProceed method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    /**
     * This function saves the created object to Firebase using the multilocation update.
     * @param database FirebaseDatabase object created using the service-worker privileges
     * @param objectToSave Map of updated paths
     * @param officeId Used only for logging
     */
    private void saveObject(FirebaseDatabase database, Map<String, Object> objectToSave, String officeId) {
        database.getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("CronGarbageServlet: OfficeId: " + officeId + ", Data deleting failed " + databaseError.getMessage());
            } else {
                System.out.println("CronGarbageServlet: OfficeId: " + officeId + ", Data deleted successfully.");
            }
        });
    }
}
