LTI 1.3
========

IMS LTI 1.3 based starter (sample) application written using Java and Spring Boot

The goal is to have a Java based web app which can serve as the basis (or starting point) for building a fully compliant
LTI 1.3 tool.

### Endpoints
#### Accessible without an LMS:
- Manage Deployment Configurations/Registrations: https://localhost:443/config/
- JWKS: https://localhost:443/jwks/jwk
- LTI Advantage Client Assertion: https://localhost:443/config/lti_advantage/client_assertion
#### Only Accessible within an LMS:
- Redirect & Target URI: https://localhost:443/lti3
- OIDC Initiation: https://localhost:443/oidc/login_initiations
- Dynamic Registration: https://localhost:443/registration

Everyday Use
------------
------------

Build
-----
This will produce a lti13demo.jar file in the *target* directory

    mvn clean install 

note, to avoid some test errors if those happen, try

    mvn install -DskipTests=true

Quick Run
---------
You can run the app in place to try it out without having to install and deploy a servlet container.

    mvn clean install -DskipTests=true spring-boot:run

NOTE: To run it and connect it to real LMSs it is recommended to run it in an accessible server 
with a valid certificate.


Instructions to Start from Scratch
----------------------------------
----------------------------------
Prerequisites
-------------
- Install Java 17 (the `java.version` in `pom.xml`).
- Install Maven
- Install MySQL (optional for local development, see the H2 note under "Creating the database")

Creating the database
---------------------
Connect to your mysql server and use your values on YOURDATABASE, YOURDATABASEUSERNAME, YOURDATABASEUSERPASSWORD Set the right
values in the properties file. This code was last tested with MySQL version 8.0.28.

`mysql> create database YOURDATABASE DEFAULT CHARACTER SET utf8 ; Query OK, 1 row affected (0.00 sec)`

`mysql> create user 'YOURDATABASEUSERNAME'@'%' identified by 'YOURDATABASEUSERPASSWORD'; Query OK, 0 rows affected (0.00 sec)`

`mysql> grant all on YOURDATABASE.* to 'YOURDATABASEUSERNAME'@'%'; Query OK, 0 rows affected (0.00 sec)`

### Using in-memory H2 instead of MySQL
For local development you can skip MySQL and use an in-memory H2 database by setting these properties:

```
spring.datasource.url=jdbc:h2:mem:lti13demo;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.liquibase.enabled=false
```

Keep in mind that the database is wiped every time the application restarts. The platform registrations seeded from
`src/main/resources/data/platform_deployment/*.json` are reloaded on startup, but anything you registered through the
`/config/` endpoint has to be registered again (or added as a JSON file in that directory so it persists).

Customizing
-----------
Use the application.properties to control various aspects of the Spring Boot application (like setup your own database
connection). The example file has some section with a self-explanatory title. Of course it is recommended to 
use a properties file external to the jar to avoid to store sensitive values in your code: 

```--spring.config.location=/home/yourhomefolder/application-local.properties```

Adding PKCS8 RSA Keys to the application.properties file
--------------------------------------------------------
1. cd to the directory that you want the keys to be located
2. `openssl genrsa -out keypair.pem 2048`
3. `openssl rsa -in keypair.pem -pubout -out publickey.crt`
4. `cat publickey.crt`
5. Copy this value to `oidc.publickey` inside the application.properties file. Ensure that each newline is indicated by \n (may need to do a find `\n` and replace with `\\n`)
6. `openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key`
7. `cat pkcs8.key`
8. Copy this value to `oidc.privatekey` inside the application.properties file. Ensure that each newline is indicated by \n (may need to do a find `\n` and replace with `\\n`)

Setting Up SSL Cert for Local Development
-----------------------------------------
Note: As of 8/11/2021, for Mac, this works with Firefox but not Chrome.
1. `cd ~/PATH/tool13demo/src/main/resources`
2. Generate ssl certificate in the resources directory: `keytool -genkeypair -alias keystoreAlias -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650`
3. Enter any values desired when prompted, but make sure you remember the password that you used.
4. In the `application.properties` file, ensure that each of these variables have the correct values filled in:
```
server.port=443
security.require-ssl=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=<password from step 2>
server.ssl.keyStoreType=PKCS12
server.ssl.keyAlias=keystoreAlias
security.headers.frame=false
application.url=https://localhost:443
```
5. After running the spring boot application, you should be able to reach it at https://localhost:443


Exposing the Local Tool to an LMS with Tailscale Funnel
-------------------------------------------------------
A hosted LMS such as Canvas needs to reach your tool from the public internet with a certificate it trusts, both for the
browser redirects and for server-to-server calls (for example fetching your JWKS to validate LTI Advantage token requests).
Tailscale Funnel provides that: Tailscale terminates TLS on port 443 with a valid certificate for your machine's
`*.ts.net` hostname and proxies the traffic to the tool running locally.

1. Install Tailscale, log in, and have your tailnet admin enable Funnel (see the Tailscale docs).
2. Run the tool on a port other than 443, for example `--server.port=9090`. Tailscale needs port 443 on your machine's
   Tailscale IP. If the tool binds `*:443` it answers that traffic itself with its self-signed certificate, and the LMS
   fails with errors like Canvas's `JWK Error: Failed to fetch public key`.
3. Start the Funnel in the background. Use `https+insecure` because the tool is serving its own self-signed certificate
   on the local port:

       tailscale funnel --bg https+insecure://localhost:9090

4. Set `application.url` and `domain.url` in your properties file to your Tailscale hostname, for example
   `https://yourmachine.your-tailnet.ts.net`, and use that hostname in the tool registration on the platform. The hostname
   is stable across restarts, so the platform registration does not need to change.
5. Check `https://yourmachine.your-tailnet.ts.net/jwks/jwk` loads in a browser without a certificate warning.
6. When you are done, turn the Funnel off. The hostname is public while it is on and automated scanners find it within minutes:

       tailscale funnel --https=443 off

`tailscale funnel status` shows the current configuration. Any other tunnel (ngrok and similar) works the same way: point it
at the local port and use the public https URL in the platform registration.

Registering a Platform Deployment
---------------------------------
Each LMS deployment the tool accepts launches from needs a platform deployment record (issuer, client id, deployment id
and the platform's OIDC, JWKS and token endpoints). You can seed them from JSON files in
`src/main/resources/data/platform_deployment/` or create them at runtime with the admin credentials
(`terracotta.admin.user` / `terracotta.admin.password`):

    curl -u admin:PASSWORD -X POST https://localhost:9090/config/ -H "Content-Type: application/json" -d '{
      "iss": "https://canvas.instructure.com",
      "clientId": "YOUR_DEVELOPER_KEY_ID",
      "deploymentId": "YOUR_DEPLOYMENT_ID",
      "oidcEndpoint": "https://sso.canvaslms.com/api/lti/authorize_redirect",
      "jwksEndpoint": "https://sso.canvaslms.com/api/lti/security/jwks",
      "oAuth2TokenUrl": "https://sso.canvaslms.com/login/oauth2/token"
    }'

A launch that fails with "Not found any existing tool deployment with iss ... clientId ... deploymentId ..." means no
record matches the values the platform sent. Create one with exactly those values.

Canvas note: use Canvas's domain-agnostic `authorize_redirect` endpoint above rather than an institution-specific
`https://INSTITUTION.instructure.com/api/lti/authorize`, which is being deprecated.
