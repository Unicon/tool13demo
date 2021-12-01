# Build Stage for Spring boot application image
FROM public.ecr.aws/o9c0t3w1/amazoncorretto:16 as build
RUN yum install -y unzip && yum clean all

RUN mkdir /build
WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
# download the dependency if needed or if the pom file is changed
RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw package
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Install new relic agent
ENV NEW_RELIC_VERSION 7.3.0
RUN curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/${NEW_RELIC_VERSION}/newrelic-java.zip && \
    unzip newrelic-java.zip


# Production Stage for Spring boot application image
FROM public.ecr.aws/o9c0t3w1/amazoncorretto:16 as production

RUN yum install -y shadow-utils && yum clean all
RUN mkdir /app /app/newrelic && useradd app-user && chown app-user /app
USER app-user

WORKDIR /app

# Copy the dependency application file from build stage artifact
COPY --from=build /build/target/lti13middleware-0.2-SNAPSHOT.jar /app/app.jar
COPY --from=build /build/newrelic/newrelic.jar /app/newrelic/
COPY --from=build /build/newrelic/newrelic.yml /app/newrelic

# Run the Spring boot application
CMD ["java", "-jar", "-javaagent:/app/newrelic/newrelic.jar", "/app/app.jar"]  