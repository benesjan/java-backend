package cz.docta.bookingtimes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import cz.docta.bookingtimes.abstractpackage.FirebaseServlet;
import cz.docta.bookingtimes.generator.Generator;
import cz.docta.bookingtimes.gsonrequests.InfoChangedRequest;
import org.joda.time.DateTime;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public class InfoChangedServlet extends FirebaseServlet {

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
        InfoChangedRequest infoChangedRequest = gson.fromJson(postData, InfoChangedRequest.class);

        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Content-Type", "application/json");

        if (infoChangedRequest != null && infoChangedRequest.areAttributesValid()) {
            resp.getWriter().append("{\"success\":true}");
            this.verifyTokenAndRegenerate(infoChangedRequest);
        } else {
            System.err.println("Request parsing failed.");
            resp.getWriter().append("{\"success\":false}");
        }
    }

    private void verifyTokenAndRegenerate(InfoChangedRequest infoChangedRequest) {
        FirebaseAuth.getInstance(Generator.getServiceApp()).verifyIdToken(infoChangedRequest.getIdToken())
                .addOnSuccessListener(decodedToken -> this.regenerateTimes(Generator.getUserDatabase(decodedToken.getUid()), infoChangedRequest))
                .addOnFailureListener(e -> System.err.println("Token verification failed. infoChangedRequest: " + infoChangedRequest));
    }

    private void regenerateTimes(FirebaseDatabase database, InfoChangedRequest infoChangedRequest) {
        DateTime generateSince = new DateTime(infoChangedRequest.getGenerateSince());
        // TODO
    }

}
