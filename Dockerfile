# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS frontend-build
WORKDIR /workspace
COPY frontend/package.json ./
RUN npm install --no-audit --no-fund
COPY frontend/tsconfig.json frontend/tsconfig.app.json frontend/angular.json ./
COPY frontend/src ./src
RUN npx ng build --configuration production

FROM gradle:9-jdk21 AS backend-build
WORKDIR /workspace
COPY backend/settings.gradle.kts backend/build.gradle.kts backend/gradle.properties ./
COPY backend/src ./src
COPY --from=frontend-build /workspace/dist/burgee-dashboard/browser ./src/main/resources/static
RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 burgee
COPY --from=backend-build /workspace/build/libs/*.jar /app/burgee.jar
USER burgee
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/burgee.jar"]
