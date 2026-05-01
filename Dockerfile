FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --prefer-offline
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
RUN mvn -q -DskipTests -Dskip.frontend.build=true package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

# Cloud Run sets PORT at runtime. Keep 8080 for local defaults.
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=gcp
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "echo \"[startup] host=0.0.0.0 port=${PORT:-8080} profile=${SPRING_PROFILES_ACTIVE:-default}\"; exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
