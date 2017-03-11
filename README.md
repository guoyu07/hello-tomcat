# hello-tomcat

A simple app that demonstrates how to run a Java Web Application as a JAR using embedded Tomcat 
and JNDI to register a data source from properties read from the environment.

### Run local

```bash
$ ./gradlew clean shadowJar
$ java -jar build/libs/hello-tomcat-0.0.1-SNAPSHOT-all.jar
```

### Run on PCF

```bash
$ ./gradlew clean shadowJar
$ cf cs p-mysql 100mb-dev hello-db
$ cf push
```