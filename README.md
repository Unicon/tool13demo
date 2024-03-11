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
- Install Java 8
- Install Maven
- Install MySQL

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

Google Classroom Adapter
------------------------
------------------------
Developer/Admin Setup in Google Cloud
-------------------------------------
Source: https://developers.google.com/classroom/quickstart/java
1. Go to the Google Cloud Console: https://console.cloud.google.com/
2. Click on the dropdown to the right of "Google Cloud" to open the pop-up to select a project.
3. Click on NEW PROJECT in the upper right hand corner.
4. Enter a project name and ensure the other fields have valid values, then click CREATE.
5. Wait for the project to be created, then click SELECT PROJECT.
6. Your project's name should now appear to the right of "Google Cloud" at the top of the page.
7. Go here: https://console.cloud.google.com/flows/enableapi?apiid=classroom.googleapis.com
8. Ensure that you still see your project's name at the top of the page and then click NEXT.
9. Click ENABLE.
10. In the Google Cloud console, go to Menu menu > APIs & Services > Credentials.
11. Click CREATE CREDENTIALS > OAuth client ID.
12. Click CONFIGURE CONSENT SCREEN.
13. Select your User Type (I chose Internal) and click CREATE.
14. Enter a name for your application, a User Support email, and a Developer email address. I left the remaining fields with blank/default values. Click SAVE & CONTINUE.
15. Click ADD OR REMOVE SCOPES.
16. Enter the following list of scopes:
```
https://www.googleapis.com/auth/classroom.announcements
https://www.googleapis.com/auth/classroom.announcements.readonly
https://www.googleapis.com/auth/classroom.courses
https://www.googleapis.com/auth/classroom.courses.readonly
https://www.googleapis.com/auth/classroom.coursework.me
https://www.googleapis.com/auth/classroom.coursework.me.readonly
https://www.googleapis.com/auth/classroom.coursework.students
https://www.googleapis.com/auth/classroom.coursework.students.readonly
https://www.googleapis.com/auth/classroom.courseworkmaterials
https://www.googleapis.com/auth/classroom.courseworkmaterials.readonly
https://www.googleapis.com/auth/classroom.guardianlinks.me.readonly
https://www.googleapis.com/auth/classroom.guardianlinks.students
https://www.googleapis.com/auth/classroom.guardianlinks.students.readonly
https://www.googleapis.com/auth/classroom.profile.emails
https://www.googleapis.com/auth/classroom.profile.photos
https://www.googleapis.com/auth/classroom.push-notifications
https://www.googleapis.com/auth/classroom.rosters
https://www.googleapis.com/auth/classroom.rosters.readonly
https://www.googleapis.com/auth/classroom.student-submissions.me.readonly
https://www.googleapis.com/auth/classroom.student-submissions.students.readonly
https://www.googleapis.com/auth/classroom.topics
https://www.googleapis.com/auth/classroom.topics.readonly
```
17. Use the left/right arrow buttons to navigate through the scopes (I see 48 total). I also checked the boxes for service.management and service.management.readonly, userinfo.email, userinfo.profile, and openid. Click UPDATE.
18. Click SAVE AND CONTINUE.
19. Review the summary then click BACK TO DASHBOARD.
20. In the Google Cloud console, go to Menu menu > APIs & Services > Credentials.
21. Click CREATE CREDENTIALS > OAuth client ID.
22. Click Application type > Web application.
23. In the Name field, type a name for the credential. This name is only shown in the Google Cloud console.
24. For Authorized Redirect URIs, enter `http://localhost:8888` (Ensure in your properties file that you have `management.port=8888`)
25. Click Create. The OAuth client created screen appears, showing your new Client ID and Client secret.
26. Click OK. The newly created credential appears under OAuth 2.0 Client IDs.
27. Save the downloaded JSON file as google-cloud-credentials.json, and move the file to src/main/resources.

Add Hardcoded Self PlatformDeployment to DB
-------------------------------------------
In Postman, use the `/config` endpoint and the following request body to add the self platform deployment. Ensure to change out the domain with your own ngrok domain.
```json
{
  "iss": "https://7ae3-184-101-4-99.ngrok.io",
  "clientId": "self-client-id",
  "oidcEndpoint": "https://7ae3-184-101-4-99.ngrok.io/app/platform-oidc-authorize",
  "jwksEndpoint": "https://7ae3-184-101-4-99.ngrok.io/jwks/jwk",
  "oAuth2TokenUrl": "",
  "oAuth2TokenAud": "",
  "deploymentId": "self-deployment-id"
}
```

Overview
--------
Hopefully after completing the steps in the previous 2 sections, you should be ready to use the Google Classroom Adapter.
The Google Classroom adapter supports Classwork insertion (Deep Linking) and standard LTI launches from Google Classroom. It does not support grade passback or memberships (NRPS).
Replacing the example ngrok domain with your own ngrok domain, the flow should be as follows:
1. In your browser, go to https://7ae3-184-101-4-99.ngrok.io/app
2. If this is your first time using the app, then there will be a Google Auth link in the logs. Copy/paste that link into a separate tab in your browser.
3. Complete the Google Auth process and close the tab for it.
4. Return to your App tab.
5. Select your Google Classroom class from the dropdown (if you don't have one, create one and then refresh this page).
6. Click the "Add Content to LMS" button.
7. Complete the demo LTI flow to get to the Deep Linking menu.
8. Select which link(s) you would like to insert into your Google Classroom class.
9. A success page should appear.
10. In another tab, open your Google Classroom class and click on the Classwork tab.
11. Your link(s) should be there.
12. Click on one of the links.
13. Complete the demo LTI flow to receive a "Google Classroom" LTI id_token.