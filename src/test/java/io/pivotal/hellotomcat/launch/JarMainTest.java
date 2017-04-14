package io.pivotal.hellotomcat.launch;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;

public class JarMainTest extends Assert {
    private JarMain main = new JarMain();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    @Category(IntegrationTest.class)
    public void testRunStartsTomcat() throws Exception {
        environmentVariables.set("SPRING_PROFILES_ACTIVE", "development,db");
        main.run("http://localhost:8888");
    }

}