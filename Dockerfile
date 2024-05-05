FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM amazoncorretto:21.0.3-alpine3.19
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/hermes.jar
ENV API_KEY=
ENV BASE_DIRECTORY=
ENTRYPOINT ["java","-jar","/app/hestia.jar"]
LABEL org.opencontainers.image.source=https://github.com/LotuxPunk/Hermes
