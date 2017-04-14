package io.pivotal.hellotomcat.launch;

import io.pivotal.config.client.ConfigClientTemplate;
import io.pivotal.hellotomcat.cloud.CloudInstanceHolder;
import io.pivotal.spring.cloud.service.common.ConfigServerServiceInfo;
import io.pivotal.springcloud.ssl.CloudFoundryCertificateTruster;
import io.pivotal.tomcat.launch.TomcatLauncher;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.cloud.service.common.MysqlServiceInfo;
import org.springframework.core.env.PropertySource;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class JarMain {

    public static final String PREFIX_JDBC = "jdbc/";

    public static final CloudFoundryCertificateTruster truster = new CloudFoundryCertificateTruster();

    public static void main(String[] args) throws Exception {
        JarMain main = new JarMain();
        if (args.length == 1) {
            main.run(args[0]);
        } else {
            main.run(null);
        }
    }

    public void run(String configServerUrl) throws Exception {
        RestTemplate restTemplate = null;
        if (configServerUrl == null || "".equals(configServerUrl)) {
            ConfigServerServiceInfo service = (ConfigServerServiceInfo) getServiceInfo("config-server");
            configServerUrl = service.getUri();
            if (service.getAccessTokenUri() != null) {
                ClientCredentialsResourceDetails ccrd = new ClientCredentialsResourceDetails();
                ccrd.setAccessTokenUri(service.getAccessTokenUri());
                ccrd.setClientId(service.getClientId());
                ccrd.setClientSecret(service.getClientSecret());
                restTemplate = new OAuth2RestTemplate(ccrd);
            }
        }
        // If running locally, create a system property in the run configuration: "SPRING_PROFILES_ACTIVE", "development,db"
        ConfigClientTemplate configClient = new ConfigClientTemplate<>(restTemplate, configServerUrl, "hello-tomcat", null, false);

        System.out.println("Getting prop directly from config server: " + configClient.getProperty("foo"));

        PropertySource<?> source = configClient.getPropertySource();

        TomcatLauncher.configure()
                .withStandardContext()
                .addEnvironment(source, "foo")
                .addEnvironment(source, "newprop")
                .addEnvironment(source, "foo.db")
                .addContextResource(createContainerDataSource(getConnectionProperties("hello-db")))
                .apply()
                .launch();
    }

    private ContextResource createContainerDataSource(Map<String, Object> credentials) {
        System.out.println("creds: " + credentials);
        Assert.notNull(credentials, "Service credentials cannot be null");
        Assert.notNull(credentials.get("name"), "Service name is null");
        Assert.notNull(credentials.get("driverClassName"), "Driver class name is null");
        Assert.notNull(credentials.get("url"), "Jdbc url is null");
        Assert.notNull(credentials.get("username"), "Username is null");
        Assert.notNull(credentials.get("password"), "Password is null");
        ContextResource resource = new ContextResource();
        resource.setAuth("Container");
        resource.setType("javax.sql.DataSource");
        resource.setName(credentials.get("name").toString());
        resource.setProperty("driverClassName", credentials.get("driverClassName"));
        resource.setProperty("url", credentials.get("url"));
        if (credentials.get("factory") != null) {
            resource.setProperty("factory", credentials.get("factory"));
        }
        if (credentials.get("connectionProperties") != null) {
            resource.setProperty("connectionProperties", credentials.get("connectionProperties"));
        }
        resource.setProperty("username", credentials.get(("username")));
        resource.setProperty("password", credentials.get("password"));
        resource.setProperty("removeAbandonedTimeout", "60");
        resource.setProperty("testWhileIdle", "true");
        resource.setProperty("timeBetweenEvictionRunsMillis", "300000");

        return resource;
    }

    private ServiceInfo getServiceInfo(String serviceName) {
        Cloud cloud = CloudInstanceHolder.getCloudInstance();
        if (cloud == null) {
            throw new CloudException("No suitable cloud connector found");
        }
        return cloud.getServiceInfo(serviceName);
    }

    private Map<String, Object> getConnectionProperties(String serviceName) {
        Map<String, Object> credentials = new HashMap<>();
        Cloud cloud = CloudInstanceHolder.getCloudInstance();
        if (cloud != null) {
            System.out.println("We're in the cloud!");
            MysqlServiceInfo service = (MysqlServiceInfo) cloud.getServiceInfo(serviceName);
            credentials.put("url", service.getJdbcUrl());
            credentials.put("username", service.getUserName());
            credentials.put("password", service.getPassword());
            credentials.put("driverClassName", "org.mariadb.jdbc.Driver");
        } else {
            credentials.put("url", "jdbc:mysql://localhost/mysql?useSSL=false");
            credentials.put("username", "root");
            credentials.put("password", "password");
            credentials.put("connectionProperties", "useUnicode=true;useJDBCCompliantTimezoneShift=true;useLegacyDatetimeCode=false;serverTimezone=UTC;");
            credentials.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        }
        credentials.put("name", PREFIX_JDBC + serviceName);
        credentials.put("factory", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
        return credentials;
    }

    private boolean useEncryptedConfig(PropertySource<?> source) {
        return source.getProperty("USE_ENCRYPT") != null
                && Boolean.valueOf(source.getProperty("USE_ENCRYPT").toString());
    }
}
