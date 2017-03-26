package cz.docta.bookingtimes;

import com.google.firebase.database.*;
import cz.docta.bookingtimes.generator.Generator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RegistrationServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String officeId = req.getParameter("officeid");

        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (officeId != null) {
            resp.getWriter().append("{\"success\":true}");
            generateTimes(officeId);
        } else {
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void generateTimes(String officeId) {
        FirebaseDatabase database = Generator.getDatabase();
        DatabaseReference ref = database.getReference("generatorInfo/" + officeId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Generator.generateAndSaveHours(dataSnapshot, database);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Registration request - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }
}