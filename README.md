LTI 1.3
========

IMS LTI 1.3 based starter (sample) application written using Java and Spring Boot

The goal is to have a Java based web app which can serve as the basis (or starting point) for building a fully compliant
LTI 1.3 tool.

###Endpoints

- Target: https://localhost:443/lti3   
- OIDC Initiation: https://localhost:443/oidc/login_initiations   
- Config: https://localhost:443/config/   
- JWKS: https://localhost:443/jwks/jwk   
- Dynamic Registration: https://localhost:443/registration

# Prerequisites

If you do not have Java v16 installed we recommend installing the [adoptium](https://adoptium.net/installation.html) version through homebrew

    brew tap homebrew/cask-versions
    brew install --cask temurin16

You will also need Maven

    brew install maven

---
# Setting up the Application

To run it and connect it to real LMSs it is recommended to run it in an accessible server
with a valid certificate. Follow all the setup steps below to set it up. 

Things you can do without the certificate:
- Use the config endpoint - https://localhost:443/config/
- See `Accessing endpoints` in the `Extra Features and Information` section below on how to configure authorization to access the endpoints


## 1: Creating the Postgres database

1. Install PostgresQL (version 13.4 as of today): `brew install postgres`
2. Start the database: `brew services start postgres`
3. Create a Postgres user and database for this project by entering the following in the Terminal:
```bash
psql postgres
CREATE ROLE lti13user WITH LOGIN PASSWORD 'yourpassword';
ALTER ROLE lti13user CREATEDB;
\q 
psql postgres -U lti13user
CREATE DATABASE lti13middleware;
GRANT ALL PRIVILEGES ON DATABASE lti13middleware TO lti13user;
```

## 2: Create an application-local.properties

It is recommended to use a properties file external to the jar to avoid to store sensitive values in your code. 

1. Copy the `application.properties` inside `src/main/resources` and name the copy `application-local.properties`
2. This is the `.properties` file where we will edit values for local development as described below
3. Ensure that the values in `application-local.properties` match the database name, user, and password that you used when creating the postgres database in the previous section.

Alternatively, you can create an `application.properties` file in a `config` folder in the root of the project, and override any property for local development.

## 3: Adding PKCS8 RSA Keys to the .properties file

1. cd to the directory that you want the keys to be located
2. `openssl genrsa -out keypair.pem 2048`
3. `openssl rsa -in keypair.pem -pubout -out publickey.crt`
4. `cat publickey.crt`
5. Copy this value to `oidc.publickey` inside the `.properties` file. The key needs to all be on one line, with a newline character, `\n`, preceding and following the string that you copy in between `---BEGIN PUBLIC KEY---` and `---END PUBLIC KEY---`.
6. `openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key`
7. `cat pkcs8.key`
8. Copy this value to `oidc.privatekey` inside the `.properties` file. The key needs to all be on one line, with a newline character, `\n`, preceding and following the string that you copy in between `---BEGIN PRIVATE KEY---` and `---END PRIVATE KEY---`.

## 4: Setting Up SSL Cert for Local Development

Note: As of 8/11/2021, for Mac, this works with Firefox but not Chrome.
1. from the middleware directory, cd into `src/main/resources`
2. Generate ssl certificate in the resources directory: `keytool -genkeypair -alias keystoreAlias -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650`
3. Enter any values desired when prompted, but make sure you remember the password that you used.
4. In the `.properties` file, ensure that each of these variables have the correct values filled in:
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

## 5: Using Ngrok For Local SSL Cert

1. Download and install ngrok
2. You will need to set up an account to use localhost. See the [setup instructions](https://dashboard.ngrok.com/get-started/setup)
3. `./ngrok http 443` (Note: This port number must match the value of `server.port` in the `.properties` file.)
4. Ensure in your `.properties` file that the `application.url` is set to the `https` ngrok url. eg: 
   1. `application.url=https://a50a-97-120-101-43.ngrok.io`
5. Utilize the https url from ngrok when registering your tool with the LMS platform   

Note: Each time you restart ngrok, you will need to change the url of your tool in your registration with the LMS. However, you may restart the tool as much as you like while leaving ngrok running without issue.

# Build and Run

## Option 1

Start by building the `.jar` file

    mvn clean install

The following command below will run the app with the `application-local.properties` file assuming you placed it in the project's resources directory.
Find the name of the .jar file in the *target* directory and replace `"name-of-.jar"` with your `.jar`

    java -jar target/"name-of-.jar" --spring.config.name=application-local

## Option 2

You can run the app in place to try it out without having to install and deploy a servlet container.


    mvn clean install spring-boot:run -Dspring-boot.run.arguments=--spring.config.name=application-local


# Testing the application

## Skipping Unit Tests

You can add this `-DskipTests=true` to either of the build and run command to skip running the tests
- This can be useful if you know the tests are passing and want to save time in development as running the tests can add on extra build time.

### Option 1

    mvn clean install -DskipTests=true

### Option 2

    mvn clean install -DskipTests=true spring-boot:run -Dspring-boot.run.arguments=--spring.config.name=application-local


----
# Extra Features and Information

## Accessing endpoints
- To set up access to endpoints you will need authorization stored in the `.properties` file
    - `terracotta.admin.user`
    - `terracotta.admin.password`

### Config Endpoints
- Likely the main endpoints you will want to test or use directly when developing locally is the `/config/` endpoint
- This endpoint is how we access and save LTI 1.3 tool deployment records which need to be configured when setting up a tool in an LMS
#### Postman Exports
- There are Postman exports stored in `src/test/postman` that can be imported in to Postman to use the config endpoints.
- Once imported, in Postman:
  - Add the value you have set for the `terracotta.admin.password` to the `auth_password` variable stored in the `lti-middleware` Globals in Environments Section of Postman 


## Turning Demo/Test Features On/Off

In the `.properties` file, the following env variables have been added.
```
lti13.demoMode=false
lti13.enableAGS=true
lti13.enableMembership=false
lti13.enableDynamicRegistration=false
lti13.enableDeepLinking=false
lti13.enableRoleTesting=false
lti13.enableTokenController=false
```
 - `lti13.demoMode` must have a value of `true` for AGS, Membership, or Deep Linking (the LTI Advantage Services) to function regardless of the value of their individual env variables. This is because each of these services is currently a demo.
 - `lti13.enableAGS` must have a value of `true` in addition to `lti13.demoMode` having a value of `true` for the Assignments and Grades (AGS) demo service to function.
 - `lti13.enableMembership` must have a value of `true` in addition to `lti13.demoMode` having a value of `true` for the Membership demo service to function.
 - `lti13.enableDeepLinking` must have a value of `true` in addition to `lti13.demoMode` having a value of `true` for the Deep Linking demo service to function.
 - The remaining new env vars `lti13.enableDynamicRegistration`, `lti13.enableRoleTesting`, `lti13.enableTokenController` are for test endpoints. `lti13.enableDynamicRegistration` corresponds to the `RegistrationController` which has not been tested. `lti13.enableRoleTesting` corresponds to the `TestController` which I suspect will be deleted as role authorization is not needed. `lti13.enableTokenController` corresponds to `TokenController` which I suspect will be deleted if it remains unused.

## Setup SQS for AGS Grade Pass Back

1. The authentication to AWS uses Spring's [default authentication chain](https://cloud.spring.io/spring-cloud-static/spring-cloud-aws/1.2.3.RELEASE/multi/multi__basic_setup.html#_sdk_credentials_configuration).
2. The URI for a SQS queue should be filled in for either `lti13.grade-passback-queue` or as `LTI13_GRADE_PASSBACK_QUEUE` environment variable.
3. The region of the SQS queue should be filled in for either `cloud.aws.region.static` in the `.properties` file or `AWS_REGION` as an environment variable.

## Turning off AWS Integration for Local Development

Prior to running `mvn clean install spring-boot:run`, do the following:
1. Ensure that the following property is present and uncommented in the `.properties` file:
   `spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextRegionProviderAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextResourceLoaderAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration`
2. Set the Spring Boot profile to `no-aws` in the run command by doing one of the following:
    ### Option 1
        java -jar target/"name-of-.jar" --spring.config.name=application-local --spring.profiles.active=no-aws
    ### Option 2
        mvn clean install -Dspring-boot.run.profiles=no-aws spring-boot:run -Dspring-boot.run.arguments=--spring.config.name=application-local


## Turning on AWS Integration for Local Development
### Prerequisite: Set up envchain
1. `brew install envchain`
2. `envchain --set NAMEYOURAWSENV SPACE_DELIMITED_VARIABLES` For example:  
    ```envchain --set aws-sqs-gpb AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY```
3. Enter the values of these variables that correspond to the AWS account for your SQS queue for grade passback.
4. Confirm the variables are set by running:
    ```envchain aws-sqs-gpb env | grep AWS_```

### Turing on AWS Integration
1. Ensure that the following properties are set in the `.properties` file:
```
cloud.aws.region.static=us-west-1
cloud.aws.region.auto=false
cloud.aws.stack.auto=false
lti13.grade-passback-queue=<queue name>
```
2. Ensure that the `spring.autoconfigure.exclude` property is commented out in the `.properties` file.
3. Run the application, ensuring that the no-aws profile is not being set. 

## Sample SQS Message for AGS/Grade Passback

```json
{
"Type" : "Notification",
"MessageId" : "f85561b6-dd51-5de6-8d75-3b128d9598d5",
"TopicArn" : "arn:aws:sns:us-west-2:725843923591:develop-lti13_grade",
"Message" : "{\"user_id\":\"4f3d12df-e1ae-484f-8b9a-b667864e8100\",\"lineitem_url\":\"https://canvas.unicon.net/api/lti/courses/3348/line_items/496\",\"client_id\":\"97140000000000230\",\"deployment_id\":\"523:0a47de91cf84ee147f6e534195988508504d3e82\",\"issuer\":\"https://canvas.instructure.com\",\"score\":0.42857142857142855}",
"Timestamp" : "2022-03-10T19:16:20.157Z",
"SignatureVersion" : "1",
"Signature" : "j48wQktGN/UUjwsUK+w7tpn68ANS7/nM8Kwbb848sEZ1Hl7+N1/D8CoDutFHzaT2GbQ+Vh6NFCXtEMyX2LF/UwqqTRqLYknIm7NVCob9/2hADuE6Ix5mPcJKD+Y7nwl6GGu/6I9o+JzVzO2DqOVz7rL3QGnZrJi35RU/SGAxE1NcKl24y6bR+kGPK6O+O3WlyFIPkAP74qIgqdVGtq6v7OJtsDirxBg0JrX4eRNTVMWT+1CK/n78yG8EWs9SARHaLtN+donriYuw59G7ofkwKVmjYniJixpz1hIsyTzP/TZpi+Fn8ZobYMB5WsuUkjnqZTsV45Q/Wm14Ul8rMvUYEg==",
"SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-7ff5318490ec183fbaddaa2a969abfda.pem",
"UnsubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:725843923591:develop-lti13_grade:e4402d2e-ab0c-4fdf-b3a7-dc0d745466e5"
}
```

## Setup Harmony Courses Endpoint

When there's a deep linking request the LTI tool displays a Harmony course selector to the instructor, the list of courses comes from a Harmony endpoint.
In order to fetch some courses you'll need credentials to access the endpoint, please complete these properties in the `.properties`
```
harmony.courses.api=https://harmony.endpoint.url
harmony.courses.jwt=a.cool.jwt
```

## Dynamic Registration

If using dynamic registration, ensure that the `domain.url` in the `.properties` file is set to the appropriate version of Waymaker. ie:

`domain.url=https://local.waymaker.xyz`

## Deploying to the Dev/Staging Environment

1. Push changes to the branch that you want to push to the dev environment (e.g. L3-66-integration-testing)
2. `git branch -d dev-env`
3. `git checkout -b dev-env`
4. `git pull origin L3-66-integration-testing`
5. `git push --force --set-upstream origin dev-env`
6. Wait 10-15 minutes for the deployment to complete.
7. View the logs using `aws logs tail /aws/elasticbeanstalk/lti-service-dev-docker/var/log/eb-docker/containers/eb-current-app/stdouterr.log --follow --since 1h`

## Getting Started with the Deep Linking UI (React)

The Deep Linking UI was written in React to support deep linking messages in the LTI middleware, it allows the instructor to pick courses from Waymaker.

It can be displayed in the LMS launching the Middleware from a deep link request or it can be run independently using NPM. 

You can install NPM chosing your OS favorite distribution from [here](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm).

### Available Scripts

In the 'src/main/frontend' directory, you can run:

#### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.\
You may also see any lint errors in the console.

#### `npm test`

Launches the test runner in the interactive watch mode.\
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### Deployment

The project contains the `frontend-maven-plugin` plugin that downloads/installs Node and NPM locally, runs npm install, and then any combination of Bower, Grunt, Gulp, Jspm, Karma, or Webpack. It's supposed to work on Windows, OS X and Linux.

After building and packing the entire React UI, it deploys it together with the Thymeleaf existing static files.
