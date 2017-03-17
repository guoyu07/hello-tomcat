package servlet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;

import static launch.Main.PREFIX_JDBC;

@WebServlet(
        name = "MyServlet",
        urlPatterns = {"/hello"}
)
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String name = req.getParameter("name") != null ? req.getParameter("name") : "Mark";
        try {
            InitialContext initialContext = new InitialContext();
            Context envContext = (Context) initialContext.lookup("java:/comp/env");
            DataSource ds = (DataSource) envContext.lookup(PREFIX_JDBC + "hello-db");
            System.out.println("DataSource found: '" + ds + "'");
        } catch (Exception e) {
            System.out.println("DataSource NOT found");
            e.printStackTrace();
        }
        String foo = getFromEnvironment("foo");
        ServletOutputStream out = resp.getOutputStream();
        String message = "Hello " + name + "! Foo is " + foo;
        out.write(message.getBytes());
        out.flush();
        out.close();
    }

    public String getFromEnvironment(final String name) {
        if (name == null) return null;
        try {
            final Object object = ((Context) (new InitialContext().lookup("java:comp/env"))).lookup(name);
            if (object != null) return object.toString();
        } catch (Exception e) {
            System.out.println("Foo NOT found");
            e.printStackTrace();
        }
        return System.getenv(name);
    }
}
