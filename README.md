# hello-tomcat

A simple app that demonstrates how to run a Java Web Application as a JAR using embedded Tomcat
and JNDI to register a data source from properties read from the environment.

## Run local

* Start ConfigServer

    Clone the sample [config-server](https://github.com/spring-cloud-samples/configserver) and point it to a git repo that contains your config.

    ```
    $ ./mvnw spring-boot:run -Dspring.cloud.config.server.git.uri=https://github.com/malston/hello-tomcat-config
    ```

* Run the app
    ```
    $ ./gradlew clean build
    $ SPRING_PROFILES_ACTIVE=db,development java -jar build/libs/hello-tomcat-0.0.1-SNAPSHOT.jar "http://localhost:8888"
    ```
* Hit the URL

    The app exposes a `/hello` endpoint that will print the value of `foo` value in the config. If you give it a query string parameter then you can print your name like this:  http://localhost:8080/hello?name=Jerry

* If you want to fetch encrypted values then you have to set `USE_ENCRYPT` to `true`.

	```
	$ SPRING_PROFILES_ACTIVE=db,development USE_ENCRYPT=true java -jar build/libs/hello-tomcat-0.0.1-SNAPSHOT.jar "http://localhost:8888"
	```

## Run on PCF

* Push config-server and change the `JBP_CONFIG_JAVA_MAIN` variable inside the `manifest.yml` to point to the config-server url.

	```
	$ ./gradlew clean build
	$ cf cs p-mysql 100mb hello-db
	$ cf push
	```

## Encrypt/Decrypt Config Values

### Create keystore

See docs for [Creating a Key Store for Testing](http://cloud.spring.io/spring-cloud-static/spring-cloud-config/1.2.3.RELEASE/#_creating_a_key_store_for_testing)

### Encrypt/decrypt values using keystore

1. Encrypt `secret`

	```
	$ curl localhost:8888/encrypt -d secret
	<YOUR ENCRYPTED VALUE>
	```

2. Decrypt `secret`

	```
	$ curl localhost:8888/decrypt -d <YOUR ENCRYPTED VALUE>
	secret
	```

### Encrypt/decrypt with custom key

1. Generate a keypair

	```
	$ cd $WORKSPACE/configserver/src/main/resources/
	$ keytool -genkeypair -alias special -keyalg RSA \
	    -dname "CN=Web Server,OU=Unit,O=Organization,L=City,S=State,C=US" \
	    -keystore keystore.jks -storepass foobar
	```

2. Restart configserver with clean

	```
	$ ./mvnw spring-boot:run -Dspring.cloud.config.server.git.uri=https://github.com/malston/hello-tomcat-config
	```

3. Encrypt config value with key from step 1.

	```
	$ curl -X POST --data-urlencode {key:special}mySpecialSecret localhost:8888/encrypt
	```

4. Decrypt config value with with key from step 1

	```
	$ curl -X POST --data-urlencode \
		{key:special}AQB0OsLmIRHqiXbOEFMB7y/y4b3UQj7WiwackJGgMfoqHMtqNFoTDVBUAPPBoFCRowoCNd5fDNJNY0gAcQt/7ORGmP1B1rjoIMjBT9u8TPRIXK++LbroJ1UUTlmb+RIuY9wrb4g6ocwYK6O8j79y6UsZsIIUxZ9WZu45nfyAcEiPmtUiAKrTSQ46tE0RmAI/iLQH5GYKCmrfPntaf5sN9qWfXUmn3haEjEppSSJgs5OGgsEIFnReC9w89Gde8vMK4T3WhFG/27guXqtcTmmfgqvFvOY6IVxTMBMgvZ6MGmGwM5jU6NY/kNVKAUObEdIAUjlzytHwT4Hp6fgS123Wv2C5N7v3SVgYzVMQI5l6q21H9uL3v1pNbaCVebuxYGpsWg8= \
		localhost:8888/decrypt
    mySpecialSecret
	```
