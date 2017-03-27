package io.pivotal.hellotomcat.launch;

import io.pivotal.hellotomcat.cloud.CloudInstanceHolder;
import io.pivotal.tomcat.launch.TomcatLaunchConfigurer;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.service.common.MysqlServiceInfo;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class Main {

	public static final String PREFIX_JDBC = "jdbc/";

	private TomcatLaunchConfigurer tomcatConfigurer;

	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.run(args[0]).getServer().await();
	}

	public Tomcat run(String configServerUrl) throws Exception {
		Tomcat tomcat = new Tomcat();
		tomcatConfigurer = new TomcatLaunchConfigurer(configServerUrl, "foo", new String[] { "default", "development", "db" });
		Context ctx = tomcatConfigurer.createStandardContext(tomcat);
		PropertySource<?> source = tomcatConfigurer.getPropertySource();

		setupContextEnvironment(ctx, source);

		tomcat.enableNaming();
		tomcat.start();
		
		// You must do this AFTER tomcat start is called (because of lifecycle hooks in tomcat)
		setupContextResource(ctx, source);
		
		return tomcat;
	}

	private void setupContextEnvironment(Context ctx, PropertySource<?> source) throws Exception {
		ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "foo"));
		ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "newprop"));
		ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "foo.db"));
		if (useEncryptedConfig(source)) {
			ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "secret"));
			ctx.getNamingResources().addEnvironment(tomcatConfigurer.getEnvironment(source, "custom-secret"));
		}
	}

	private void setupContextResource(Context ctx, PropertySource<?> source) {
		Cloud cloud = CloudInstanceHolder.getCloudInstance();
		if (cloud != null) {
			System.out.println("Creating datasource from spring-cloud-connector and adding extra props to it");
			ContextResource resource = tomcatConfigurer.createContainerDataSource(this.getServiceConfig("hello-db"));
			resource.setProperty("removeAbandonedTimeout", "60");
			resource.setProperty("testWhileIdle", "true");
			resource.setProperty("timeBetweenEvictionRunsMillis", "300000");
			ctx.getNamingResources().addResource(resource);
		} else if (ctx.getNamingResources().findResource(PREFIX_JDBC + "hello-db") == null) {
			System.out.println("Container datasource already registered in context.");
		} else {
			System.out.println("Container datasource not registered in context.");
			ctx.getNamingResources().addResource(tomcatConfigurer.createContainerDataSource(this.getServiceConfig("hello-db")));
		}
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
