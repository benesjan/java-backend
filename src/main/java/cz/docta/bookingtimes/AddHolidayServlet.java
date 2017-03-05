package cz.docta.bookingtimes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.gsonrequests.AddHolidayRequest;

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
                }

                if (dataSnapshot.exists() && Generator.areHollidaysColliding(dataSnapshot, addHolidayRequest.getStartAt())) {
                    deleteInterferingTimesAndSave(database, dataSnapshot, objectToSave, addHolidayRequest);
                } else {
                    saveObject(database, objectToSave, officeId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("AddHolidays request (addHoliday method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    private void deleteInterferingTimesAndSave(FirebaseDatabase database, DataSnapshot generatorSnapshot, Map objectToSave, AddHolidayRequest addHolidayRequest) {
        String officeId = addHolidayRequest.getOfficeId();
        Integer visitLength = generatorSnapshot.child("visitLength").getValue(Integer.class);

        // Slightly hacky
        final Map<String, Boolean> synchronizedMap = new HashMap<>();

        synchronizedMap.put("privateFinished", false);
        synchronizedMap.put("publicFinished", false);

        // I have top remove all times that interfere with holiday so I am subtracting 1 visit length in milliseconds
        String startId = Generator.timestampToId(addHolidayRequest.getStartAt() - visitLength * 60 * 1000);
        String endId = Generator.timestampToId(addHolidayRequest.getEndAt());

        // Might rekt a patient if he managed to create an appointment just before the appointments are deleted> possible handled automatically by Firebase
        database.getReference("appointmentsPublic/" + officeId).orderByKey().startAt(startId).endAt(endId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot time : dataSnapshot.getChildren()) {
                    objectToSave.put("/appointmentsPublic/" + officeId + "/" + time.getKey(), null);
                }

                synchronized (synchronizedMap) {
                    if (synchronizedMap.get("privateFinished")) {
                        saveObject(database, objectToSave, officeId);
                    } else {
                        synchronizedMap.put("publicFinished", true);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("AddHolidays request (deleteInterferingTimesAndSave method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });

        database.getReference("appointmentsPrivate/" + officeId).orderByKey().startAt(startId).endAt(endId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, String> patientMessage = new HashMap<>();
                patientMessage.put("message", "Vaše objednání bylo zrušeno kvůli dovolené. Děkujeme za pochopení");
                patientMessage.put("title", "Zrušené objednání");
                patientMessage.put("sender", officeId);

                for (DataSnapshot time : dataSnapshot.getChildren()) {
                    String patientId = time.child("patient").getValue(String.class);
                    objectToSave.put("/appointmentsPrivate/" + officeId + "/" + time.getKey(), null);
                    objectToSave.put("/userAppointments/" + patientId + "/" + time.getKey(), null);
                    objectToSave.put("/userNotifications/" + patientId + "/" + database.getReference("/").push().getKey(), patientMessage);

                    database.getReference("/userInterfaceInfo/" + patientId + "/numberOfNotifications").runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Integer currentValue = mutableData.getValue(Integer.class);
                            if (currentValue == null) {
                                mutableData.setValue(1);
                            } else {
                                mutableData.setValue(currentValue + 1);
                            }

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                            System.out.println("Transaction on notifications of " + patientId + " completed");
                        }
                    });
                }

                synchronized (synchronizedMap) {
                    if (synchronizedMap.get("publicFinished")) {
                        saveObject(database, objectToSave, officeId);
                    } else {
                        synchronizedMap.put("privateFinished", true);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("AddHolidays request (deleteInterferingTimesAndSave method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });

    }

    private synchronized void saveObject(FirebaseDatabase database, Map<String, Object> objectToSave, String officeId) {
        database.getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Holiday saving failed " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Holiday saved successdully.");
            }
        });
    }
}
