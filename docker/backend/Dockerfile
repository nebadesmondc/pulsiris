# Build stage: compiles the app.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src/main src/main

RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon \
	&& cp build/libs/*.jar application.jar \
	&& java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Runtime stage: only a JRE and the app.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S pulsiris && adduser -S -G pulsiris pulsiris

# Least to most frequently changed, code change rebuilds only the last layer.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

USER pulsiris
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60"

ENTRYPOINT ["java", "-jar", "application.jar"]
