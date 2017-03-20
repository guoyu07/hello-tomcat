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

### Encrypt/Decrypt Config Values

#### Encrypt/decrypt with custom key

1. Generate a keypair

```bash
$ cd ~/workspace/configserver/src/main/resources/
$ keytool -genkeypair -alias special -keyalg RSA \
    -dname "CN=Web Server,OU=Unit,O=Organization,L=City,S=State,C=US" \
    -keystore keystore.jks -storepass foobar
```

2. Restart configserver with clean

```bash
$ ./mvnw spring-boot:run -Dspring.cloud.config.server.git.uri=https://github.com/malston/config-repo
```

3. Encrypt config value with key from step 1.

```bash
$ curl -X POST --data-urlencode {key:special}mySpecialSecret localhost:8888/encrypt
```

4. Decrypt config value with with key from step 1

```bash
curl -X POST --data-urlencode \ 
  {key:special}AQB0OsLmIRHqiXbOEFMB7y/y4b3UQj7WiwackJGgMfoqHMtqNFoTDVBUAPPBoFCRowoCNd5fDNJNY0gAcQt/7ORGmP1B1rjoIMjBT9u8TPRIXK++LbroJ1UUTlmb+RIuY9wrb4g6ocwYK6O8j79y6UsZsIIUxZ9WZu45nfyAcEiPmtUiAKrTSQ46tE0RmAI/iLQH5GYKCmrfPntaf5sN9qWfXUmn3haEjEppSSJgs5OGgsEIFnReC9w89Gde8vMK4T3WhFG/27guXqtcTmmfgqvFvOY6IVxTMBMgvZ6MGmGwM5jU6NY/kNVKAUObEdIAUjlzytHwT4Hp6fgS123Wv2C5N7v3SVgYzVMQI5l6q21H9uL3v1pNbaCVebuxYGpsWg8= \
  localhost:8888/decrypt
```
