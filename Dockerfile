# Etapa 1: compilación con Maven y JDK 21
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen liviana con el JRE, ejecutada sin privilegios
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 aplicacion
COPY --from=compilacion /app/target/showcase-api-springboot.jar app.jar
USER aplicacion
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
