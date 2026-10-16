# Etapa 1: Build
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS build
WORKDIR /app

# Copiamos el código respetando el usuario de la imagen UBI (jboss: 185)
COPY --chown=185:0 pom.xml .
COPY --chown=185:0 src ./src

# Construir el jar
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

WORKDIR /app

#nos aseguramos de que el usuario sea el estándar de la imagen (jboss)
USER 185

# Copiar el jar generado desde la etapa de build
COPY --from=build --chown=185:0 /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]