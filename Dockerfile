# Corretto 21.0.13.1.2 (pushed under the 21.0.13 tags on 2026-11-17) is the first build that fixes
# CVE-2026-114938 "Java4Shell" in the Base64 decoder. Do not go back to an older 21 image.
FROM amazoncorretto:21.0.13-alpine3.24 AS build
COPY . /src
WORKDIR /src
RUN ./gradlew buildFatJar --no-daemon

FROM amazoncorretto:21.0.13-alpine3.24
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=build /src/build/libs/*.jar /app/hestia.jar
ENV API_KEY=
ENV BASE_DIRECTORY=
ENV PUBLIC_DIRECTORY=
ENV JWT_AUDIENCE=
ENV JWT_ISSUER=
ENV JWT_REALM=
ENV JWT_SECRET=
ENV MAX_UPLOAD_SIZE_BYTES=536870912
ENTRYPOINT ["java","-jar","/app/hestia.jar"]
LABEL org.opencontainers.image.source=https://github.com/LotuxPunk/Hestia
