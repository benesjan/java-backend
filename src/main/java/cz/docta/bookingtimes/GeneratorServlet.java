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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    private void generateAndSaveHours(DataSnapshot office) {
        String officeId = office.getKey();
        Integer visitLength = office.child("visitLength").getValue(Integer.class);
        Integer numberOfDays = office.child("numberOfDays").getValue(Integer.class);
        DateTime firstDate = new DateTime(); // Set to today for development purposes

        DatabaseReference ref = this.database.getReference("/appointmentsPublic/" + officeId);
        Map<Integer, Boolean> officeHours = new HashMap<>();

        DateTime currentDate = firstDate;
        DataSnapshot dayHours;
        for (int i = 0; i < numberOfDays; i++) {
            dayHours = office.child("officeHours/" + (currentDate.getDayOfWeek() - 1));
            if (dayHours.child("available").getValue(Boolean.class)) {
                this.generateHours(officeHours, visitLength, this.getIntervals(dayHours.child("hours").getValue(String.class)), currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

    }

    private void generateHours(Map<Integer, Boolean> dayHours, Integer visitLength, List<Interval> intervals, DateTime date) {
        Integer dayDate = date.getDayOfMonth();
        Integer month = date.getMonthOfYear();

        String prefix = "" + date.getYear() + ((month < 10) ? "0" : "") + month + ((dayDate < 10) ? "0" : "") + dayDate;

        for (Interval interval : intervals) {
            LocalTime currentTime = interval.start;
            LocalTime nextTime;

            while ((nextTime = currentTime.plusMinutes(visitLength)).compareTo(interval.end) != 1) {
//                System.out.println(currentTime);
                String bookTime = prefix + ((currentTime.getHourOfDay() < 10) ? "0" : "") + currentTime.getHourOfDay()
                        + ((currentTime.getMinuteOfHour() < 10) ? "0" : "") + currentTime.getMinuteOfHour();
                System.out.println(bookTime);
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