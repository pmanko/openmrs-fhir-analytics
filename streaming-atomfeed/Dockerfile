FROM alpine/git as clone
WORKDIR /app

# TODO: Look over fixes and create PRs as needed to remove this requirement
RUN ["git", "clone", "-b", "db-url-port-fix", "https://github.com/pmanko/atomfeed.git"]
RUN ["git", "clone", "-b", "isanteplus-fixes", "https://github.com/pmanko/openmrs-module-atomfeed.git"]

FROM maven:3.6.3-openjdk-8 as build
WORKDIR /app
COPY --from=clone /app/atomfeed /app/atomfeed
COPY --from=clone /app/openmrs-module-atomfeed /app/openmrs-module-atomfeed
WORKDIR /app/atomfeed
CMD ["mvn", "clean", "install", "-DskipTests"]
WORKDIR /app/openmrs-module-atomfeed
CMD ["mvn", "clean", "install", "-DskipTests"]
WORKDIR /app/openmrs-fhir-analytics
CMD ["mvn", "clean", "install", "-DskipTests"]

ENV GCP_FHIR_STORE="projects/PROJECT/locations/LOCATION/datasets/DATASET/"
ENV SERVER_URL="http://localhost:8099/openmrs"
ENV SOURCE_USERNAME="admin"
ENV SOURCE_PW="admin"

ENTRYPOINT mvn exec:java -pl streaming -Dexec.mainClass=org.openmrs.analytics.FhirStreaming ${SERVER_URL} ${SOURCE_USERNAME}/${SOURCE_PW} ${GCP_FHIR_STORE}