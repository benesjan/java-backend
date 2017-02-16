package cz.docta.bookingtimes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.gsonrequests.AddHolidayRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class AddHolidayServlet extends FirebaseServlet {

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
        AddHolidayRequest deleteHolidayRequest = gson.fromJson(postData, AddHolidayRequest.class);

        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (deleteHolidayRequest != null && deleteHolidayRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyTokenAndAddHoliday(deleteHolidayRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyTokenAndAddHoliday(AddHolidayRequest addHolidayRequest) {
        FirebaseAuth.getInstance(Generator.getServiceApp()).verifyIdToken(addHolidayRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> this.addHoliday(Generator.getUserDatabase(decodedToken.getUid()), addHolidayRequest))
                .addOnFailureListener(e -> System.err.println("Token verification failed. addHolidayRequest: " + addHolidayRequest));
    }

    private void addHoliday(FirebaseDatabase database, AddHolidayRequest addHolidayRequest) {
        // TODO: delete booking times if necessary
        Long holidayId = addHolidayRequest.getStartAt();
        String officeId = addHolidayRequest.getOfficeId();
        Map<String, Object> objectToSave = new HashMap<>();

        objectToSave.put("/generatorInfo/" + officeId + "/holidays/" + holidayId, addHolidayRequest.getEndAt());
        objectToSave.put("/officeHolidays/" + officeId + "/" + holidayId, addHolidayRequest.getOfficeHolidaysMap());

        database.getReference("generatorInfo/" + officeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Generator.generateHoursInInterval(dataSnapshot, objectToSave, addHolidayRequest.getStartAt(), addHolidayRequest.getEndAt(), true);

                database.getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
                    if (databaseError != null) {
                        System.err.println("OfficeId: " + officeId + ", Holiday saving failed " + databaseError.getMessage());
                    } else {
                        System.out.println("OfficeId: " + officeId + ", Holiday saved successdully.");
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Registration request - officeId: " + officeId + ", The read failed: " + databaseError.getCode());
            }
        });
    }
}
