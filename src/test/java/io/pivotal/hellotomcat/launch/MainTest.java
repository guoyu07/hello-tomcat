package io.pivotal.hellotomcat.launch;

import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;

public class MainTest extends Assert {
    private Main main = new Main();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    @Category(IntegrationTest.class)
    public void testRunStartsTomcat() throws Exception {
        environmentVariables.set("SPRING_PROFILES_ACTIVE", "development,db");
        Tomcat tomcat = main.run("http://localhost:8888");
        assertNotNull(tomcat);
        StandardServer server = (StandardServer) tomcat.getServer();
        assertNotNull(server);
        server.stopAwait();
    }

}