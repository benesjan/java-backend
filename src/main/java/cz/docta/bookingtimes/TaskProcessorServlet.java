package cz.docta.bookingtimes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.taskprocessors.HolidayAdditionsProcessor;
import cz.docta.bookingtimes.taskprocessors.HolidayDeletionsProcessor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class TaskProcessorServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("{\"success\":true}");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FirebaseDatabase database = Generator.getDatabase();

        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");
        resp.getWriter().append("{\"success\":true}");

        // TODO: fetch all tasks at once (activate when indexing is ready)
        database.getReference("taskQueue/holidayAdditions/").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    HolidayAdditionsProcessor.process(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("holidayDeletions request (TaskProcessorServlet), The read failed: " + databaseError.getCode());
            }
        });

        database.getReference("taskQueue/holidayDeletions/").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    HolidayDeletionsProcessor.process(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("holidayAdditions request (TaskProcessorServlet), The read failed: " + databaseError.getCode());
            }
        });


    }
}
