package cz.docta.bookingtimes.abstractpackage;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import cz.docta.bookingtimes.generator.Generator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.InputStream;

/**
 * @author Jan Benes (janbenes1234@gmail.com)
 */
public abstract class FirebaseServlet extends HttpServlet {
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

                FirebaseApp.initializeApp(options, Generator.getServiceAppName());
            }

        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}