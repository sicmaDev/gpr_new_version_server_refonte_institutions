FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/gpr-0.0.1-SNAPSHOT.jar /app

RUN chmod 755 /app

EXPOSE 9003

CMD [ "java", "-jar", "gpr-0.0.1-SNAPSHOT.jar" ]