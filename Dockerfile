FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/assilassime-gps-0.0.1-SNAPSHOT.jar /app
RUN chmod 755 /app
EXPOSE 9195
CMD [ "java", "-jar", "assilassime-gps-0.0.1-SNAPSHOT.jar" ]