package cz.docta.bookingtimes;

import com.google.firebase.database.*;
import com.google.gson.Gson;
import cz.docta.bookingtimes.gsonrequests.RegistrationRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class RegistrationServlet extends BookingTimesServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Gson gson = new Gson();

        String postData = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        RegistrationRequest registrationRequest = gson.fromJson(postData, RegistrationRequest.class);


        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (registrationRequest != null && registrationRequest.getOfficeId() != null) {
            resp.getWriter().append("{\"success\":true}");
            this.handleFirebaseLogic(registrationRequest.getOfficeId());
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void handleFirebaseLogic(String officeId) {
        FirebaseDatabase database = this.database;
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