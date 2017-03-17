# hello-tomcat

A simple app that demonstrates how to run a Java Web Application as a JAR using embedded Tomcat 
and JNDI to register a data source from properties read from the environment.

### Run local

Start ConfigServer
* Clone the sample [config-server](https://github.com/spring-cloud-samples/configserver) and point it to a git repo that contains your config. 

    ```bash
    $ ./mvnw spring-boot:run -Dspring.cloud.config.server.git.uri=https://github.com/malston/config-repo
    ```

```bash
$ ./gradlew clean shadowJar
$ java -jar build/libs/hello-tomcat-0.0.1-SNAPSHOT-all.jar "http://localhost:8888"
```

### Run on PCF

Push config-server and change the `JBP_CONFIG_JAVA_MAIN` variable inside the `manifest.yml` to point to the config-server url.

```bash
$ ./gradlew clean shadowJar
$ cf cs p-mysql 100mb-dev hello-db
$ cf push
```