# FROM eclipse-temurin:17-jdk-alpine

# WORKDIR /app

# COPY target/gpr-0.0.1-SNAPSHOT.jar /app

# RUN chmod 755 /app

# EXPOSE 9000

# CMD [ "java", "-jar", "gpr-0.0.1-SNAPSHOT.jar" ]


FROM eclipse-temurin:17-jdk-alpine

# Créer un utilisateur non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Définir le répertoire de travail et détenir l'utilisateur non-root
WORKDIR /app
RUN chown appuser:appgroup /app
USER appuser

# Copier le fichier JAR dans le conteneur
COPY target/gpr-0.0.1-SNAPSHOT.jar /app

# Exposer le port
EXPOSE 9000

# Commande pour exécuter l'application
CMD ["java", "-jar", "gpr-0.0.1-SNAPSHOT.jar"]
