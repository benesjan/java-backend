package cz.docta.bookingtimes.taskprocessors;

import com.google.firebase.database.*;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.taskprocessors.snapshotclasses.HolidayAddition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class HolidayAdditionsProcessor {

    public static void process(DataSnapshot holidayAdditions) {
        for (DataSnapshot office : holidayAdditions.getChildren()) {
            String officeId = office.getKey();
            for (DataSnapshot holidays : office.getChildren()) {
                addHolidays(new HolidayAddition(holidays, officeId));
            }
        }
    }

    private static void addHolidays(HolidayAddition holidayAddition) {
        Map<String, Object> objectToSave = new HashMap<>();

        String officeId = holidayAddition.getOfficeId();
        String holidayId = holidayAddition.getHolidayId();

        objectToSave.put("/taskQueue/holidayAdditions/" + officeId + "/" + holidayId, null);
        objectToSave.put("/officeHolidays/" + officeId + "/" + holidayId + "/status", "processed");
        objectToSave.put("/officeFullInfo/" + officeId + "/holidays/" + holidayId, holidayAddition.getOfficeFullInfoMap());

        Generator.getDatabase().getReference("generatorInfo/" + officeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    objectToSave.put("/generatorInfo/" + officeId + "/holidays/" + holidayId, holidayAddition.getEndAt());
                }

                if (dataSnapshot.exists() && Generator.areHolidaysColliding(dataSnapshot, holidayAddition.getStartAt())) {
                    deleteInterferingTimesAndSave(dataSnapshot, objectToSave, holidayAddition);
                } else {
                    saveObject(objectToSave, officeId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("AddHolidays request (addHoliday method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    private static void deleteInterferingTimesAndSave(DataSnapshot generatorSnapshot, Map objectToSave, HolidayAddition holidayAddition) {
        FirebaseDatabase database = Generator.getDatabase();
        String officeId = holidayAddition.getOfficeId();
        Integer visitLength = generatorSnapshot.child("visitLength").getValue(Integer.class);

        // Slightly hacky
        final Map<String, Boolean> synchronizedMap = new HashMap<>();

        synchronizedMap.put("privateFinished", false);
        synchronizedMap.put("publicFinished", false);

        // I have to remove all times that interfere with holiday so I am subtracting 1 visit length in milliseconds
        String startId = Generator.timestampToId(holidayAddition.getStartAt() - visitLength * 60 * 1000);
        String endId = Generator.timestampToId(holidayAddition.getEndAt());

        // Might rekt a patient if he managed to create an appointment just before the appointments are deleted>. Possibly handled automatically by Firebase
        database.getReference("appointmentsPublic/" + officeId).orderByKey().startAt(startId).endAt(endId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot time : dataSnapshot.getChildren()) {
                    objectToSave.put("/appointmentsPublic/" + officeId + "/" + time.getKey(), null);
                }

                synchronized (synchronizedMap) {
                    if (synchronizedMap.get("privateFinished")) {
                        saveObject(objectToSave, officeId);
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
                    objectToSave.put("/appointmentsPrivate/" + officeId + "/" + time.getKey(), null);

                    if (time.child("source").getValue(String.class).equals("online")) {
                        String patientId = time.child("patient").getValue(String.class);
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
                }

                synchronized (synchronizedMap) {
                    if (synchronizedMap.get("publicFinished")) {
                        saveObject(objectToSave, officeId);
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

    private static synchronized void saveObject(Map<String, Object> objectToSave, String officeId) {
        Generator.getDatabase().getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Holiday saving failed " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Holiday saved successfully.");
            }
        });
    }
}
