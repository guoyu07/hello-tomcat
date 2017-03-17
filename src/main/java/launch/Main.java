package launch;

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
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.service.common.MysqlServiceInfo;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Main {
    public static final String PREFIX_JDBC = "jdbc/";

    public static final String HTTPS_SCHEME = "https://";

    public static final String HTTP_SCHEME = "http://";

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static final Object monitor = new Object();

    private static volatile Cloud cloud;

    private final ConfigurableEnvironment environment = new StandardEnvironment();

    private ConfigServicePropertySourceLocator locator;

    private final RestTemplate restTemplate = new RestTemplate();

    private File getRootFolder() {
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

        Main main = new Main();
        main.loadConfiguration(args[0]);

        File root = main.getRootFolder();
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
        File additionWebInfClassesFolder = new File(root.getAbsolutePath(), "build/classes/main");
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
        PropertySource source = main.locator.locate(main.environment);

        ctx.getNamingResources().addEnvironment(getEnvironment(source));
        ctx.getNamingResources().addResource(getResource(source, "hello-db"));

        tomcat.enableNaming();
        tomcat.start();
        tomcat.getServer().await();
    }

    private void loadConfiguration(String configServerUrl) {
        if (configServerUrl == null || configServerUrl.isEmpty()) {
            throw new RuntimeException("You MUST set the config server URI");
        }
        if (!configServerUrl.startsWith(HTTP_SCHEME) && !configServerUrl.startsWith(HTTP_SCHEME)) {
            throw new RuntimeException("You MUST put the URI scheme in front of the config server URI");
        }
        System.out.println("configServerUrl is '" + configServerUrl + "'");
        ConfigClientProperties defaults = new ConfigClientProperties(this.environment);
        defaults.setFailFast(true);
        defaults.setUri(configServerUrl);
        DefaultUriTemplateHandler uriTemplateHandler = new DefaultUriTemplateHandler();
        uriTemplateHandler.setBaseUrl(configServerUrl);
        this.restTemplate.setUriTemplateHandler(uriTemplateHandler);
        this.locator = new ConfigServicePropertySourceLocator(defaults);
        this.locator.setRestTemplate(restTemplate);
    }

    private static ContextResource getResource(PropertySource source, String serviceName) {
        Map<String, Object> credentials = new HashMap<>();
        Cloud cloud = getCloudInstance();
        if (source != null) {
            System.out.println("We're running config server!");
            Object foo = source.getProperty("foo");
            logger.info("foo is set to: " + foo);
        }
        if (cloud != null) {
            System.out.println("We're in the cloud!");
            MysqlServiceInfo service = (MysqlServiceInfo) cloud.getServiceInfo(serviceName);
            credentials.put("jdbcUrl", service.getJdbcUrl());
            credentials.put("username", service.getUserName());
            credentials.put("password", service.getPassword());
        } else {
            System.out.println("We're running locally!");
            if (source != null) {
                Object jdbcUrl = source.getProperty("jdbcUrl");
                logger.info("jdbcUrl is set to: " + jdbcUrl);
                credentials.put("jdbcUrl", source.getProperty("jdbcUrl"));
                credentials.put("username", source.getProperty("username"));
                credentials.put("password", source.getProperty("password"));
            } else {
                System.out.println("We're running without config server!");
                credentials.put("jdbcUrl", "jdbc:mysql://localhost/mysql?useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
                credentials.put("username", "root");
                credentials.put("password", "password");
            }
        }

        System.out.println(credentials);

        ContextResource resource = new ContextResource();
        resource.setName(PREFIX_JDBC + serviceName);
        resource.setAuth("Container");
        resource.setType("javax.sql.DataSource");
        resource.setProperty("driverClassName", "com.mysql.cj.jdbc.Driver");
        resource.setProperty("url", credentials.get("jdbcUrl"));
        resource.setProperty("factory", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
        resource.setProperty("username", credentials.get(("username")));
        resource.setProperty("password", credentials.get("password"));

        return resource;
    }

    private static ContextEnvironment getEnvironment(PropertySource source) {
        ContextEnvironment env = new ContextEnvironment();
        env.setName("foo");
        env.setValue(source.getProperty("foo").toString());
        env.setType("java.lang.String");
        env.setOverride(false);
        return env;
    }

    private static Cloud getCloudInstance() {
        if (null == cloud) {
            synchronized (monitor) {
                if (null == cloud) {
                    try {
                        cloud = new CloudFactory().getCloud();
                    } catch (CloudException e) {
                        //ignore
                        return null;
                    }
                }
            }
        }
        return cloud;
    }
}
