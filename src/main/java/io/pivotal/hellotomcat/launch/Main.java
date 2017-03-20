package io.pivotal.hellotomcat.launch;

import io.pivotal.hellotomcat.cloud.CloudInstanceHolder;
import io.pivotal.launch.TomcatConfigurer;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.service.common.MysqlServiceInfo;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static final String PREFIX_JDBC = "jdbc/";

    private final TomcatConfigurer tomcatConfigurer = new TomcatConfigurer();

    public static void main(String[] args) throws Exception {

        Main main = new Main();
        main.run(args[0]).getServer().await();
    }

    public Tomcat run(String configServerUrl) throws Exception {
        Tomcat tomcat = new Tomcat();
        Context ctx = tomcatConfigurer.createStandardContext(tomcat);
        PropertySource source = tomcatConfigurer.loadConfiguration(configServerUrl);

        setupContext(ctx, source);

        tomcat.enableNaming();
        tomcat.start();
        return tomcat;
    }

    private void setupContext(Context ctx, PropertySource source) throws Exception {

        ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "foo"));
        if (isConfigServerLocal(source)) {
            ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "secret"));
            ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "custom-secret"));
        }
        ctx.getNamingResources().addResource(tomcatConfigurer.getResource(this.getServiceConfig("hello-db")));
    }

    private Map<String, Object> getServiceConfig(String serviceName) {
        Map<String, Object> credentials = new HashMap<>();
        Cloud cloud = CloudInstanceHolder.getCloudInstance();
        if (cloud != null) {
            System.out.println("We're in the cloud!");
            MysqlServiceInfo service = (MysqlServiceInfo) cloud.getServiceInfo(serviceName);
            credentials.put("jdbcUrl", service.getJdbcUrl());
            credentials.put("username", service.getUserName());
            credentials.put("password", service.getPassword());
        }
        credentials.put("serviceName", PREFIX_JDBC + serviceName);
        credentials.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        return credentials;
    }

    private boolean isConfigServerLocal(PropertySource source) {
        return source.getProperty("CONFIG_LOCAL") != null &&
                Boolean.valueOf(source.getProperty("CONFIG_LOCAL").toString());
    }
}