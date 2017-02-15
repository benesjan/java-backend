package cz.docta.bookingtimes;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import cz.docta.bookingtimes.gsonrequests.DeleteHolidayRequest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class DeleteHolidayServlet extends HttpServlet {

    FirebaseDatabase database = null;

    @Override
    public void init(ServletConfig config) {

        try {
            super.init(config);

            InputStream serviceKeyStream = this.getServletContext().getResourceAsStream("/WEB-INF/service_key.json");

            if (FirebaseApp.getApps().size() == 0) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredential(FirebaseCredentials.fromCertificate(serviceKeyStream))
                        .setDatabaseUrl("https://doctor-appointment-system.firebaseio.com/")
                        .build();

                FirebaseApp.initializeApp(options);
            }

            this.database = FirebaseDatabase.getInstance();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

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
        DeleteHolidayRequest deletionRequest = gson.fromJson(postData, DeleteHolidayRequest.class);


        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (deletionRequest != null && deletionRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyToken(deletionRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyToken(DeleteHolidayRequest deletionRequest) {
        FirebaseAuth.getInstance().verifyIdToken(deletionRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> {
                    String uid = decodedToken.getUid();
                    System.out.println(uid);
                    System.out.println(decodedToken);
                }).addOnFailureListener(e -> System.err.println("Token verification failed. deletionRequest: " + deletionRequest.toString()));
    }

}
