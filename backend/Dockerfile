FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY . .
RUN mvn -q -DskipTests package \
  && cp "$(find target -maxdepth 1 -name '*.jar' ! -name 'original-*' | head -n 1)" /tmp/app.jar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
