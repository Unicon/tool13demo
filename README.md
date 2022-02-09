LTI 1.3
========

IMS LTI 1.3 based starter (sample) application written using Java and Spring Boot

The goal is to have a Java based web app which can serve as the basis (or starting point) for building a fully compliant
LTI 1.3 tool.

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

Then go to the following default URL:

    https://localhost:9090/

NOTE: To run it and connect it to real LMSs it is recommended to run it in an accessible server 
with a valid certificate.

Endpoints
---------
Redirect & Target URI: https://localhost:443/lti3/   
OIDC Initiation: https://localhost:443/oidc/login_initiations
Manage Deployment Configurations/Registrations: https://localhost:443/config/

Instructions to Start from Scratch
----------------------------------
----------------------------------

Creating the database
---------------------
Connect to your mysql server and use your values on YOURDATABASE, YOURDATABASEUSERNAME, YOURDATABASEUSERPASSWORD Set the right
values in the properties file. This code was last tested with MySQL version 8.0.28.

`mysql> create database YOURDATABASE DEFAULT CHARACTER SET utf8 ; Query OK, 1 row affected (0.00 sec)`

`mysql> create user 'YOURDATABASEUSERNAME'@'%' identified by 'YOURDATABASEUSERPASSWORD'; Query OK, 0 rows affected (0.00 sec)`

`mysql> grant all on YOURDATABASE.* to 'YOURDATABASEUSERNAME'@'%'; Query OK, 0 rows affected (0.00 sec)`

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


Using Ngrok For Local SSL Cert
-------------------------------------
1. Download and install ngrok
2. `./ngrok http 9090` (Note: This port number must match the value of `server.port` in the application.properties file.)
3. Utilize the https url from ngrok when registering your tool with the platform   

Note: Each time you restart ngrok, you will need to change the url of your tool in your registration with the LMS. However, you may restart the tool as much as you like while leaving ngrok running without issue.
