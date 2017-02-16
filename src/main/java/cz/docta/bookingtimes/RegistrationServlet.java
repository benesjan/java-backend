package cz.docta.bookingtimes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.gsonrequests.RegistrationRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class RegistrationServlet extends FirebaseServlet {

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

        if (registrationRequest != null && registrationRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyTokenAndGenerate(registrationRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyTokenAndGenerate(RegistrationRequest registrationRequest) {
        FirebaseAuth.getInstance(Generator.getServiceApp()).verifyIdToken(registrationRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> this.generateTimes(Generator.getUserDatabase(decodedToken.getUid()), registrationRequest))
                .addOnFailureListener(e -> System.err.println("Token verification failed. RegistrationRequest: " + registrationRequest));
    }

    private void generateTimes(FirebaseDatabase database, RegistrationRequest registrationRequest) {
        DatabaseReference ref = database.getReference("generatorInfo/" + registrationRequest.getOfficeId());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Generator.generateAndSaveHours(dataSnapshot, database);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Registration request - officeId: " + registrationRequest.getOfficeId() + ", The read failed: " + databaseError.getCode());
            }
        });
    }
}