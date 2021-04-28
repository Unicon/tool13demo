LTI 1.3
========

IMS LTI 1.3 based starter (sample) application written using Java and Spring Boot

The goal is to have a Java based web app which can serve as the basis (or starting point) for building a fully compliant
LTI 1.3 tool.


Build
-----
This will produce a lti13demo.jar file in the *target* directory

    mvn install 

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

Customizing
-----------
Use the application.properties to control various aspects of the Spring Boot application (like setup your own database
connection). The example file has some section with a self-explanatory title. Of course it is recommended to 
use a properties file external to the jar to avoid to store sensitive values in your code: 

```--spring.config.location=/home/yourhomefolder/application-local.properties```


Creating the database
---------
Connect to your mysql server and use your values on xxDATABASENAMExxx, xxxuserNamexxx, xxxPasswordxxx Set the right
values in the properties file.

mysql> create database xxDATABASENAMExxx DEFAULT CHARACTER SET utf8 ; Query OK, 1 row affected (0.00 sec)

mysql> create user 'xxxuserNamexxx'@'%' identified by 'xxxPasswordxxx'; Query OK, 0 rows affected (0.00 sec)

mysql> grant all on xxDATABASENAMExxx.* to 'xxxuserNamexxx'@'%'; Query OK, 0 rows affected (0.00 sec)