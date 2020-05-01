package cz.docta.bookingtimes;

import com.google.firebase.database.*;
import cz.docta.bookingtimes.generator.Generator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CronTimesServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FirebaseDatabase database = Generator.getDatabase();

        resp.getWriter().append("{\"success\":true}");

        DatabaseReference ref = database.getReference("generatorInfo");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot office : dataSnapshot.getChildren()) {
                    Generator.generateAndSaveHours(office, database);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("The read failed: " + databaseError.getCode());
            }
        });
    }
}