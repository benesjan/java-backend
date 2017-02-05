package cz.docta.bookingtimes;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class GeneratorServlet extends HttpServlet {

    FirebaseDatabase database = null;

    @Override
    public void init(ServletConfig config) {
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.applicationDefault())
                .setDatabaseUrl("https://doctor-appointment-system.firebaseio.com/")
                .build();

        FirebaseApp defaultApp = FirebaseApp.initializeApp(options);

        this.database = FirebaseDatabase.getInstance(defaultApp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().println("OK");

        DatabaseReference ref = this.database.getReference("generatorInfo");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot office : dataSnapshot.getChildren()) {
                    generateAndSaveHours(office);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    /**
     * This function generates booking time according to office Data and saves them to Firebase using reference,
     * which is created using the global database object.
     *
     * @param office Raw snapshot of data received from Firebase
     */
    private void generateAndSaveHours(DataSnapshot office) {
        String officeId = office.getKey();
        Integer visitLength = office.child("visitLength").getValue(Integer.class);
        Integer numberOfDays = office.child("numberOfDays").getValue(Integer.class);

        DatabaseReference ref = this.database.getReference("/");
        Map updatedOfficeData = new HashMap();

        DateTime currentDate;
        if (office.hasChild("lastGeneratedDate")) {
            currentDate = new DateTime(
                    office.child("lastGeneratedDate/year").getValue(Integer.class),
                    office.child("lastGeneratedDate/month").getValue(Integer.class),
                    office.child("lastGeneratedDate/date").getValue(Integer.class),
                    0,
                    0
            );
        } else {
            currentDate = new DateTime(); // If the lastGeneratedDate is not available set to tomorrow
        }

        DateTime lastDate = new DateTime().plusDays(numberOfDays);
        DataSnapshot dayHours;
        while ((currentDate = currentDate.plusDays(1)).compareTo(lastDate) == -1) {
            dayHours = office.child("officeHours/" + (currentDate.getDayOfWeek() - 1));
            if (dayHours.child("available").getValue(Boolean.class)) {
                this.generateHours(updatedOfficeData, visitLength, this.getIntervals(dayHours.child("hours").getValue(String.class)), currentDate, officeId);
            }
        }

        Map<String, Integer> lastGeneratedDate = new HashMap<>();

        // On this date the condition hasn't passed, so it has to be decremented in order to represent last generated date
        currentDate = currentDate.minusDays(1);

        lastGeneratedDate.put("year", currentDate.getYear());
        lastGeneratedDate.put("month", currentDate.getMonthOfYear());
        lastGeneratedDate.put("date", currentDate.getDayOfMonth());

        updatedOfficeData.put("/generatorInfo/" + officeId + "/lastGeneratedDate", lastGeneratedDate);
        updatedOfficeData.put("/officeFullInfo/" + officeId + "/lastGeneratedDate", lastGeneratedDate);

        ref.updateChildren(updatedOfficeData, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + officeId + ", Data could not be saved " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + officeId + ", Data saved successfully.");
            }
        });

    }

    /**
     * The following function generates booking times and saves them to updatedOfficeData map.
     *
     * @param updatedOfficeData Map, which gets saved using the multi location update
     * @param visitLength       Length of a doctor visit
     * @param intervals         List of Interval objects, which take place that day
     * @param date
     * @param officeId
     */

    private void generateHours(Map updatedOfficeData, Integer visitLength, List<Interval> intervals, DateTime date, String officeId) {
        Integer dayDate = date.getDayOfMonth();
        Integer month = date.getMonthOfYear();

        String prefix = "" + date.getYear() + ((month < 10) ? "0" : "") + month + ((dayDate < 10) ? "0" : "") + dayDate;

        for (Interval interval : intervals) {
            LocalTime currentTime = interval.start;
            LocalTime nextTime;

            while ((nextTime = currentTime.plusMinutes(visitLength)).compareTo(interval.end) != 1) {
                String bookTime = prefix + ((currentTime.getHourOfDay() < 10) ? "0" : "") + currentTime.getHourOfDay()
                        + ((currentTime.getMinuteOfHour() < 10) ? "0" : "") + currentTime.getMinuteOfHour();

                updatedOfficeData.put("/appointmentsPublic/" + officeId + "/" + bookTime, true);

                currentTime = nextTime;
            }
        }
    }

    /**
     * @param officeHours (example: 7:30-12:30,13:30-15:30)
     * @return List of Intervals. Each Interval consists of Start time (LocalTime object) and end time.
     */
    private List<Interval> getIntervals(String officeHours) {
        ArrayList<Interval> intervals = new ArrayList<>();

        for (String interval : officeHours.split(",")) {
            intervals.add(new Interval(interval));
        }

        return intervals;
    }
}