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

    private TomcatConfigurer tomcatConfigurer;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run(args[0]).getServer().await();
    }

    public Tomcat run(String configServerUrl) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcatConfigurer = new TomcatConfigurer(configServerUrl);
        Context ctx = tomcatConfigurer.createStandardContext(tomcat);
        PropertySource<?> source = tomcatConfigurer.loadConfiguration("hello-tomcat", new String[]{"default"});

        setupContext(ctx, source);

        tomcat.enableNaming();
        tomcat.start();
        return tomcat;
    }

    private void setupContext(Context ctx, PropertySource<?> source) throws Exception {

        ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "foo"));
//        ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "newprop"));
        if (useEncryptedConfig(source)) {
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
            credentials.put("url", service.getJdbcUrl());
            credentials.put("username", service.getUserName());
            credentials.put("password", service.getPassword());
        } else {
        	credentials.put("url", "jdbc:mysql://localhost/mysql?useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
            credentials.put("username", "root");
            credentials.put("password", "password");
        }
        credentials.put("name", PREFIX_JDBC + serviceName);
        credentials.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        credentials.put("factory", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
        return credentials;
    }

    private boolean useEncryptedConfig(PropertySource<?> source) {
        return source.getProperty("USE_ENCRYPT") != null &&
                Boolean.valueOf(source.getProperty("USE_ENCRYPT").toString());
    }
}
