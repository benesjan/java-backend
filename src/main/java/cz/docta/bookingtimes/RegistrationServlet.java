package cz.docta.bookingtimes;

import com.google.firebase.database.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class RegistrationServlet extends BookingTimesServlet {

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FirebaseDatabase database = this.database;

        String officeId = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        resp.getWriter().println("OK");

        DatabaseReference ref = database.getReference("generatorInfo/" + officeId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                generateAndSaveHours(dataSnapshot, database);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("The read failed: " + databaseError.getCode());
            }
        });

    }
}