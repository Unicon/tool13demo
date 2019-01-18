LTI 1.3
========

IMS LTI 1.3 based starter (sample) application written using Java and Spring Boot

The goal is to have a Java based web app which can serve as the basis (or starting point) for building a fully compliant LTI 1.3 tool.


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

You can access the H2 console for default in-memory DB (JDBC URL: **jdbc:h2:mem:AZ**, username: **sa**, password: *(blank)*) at:

    https://localhost:9090/console

Customizing
-----------
Use the application.properties to control various aspects of the Spring Boot application (like setup your own database connection).
Use the logback.xml to adjust and control logging.

Debugging
---------
To enable the debugging port (localhost:5050) when using spring-boot:run, use the maven profile: **-Pdebug**. Then you can attach any remote debugger (eclipse, intellij, etc.) to localhost:8000. NOTE that the application will pause until you connect the debugger to it.

    mvn clean install spring-boot:run -Pdebug
