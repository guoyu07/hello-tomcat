package launch;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryService;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.EmptyResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.scan.Constants;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Main {

    private static File getRootFolder() {
        try {
            File root;
            String runningJarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().replaceAll("\\\\", "/");
            int lastIndexOf = runningJarPath.lastIndexOf("/build/libs/");
            if (lastIndexOf < 0) {
                root = new File("");
            } else {
                root = new File(runningJarPath.substring(0, lastIndexOf));
            }
            System.out.println("application resolved root folder: " + root.getAbsolutePath());
            return root;
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws Exception {

        CloudFoundryEnvironment environment = new CloudFoundryEnvironment(System::getenv);

        File root = getRootFolder();
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
        Tomcat tomcat = new Tomcat();
        Path tempPath = Files.createTempDirectory("tomcat-base-dir");
        tomcat.setBaseDir(tempPath.toString());

        //The port that we should run on can be set into an environment variable
        //Look for that variable and default to 8080 if it isn't there.
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }

        tomcat.setPort(Integer.valueOf(webPort));
        File webContentFolder = new File(root.getAbsolutePath(), "src/main/resources/");
        if (!webContentFolder.exists()) {
            //webContentFolder = Files.createTempDirectory("default-doc-base").toFile();
            webContentFolder = new File(root.getAbsolutePath());
        }
        System.out.println("webContentFolder is '" + webContentFolder.getAbsolutePath() + "'");
        StandardContext ctx = (StandardContext) tomcat.addWebapp("", webContentFolder.getAbsolutePath());

        //Set execution independent of current thread context classloader (compatibility with exec:java mojo)
        ctx.setParentClassLoader(Main.class.getClassLoader());

        //Disable TLD scanning by default
        if (System.getProperty(Constants.SKIP_JARS_PROPERTY) == null && System.getProperty(Constants.SKIP_JARS_PROPERTY) == null) {
            System.out.println("disabling TLD scanning");
            StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) ctx.getJarScanner().getJarScanFilter();
            jarScanFilter.setTldSkip("*");
        }

        System.out.println("configuring app with basedir: " + webContentFolder.getAbsolutePath());

        // Declare an alternative location for your "WEB-INF/classes" dir
        // Servlet 3.0 annotation will work
        File additionWebInfClassesFolder = new File(root.getAbsolutePath(), "build/classes");
        WebResourceRoot resources = new StandardRoot(ctx);

        WebResourceSet resourceSet;
        if (additionWebInfClassesFolder.exists()) {
            resourceSet = new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClassesFolder.getAbsolutePath(), "/");
            System.out.println("loading WEB-INF/classes from '" + additionWebInfClassesFolder.getAbsolutePath() + "'");
        } else {
            additionWebInfClassesFolder = new File(root.getAbsolutePath());
            if (additionWebInfClassesFolder.exists()) {
                resourceSet = new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClassesFolder.getAbsolutePath(), "/");
                System.out.println("loading WEB-INF/classes from '" + additionWebInfClassesFolder.getAbsolutePath() + "'");
            } else {
                resourceSet = new EmptyResourceSet(resources);
            }
        }
        resources.addPreResources(resourceSet);
        ctx.setResources(resources);

        ctx.getNamingResources().addEnvironment(getEnvironment());
        ctx.getNamingResources().addResource(getResource(environment, "hello-db"));

        tomcat.enableNaming();
        tomcat.start();
        tomcat.getServer().await();
    }

    private static ContextResource getResource(CloudFoundryEnvironment environment, String serviceName) {
        CloudFoundryService service = environment.getService(serviceName);

        Map<String, Object> credentials = service.getCredentials();

        ContextResource resource = new ContextResource();
        resource.setName("jdbc/demo");
        resource.setAuth("Container");
        resource.setType("javax.sql.DataSource");
        resource.setProperty("driverClassName","com.mysql.jdbc.Driver");
        resource.setProperty("url", credentials.get("jdbcUrl"));
        resource.setProperty("factory", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
        resource.setProperty("username", credentials.get(("username")));
        resource.setProperty("password", credentials.get("password"));

        return resource;
    }

    private static ContextEnvironment getEnvironment() {
        ContextEnvironment env = new ContextEnvironment();
        env.setName("lastname");
        env.setValue("Alston");
        env.setType("java.lang.String");
        env.setOverride(false);
        return env;
    }
}
