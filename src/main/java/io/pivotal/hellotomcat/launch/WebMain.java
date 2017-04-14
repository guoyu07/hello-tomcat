package io.pivotal.hellotomcat.launch;

import io.pivotal.config.client.ConfigClientTemplate;
import io.pivotal.hellotomcat.cloud.CloudInstanceHolder;
import io.pivotal.spring.cloud.service.common.ConfigServerServiceInfo;
import io.pivotal.springcloud.ssl.CloudFoundryCertificateTruster;
import io.pivotal.tomcat.launch.TomcatLauncher;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.core.env.PropertySource;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

public class WebMain {

    public static final CloudFoundryCertificateTruster truster = new CloudFoundryCertificateTruster();

    public static void main(String[] args) throws Exception {
        WebMain main = new WebMain();
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

        // Uncomment the datasource in web.xml and context.xml if you want to use this method
        TomcatLauncher.configure()
                .withStandardContext()
                .defaultContextXml("META-INF/context.xml")
                .addEnvironment(source, "foo")
                .addEnvironment(source, "newprop")
                .addEnvironment(source, "foo.db")
//                .addContextResource(createContainerDataSource(getConnectionProperties("hello-db")))
                .apply()
                .launch();
    }

    private ServiceInfo getServiceInfo(String serviceName) {
        Cloud cloud = CloudInstanceHolder.getCloudInstance();
        if (cloud == null) {
            throw new CloudException("No suitable cloud connector found");
        }
        return cloud.getServiceInfo(serviceName);
    }

}