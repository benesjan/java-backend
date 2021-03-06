package cz.docta.bookingtimes.generator;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class Generator {
    private static String projectName = "docta-project";

    public static FirebaseDatabase getDatabase() {

        Map<String, Object> auth = new HashMap<>();
        auth.put("uid", "my-service-worker");

        if (FirebaseApp.getApps().size() == 0) {
            try {
                FileInputStream serviceAccount =
                        new FileInputStream("keys/adminsdk.json");

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://" + Generator.getProjectName() + ".firebaseio.com/")
                        .setDatabaseAuthVariableOverride(auth)
                        .build();

                FirebaseApp.initializeApp(options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return FirebaseDatabase.getInstance(FirebaseApp.getApps().get(0));
    }

    public static String getProjectName() {
        return projectName;
    }

    public static String timestampToId(Long timestamp) {
        DateTime time = new DateTime(timestamp);
        Integer month = time.getMonthOfYear();
        Integer dayOfMonth = time.getDayOfMonth();
        Integer hours = time.getHourOfDay();
        Integer minutes = time.getMinuteOfHour();

        return "" + time.getYear() + ((month < 10) ? "0" : "") + month + ((dayOfMonth < 10) ? "0" : "") + dayOfMonth
                + ((hours < 10) ? "0" : "") + hours + ((minutes < 10) ? "0" : "") + minutes;
    }

    /**
     * This function generates booking time according to office Data and saves them to Firebase using reference,
     * which is created using the global database object.
     *
     * @param office Raw snapshot of data received from Firebase
     */
    public static void generateAndSaveHours(DataSnapshot office, FirebaseDatabase database) {
        String officeId = office.getKey();
        Integer visitLength = office.child("visitLength").getValue(Integer.class);
        Integer numberOfDays = office.child("numberOfDays").getValue(Integer.class);

        Map<String, Object> updatedOfficeData = new HashMap<>();

        LocalDate lastDate = new LocalDate().plusDays(numberOfDays);
        LocalDate currentDate;
        if (office.hasChild("lastGeneratedDate")) {
            currentDate = new LocalDate(
                    office.child("lastGeneratedDate/year").getValue(Integer.class),
                    office.child("lastGeneratedDate/month").getValue(Integer.class),
                    office.child("lastGeneratedDate/date").getValue(Integer.class)
            );
        } else {
            currentDate = new LocalDate(); // If the lastGeneratedDate is not available set to tomorrow
        }

        List relevantHolidays = getRelevantHolidays(office.child("holidays"), lastDate);

        DataSnapshot dayHours;
        // Iterate until the currentDate is bigger than last date and generate values accordingly
        while ((currentDate = currentDate.plusDays(1)).compareTo(lastDate) != 1) {
            dayHours = office.child("officeHours/" + (currentDate.getDayOfWeek() - 1));
            if (dayHours.child("available").getValue(Boolean.class)) {
                List<Interval> intervalList = shrinkByHolidays(getIntervals(dayHours.child("hours").getValue(String.class)), relevantHolidays, currentDate);

                generateHours(updatedOfficeData, visitLength, intervalList, currentDate, officeId);
            }
        }

        Map<String, Integer> lastGeneratedDate = new HashMap<>();

        // Set the last generated date
        lastGeneratedDate.put("year", lastDate.getYear());
        lastGeneratedDate.put("month", lastDate.getMonthOfYear());
        lastGeneratedDate.put("date", lastDate.getDayOfMonth());

        updatedOfficeData.put("/generatorInfo/" + officeId + "/lastGeneratedDate", lastGeneratedDate);
        updatedOfficeData.put("/officeFullInfo/" + officeId + "/lastGeneratedDate", lastGeneratedDate);

        database.getReference("/").updateChildren(updatedOfficeData, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Data could not be saved " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Data saved successfully.");
            }
        });

    }

    /**
     * @param holidays DataSnapshot of all the holidays which belong to the
     * @param lastDate last date in which booking times will be generated
     * @return List of holidays which intersects with generated dates
     */
    private static List getRelevantHolidays(DataSnapshot holidays, LocalDate lastDate) {
        ArrayList<Holidays> toReturn = new ArrayList<>();

        DateTime holidaysStart;

        for (DataSnapshot _holidays : holidays.getChildren()) {
            holidaysStart = new DateTime(Long.parseLong(_holidays.getKey()));
            if (holidaysStart.getYear() < lastDate.getYear() || (holidaysStart.getDayOfYear() <= lastDate.getDayOfYear())) {
                toReturn.add(new Holidays(_holidays));
            }
        }

        return toReturn;
    }

    /**
     * The following function generates booking times and saves them to objectToSave map.
     *
     * @param objectToSave Map, which gets saved using the multi location update
     * @param visitLength  Length of a doctor visit
     * @param intervals    List of Interval objects, which take place that day
     * @param date         Date at which the appointment times are generated
     * @param officeId     Id of the office
     */

    public static void generateHours(Map objectToSave, Integer visitLength, List<Interval> intervals, LocalDate date, String officeId) {
        Integer dayDate = date.getDayOfMonth();
        Integer month = date.getMonthOfYear();

        String prefix = "" + date.getYear() + ((month < 10) ? "0" : "") + month + ((dayDate < 10) ? "0" : "") + dayDate;

        for (Interval interval : intervals) {
            LocalTime currentTime = interval.start;
            LocalTime nextTime;

            while ((nextTime = currentTime.plusMinutes(visitLength)).compareTo(interval.end) != 1) {
                String bookTime = prefix + ((currentTime.getHourOfDay() < 10) ? "0" : "") + currentTime.getHourOfDay()
                        + ((currentTime.getMinuteOfHour() < 10) ? "0" : "") + currentTime.getMinuteOfHour();

                objectToSave.put("/appointmentsPublic/" + officeId + "/" + bookTime, true);

                currentTime = nextTime;
            }
        }
    }

    /**
     * @param officeHours (example: 7:30-12:30,13:30-15:30)
     * @return List of Intervals. Each Interval consists of Start time (LocalTime object) and end time.
     */
    public static List<Interval> getIntervals(String officeHours) {
        ArrayList<Interval> intervals = new ArrayList<>();

        for (String interval : officeHours.split(",")) {
            intervals.add(new Interval(interval));
        }

        return intervals;
    }

    /**
     * @param intervalList currentDate's intervals
     * @param holidaysList list of holidays
     * @param currentDate  date which has to be set to 00:00
     * @return list of modified intervals
     */
    private static List<Interval> shrinkByHolidays(List<Interval> intervalList, List<Holidays> holidaysList, LocalDate currentDate) {
        if (intervalList.size() == 0) return intervalList; // Performance improvement

        for (Holidays holidays : holidaysList) {
            Interval holidayInterval = Interval.getPenetrationOfHolidaysAndDate(holidays, currentDate);
            if (holidayInterval != null) {
                ArrayList<Interval> temporary = new ArrayList<>();

                for (Interval interval : intervalList) {

                    if (interval.start.compareTo(holidayInterval.start) != 1) {
                        // Original interval starts before holiday

                        if (interval.end.compareTo(holidayInterval.start) != 1) {
                            // There is no penetration of holiday interval and original interval (start and end is smaller than holiday start)
                            temporary.add(interval);
                        } else if (interval.end.compareTo(holidayInterval.end) != 1) {
                            // Original interval ends between start and end of holidays
                            interval.end = holidayInterval.start;
                            temporary.add(interval);
                        } else {
                            // Original ends after holidays end. Split interval in 2
                            temporary.add(new Interval(interval.start, holidayInterval.start));
                            interval.start = holidayInterval.end;
                            temporary.add(interval);
                        }

                    } else if (interval.start.compareTo(holidayInterval.end) != 1) {
                        // Original interval starts between start and end of holidayInterval

                        // If this condition is not met (a.k.a. the end of original interval is smaller than the end of holiday interval)
                        // the interval as a whole is deleted
                        if (interval.end.compareTo(holidayInterval.end) == 1) {
                            // The original interval ends after the holidays so start is set to end of holidays.
                            interval.start = holidayInterval.end;
                            temporary.add(interval);
                        }
                    } else {
                        // Original interval starts after the end of holidays. No modifications will be made.
                        temporary.add(interval);
                    }
                }

                intervalList = temporary;
            }
        }

        return intervalList;
    }

    /**
     * @param generatorSnapshot Office generator snapshot
     * @param startAtTimestamp  Timestamp of a start of the interval
     * @return Boolean representing the comparison of the timestamp of the last generated date and the startAt timestamp.
     */
    public static Boolean areHolidaysColliding(DataSnapshot generatorSnapshot, Long startAtTimestamp) {
        if (generatorSnapshot.hasChild("lastGeneratedDate")) {
            // Get the timestamp in milliseconds of the end (plus 1 day) of the last generated date
            Long timestamp = new DateTime(
                    generatorSnapshot.child("lastGeneratedDate/year").getValue(Integer.class),
                    generatorSnapshot.child("lastGeneratedDate/month").getValue(Integer.class),
                    generatorSnapshot.child("lastGeneratedDate/date").getValue(Integer.class),
                    0,
                    0
            ).plusDays(1).getMillis();

            return startAtTimestamp < timestamp;
        }

        return false;
    }
}
