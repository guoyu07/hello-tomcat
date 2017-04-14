package io.pivotal.hellotomcat.servlet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;

import static io.pivotal.hellotomcat.launch.JarMain.PREFIX_JDBC;

public class HelloServlet extends HttpServlet {

	private static final long serialVersionUID = -7408787806734219962L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

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
		String message = "Hello " + name + "! Foo is " + foo;
		try (PrintWriter out = resp.getWriter()) {
			out.println(message);
			String fooDb = getFromEnvironment("foo.db");
			String newProp = getFromEnvironment("newprop");
			out.println("foo.db: " + fooDb);
			out.println("newprop: " + newProp);
			out.println();
		}
	}

	public String getFromEnvironment(final String name) {
		if (name == null)
			return null;
		try {
			final Object object = ((Context) (new InitialContext().lookup("java:comp/env"))).lookup(name);
			if (object != null)
				return object.toString();
		} catch (Exception e) {
			System.out.println("Foo NOT found");
			e.printStackTrace();
		}
		return System.getenv(name);
	}
}
