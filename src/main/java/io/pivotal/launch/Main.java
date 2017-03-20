package io.pivotal.launch;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.core.env.PropertySource;

public class Main {

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
        ctx.getNamingResources().addResource(tomcatConfigurer.getResource("hello-db"));
    }

    private boolean isConfigServerLocal(PropertySource source) {
        return source.getProperty("configlocal") != null &&
                Boolean.valueOf(source.getProperty("configlocal").toString());
    }
}
