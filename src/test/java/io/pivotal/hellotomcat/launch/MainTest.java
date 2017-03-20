package io.pivotal.hellotomcat.launch;

import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.junit.Assert;
import org.junit.Test;

public class MainTest {
    private Main main = new Main();

    @Test
    public void testRunStartsTomcat() throws Exception {
        Tomcat tomcat = main.run("http://localhost:8888");
        Assert.assertNotNull(tomcat);
        StandardServer server = (StandardServer) tomcat.getServer();
        Assert.assertNotNull(server);
        server.stopAwait();
    }

}