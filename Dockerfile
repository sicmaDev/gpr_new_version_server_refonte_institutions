FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
<<<<<<< HEAD

COPY target/gpr-0.0.1-SNAPSHOT.jar /app

RUN chmod 755 /app

EXPOSE 9003

CMD [ "java", "-jar", "gpr-0.0.1-SNAPSHOT.jar" ]
=======
COPY target/assilassime-gps-0.0.1-SNAPSHOT.jar /app
RUN chmod 755 /app
EXPOSE 9195
CMD [ "java", "-jar", "assilassime-gps-0.0.1-SNAPSHOT.jar" ]
>>>>>>> 5f3aa8d737be158adde0de100d355d6f20a0ecc3
