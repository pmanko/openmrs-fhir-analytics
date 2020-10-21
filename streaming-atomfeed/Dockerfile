FROM alpine/git as clone
WORKDIR /app

# TODO: Look over fixes and create PRs as needed to remove this requirement
RUN ["git", "clone", "-b", "db-url-port-fix", "https://github.com/pmanko/atomfeed.git"]
RUN ["git", "clone", "-b", "isanteplus-fixes", "https://github.com/pmanko/openmrs-module-atomfeed.git"]

FROM maven:3.6.3-openjdk-8 as build
COPY --from=clone /app/atomfeed /app/atomfeed
COPY --from=clone /app/openmrs-module-atomfeed /app/openmrs-module-atomfeed

WORKDIR /app/atomfeed
RUN mvn -B install -DskipTests

WORKDIR /app/openmrs-module-atomfeed

RUN mvn -B install -DskipTests

WORKDIR /app/openmrs-fhir-analytics
COPY . /app/openmrs-fhir-analytics
RUN mvn -B install -DskipTests

ENTRYPOINT mvn exec:java -pl streaming-atomfeed -Dexec.mainClass=org.openmrs.analytics.FhirStreaming -Dexec.args="${SOURCE_URL} ${SOURCE_USERNAME}/${SOURCE_PW} ${SINK_URL} ${SINK_USERNAME}/${SINK_PASSWORD}"