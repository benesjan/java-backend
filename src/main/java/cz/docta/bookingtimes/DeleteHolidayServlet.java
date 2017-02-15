package cz.docta.bookingtimes;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.gsonrequests.DeleteHolidayRequest;

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
public class DeleteHolidayServlet extends FirebaseServlet {

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
        DeleteHolidayRequest deleteHolidayRequest = gson.fromJson(postData, DeleteHolidayRequest.class);


        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (deleteHolidayRequest != null && deleteHolidayRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyTokenAndDeleteHoliday(deleteHolidayRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyTokenAndDeleteHoliday(DeleteHolidayRequest deleteHolidayRequest) {
        FirebaseAuth.getInstance(Generator.getServiceApp()).verifyIdToken(deleteHolidayRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> {
                    this.deleteHoliday(Generator.getUserDatabase(decodedToken.getUid()), deleteHolidayRequest);
                }).addOnFailureListener(e -> System.err.println("Token verification failed. deleteHolidayRequest: " + deleteHolidayRequest));
    }

    private void deleteHoliday(FirebaseDatabase database, DeleteHolidayRequest deleteHolidayRequest) {
        // TODO: generate booking times if necessary

        Map objectToSave = new HashMap();
        objectToSave.put("/officeHolidays/" + deleteHolidayRequest.getOfficeId() + "/" + deleteHolidayRequest.getHolidayId(), null);
        objectToSave.put("/generatorInfo/" + deleteHolidayRequest.getOfficeId() + "/holidays/" + deleteHolidayRequest.getHolidayId(), null);

        database.getReference("/").updateChildren(objectToSave, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.err.println("OfficeId: " + deleteHolidayRequest.getOfficeId() + ", Holiday deleting failed " + databaseError.getMessage());
            } else {
                System.out.println("OfficeId: " + deleteHolidayRequest.getOfficeId() + ", Holiday deleted successdully.");
            }
        });
    }
}
