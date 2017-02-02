package cz.docta.bookingtimes;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import org.joda.time.DateTime;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
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
        System.out.println(firstDate.getDayOfWeek());
        for (int i = 0; i<numberOfDays; i++) {
//            if (office.child(""))
//            this.generateHours(officeHours, visitLength, of);
            currentDate = currentDate.plusDays(1);
        }
    }

    private void generateHours(Map<Integer, Boolean> dayHours, Integer visitLength, String officeHours, DateTime date) {
    }
}