package io.pivotal.hellotomcat.servlet;

import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

@WebServlet("/ini")
public class ServicesAsIniFileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CloudFactory cloudFactory = new CloudFactory();
        Cloud cloud = null;
        try {
            cloud = cloudFactory.getCloud();
        } catch (CloudException e) {
            throw new ServletException(e);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");

        try (PrintWriter out = response.getWriter()) {
            Properties properties = cloud.getCloudProperties();
            properties.forEach((name, value) -> out.println(name + " = " + value));
            out.println();
        }
    }
}
