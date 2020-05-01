package cz.docta.bookingtimes.taskprocessors;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.generator.Holidays;
import cz.docta.bookingtimes.generator.Interval;
import cz.docta.bookingtimes.taskprocessors.snapshotclasses.HolidayDeletion;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class HolidayDeletionsProcessor {
    public static void process(DataSnapshot holidayAdditions) {
        for (DataSnapshot office : holidayAdditions.getChildren()) {
            String officeId = office.getKey();
            for (DataSnapshot holidays : office.getChildren()) {
                deleteHoliday(new HolidayDeletion(holidays, officeId));
            }
        }
    }

    private static void deleteHoliday(HolidayDeletion holidayDeletion) {
        String officeId = holidayDeletion.getOfficeId();

        Map objectToSave = new HashMap();
        objectToSave.put("/taskQueue/holidayDeletions/" + officeId + "/" + holidayDeletion.getHolidayId(), null);
        objectToSave.put("/officeHolidays/" + officeId + "/" + holidayDeletion.getHolidayId(), null);
        objectToSave.put("/officeFullInfo/" + officeId + "/holidays/" + holidayDeletion.getHolidayId(), null);
        objectToSave.put("/generatorInfo/" + officeId + "/holidays/" + holidayDeletion.getHolidayId(), null);

        Generator.getDatabase().getReference("generatorInfo/" + officeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && Generator.areHolidaysColliding(dataSnapshot, Long.parseLong(holidayDeletion.getHolidayId()))) {
                    generateTimesAndSave(holidayDeletion, dataSnapshot, objectToSave);
                } else {
                    saveObject(objectToSave, officeId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("DeleteHoliday request (deleteHoliday method) - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }

    private static void generateTimesAndSave(HolidayDeletion holidayDeletion, DataSnapshot generatorSnapshot, Map objectToSave) {
        Holidays holidays = new Holidays(generatorSnapshot.child("holidays/" + holidayDeletion.getHolidayId()));
        LocalDate currentDate = new LocalDate(holidays.getStartAt().minusDays(1)); // Decremented because of iterations
        LocalDate lastDate = new LocalDate(holidays.getEndAt());
        LocalDate lastGeneratedDate = new LocalDate(
                generatorSnapshot.child("lastGeneratedDate/year").getValue(Integer.class),
                generatorSnapshot.child("lastGeneratedDate/month").getValue(Integer.class),
                generatorSnapshot.child("lastGeneratedDate/date").getValue(Integer.class)
        );
        List<Interval> intervalsToGenerateIn;
        Integer visitLength = generatorSnapshot.child("visitLength").getValue(Integer.class);
        String officeId = holidayDeletion.getOfficeId();

        if (lastGeneratedDate.compareTo(lastDate) < 0) {
            lastDate = lastGeneratedDate;
        }

        DataSnapshot dayHours;
        while ((currentDate = currentDate.plusDays(1)).compareTo(lastDate) != 1) {
            dayHours = generatorSnapshot.child("officeHours/" + (currentDate.getDayOfWeek() - 1));
            intervalsToGenerateIn = new ArrayList<>();

            if (dayHours.child("available").getValue(Boolean.class)) {
                List<Interval> dayHoursIntervals = Generator.getIntervals(dayHours.child("hours").getValue(String.class));
                Interval holidayInterval = Interval.getPenetrationOfHolidaysAndDate(holidays, currentDate);

                for (Interval officeInterval : dayHoursIntervals) {
                    if (holidayInterval.start.compareTo(officeInterval.start) != 1) {
                        // Holiday interval starts before the office hours begin
                        if (officeInterval.end.compareTo(holidayInterval.end) != 1) {
                            // office interval ends before the holidays end and starts after the holiday start
                            intervalsToGenerateIn.add(officeInterval);
                        } else if (officeInterval.start.compareTo(holidayInterval.end) != 1) {
                            // holidays start before the office hours start and end before office hours end
                            intervalsToGenerateIn.add(new Interval(officeInterval.start, officeInterval.end));
                        }
                        // else: holidays start end end before the start of office hours --> no intersection
                    } else {
                        // Holiday interval starts after the office hours begin
                        if (holidayInterval.end.compareTo(officeInterval.end) != 1) {
                            // Holidays begin after the office hours start and ends before the officeHours end
                            intervalsToGenerateIn.add(holidayInterval);
                        } else if (holidayInterval.start.compareTo(officeInterval.end) != 1) {
                            // Holidays begin before between the start and end of office hours and end after the end of office hours
                            intervalsToGenerateIn.add(new Interval(holidayInterval.start, officeInterval.end));
                        }
                        // else: holidays start end end after the end of office hours --> no intersection
                    }
                }

                Generator.generateHours(objectToSave, visitLength, intervalsToGenerateIn, currentDate, officeId);
            }
        }

        saveObject(objectToSave, holidayDeletion.getOfficeId());
    }

    private static synchronized void saveObject(Map<String, Object> objectToSave, String officeId) {
        Generator.getDatabase().getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Holiday deleting failed " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Holiday deleted successfully.");
            }
        });
    }
}
