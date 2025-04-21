FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /work
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /work/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
