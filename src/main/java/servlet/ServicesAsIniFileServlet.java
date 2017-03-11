package servlet;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryEnvironmentException;
import io.pivotal.labs.cfenv.CloudFoundryService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/ini")
public class ServicesAsIniFileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CloudFoundryEnvironment environment;
        try {
            environment = new CloudFoundryEnvironment(System::getenv);
        } catch (CloudFoundryEnvironmentException e) {
            throw new ServletException(e);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");

        try (PrintWriter out = response.getWriter()) {
            for (String serviceName : environment.getServiceNames()) {
                CloudFoundryService service = environment.getService(serviceName);
                out.println("[" + service.getName() + "]");
                out.println("label = " + service.getLabel());
                if (service.getPlan() != null) out.println("plan = " + service.getPlan());
                out.println("tags = " + service.getTags().stream().collect(Collectors.joining(", ")));
                Map<String, Object> credentials = service.getCredentials();
                credentials.forEach((name, value) -> out.println("credentials." + name + " = " + value));
                out.println();
            }
        }
    }
}
