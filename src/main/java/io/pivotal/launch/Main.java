package io.pivotal.launch;

import io.pivotal.config.LocalConfigFileEnvironmentProcessor;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.descriptor.web.ContextResource;
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

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.pivotal.config.LocalConfigFileEnvironmentProcessor.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME;

public class Main {
    public static final String PREFIX_JDBC = "jdbc/";

    public static final String HTTPS_SCHEME = "https://";

    public static final String HTTP_SCHEME = "http://";

    private static final Object monitor = new Object();

    private static volatile Cloud cloud;

    private final ConfigurableEnvironment environment = new StandardEnvironment();

    private final TomcatLaunchHelper tomcatLaunchHelper = new TomcatLaunchHelper();

    private ConfigServicePropertySourceLocator locator;

    private final RestTemplate restTemplate = new RestTemplate();

    private final LocalConfigFileEnvironmentProcessor localLocalConfigFileEnvironmentProcessor = new LocalConfigFileEnvironmentProcessor();

    public static void main(String[] args) throws Exception {

        Main main = new Main();
        PropertySource source = main.loadConfiguration(args[0]);
        Tomcat tomcat = new Tomcat();

        Context ctx = main.getContext(tomcat);
        ctx.getNamingResources().addEnvironment(main.getEnvironment(source, "foo"));
        if (main.isConfigServerLocal(source)) {
            ctx.getNamingResources().addEnvironment(main.getEnvironment(source, "secret"));
            ctx.getNamingResources().addEnvironment(main.getEnvironment(source, "custom-secret"));
        }
        ctx.getNamingResources().addResource(main.getResource(source, "hello-db"));

        tomcat.enableNaming();
        tomcat.start();
        tomcat.getServer().await();
    }

    private PropertySource loadConfiguration(String configServerUrl) {
        if (configServerUrl == null || configServerUrl.isEmpty()) {
            throw new RuntimeException("You MUST set the config server URI");
        }
        if (!configServerUrl.startsWith(HTTP_SCHEME) && !configServerUrl.startsWith(HTTPS_SCHEME)) {
            throw new RuntimeException("You MUST put the URI scheme in front of the config server URI");
        }
        System.out.println("configServerUrl is '" + configServerUrl + "'");
        ConfigClientProperties defaults = new ConfigClientProperties(this.environment);
        defaults.setFailFast(false);
        defaults.setUri(configServerUrl);
        DefaultUriTemplateHandler uriTemplateHandler = new DefaultUriTemplateHandler();
        uriTemplateHandler.setBaseUrl(configServerUrl);
        this.restTemplate.setUriTemplateHandler(uriTemplateHandler);
        this.locator = new ConfigServicePropertySourceLocator(defaults);
        this.locator.setRestTemplate(restTemplate);
        PropertySource source = this.locator.locate(this.environment);
        this.localLocalConfigFileEnvironmentProcessor.processEnvironment(environment, source);

        return source == null ? this.environment.getPropertySources().get(APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME) : source;
    }

    private Context getContext(Tomcat tomcat) throws IOException, ServletException {
        return tomcatLaunchHelper.getContext(tomcat);
    }

    private ContextResource getResource(PropertySource source, String serviceName) {
        Map<String, Object> credentials = new HashMap<String, Object>();
        Cloud cloud = getCloudInstance();
        if (cloud != null) {
            System.out.println("We're in the cloud!");
            MysqlServiceInfo service = (MysqlServiceInfo) cloud.getServiceInfo(serviceName);
            credentials.put("jdbcUrl", service.getJdbcUrl());
            credentials.put("username", service.getUserName());
            credentials.put("password", service.getPassword());
        }
        credentials.put("serviceName", PREFIX_JDBC + serviceName);
        credentials.put("driverClassName", "com.mysql.cj.jdbc.Driver");

        return tomcatLaunchHelper.getResource(credentials);
    }

    private ContextEnvironment getEnvironment(PropertySource source, String name) {
        return tomcatLaunchHelper.getEnvironment(name, source.getProperty(name).toString());
    }

    private boolean isConfigServerLocal(PropertySource source) {
        return source.getProperty("configlocal") != null &&
                Boolean.valueOf(source.getProperty("configlocal").toString());
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
