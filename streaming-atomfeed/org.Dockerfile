
FROM maven:3.6.3-openjdk-8 as build

ARG USERNAME
ARG TOKEN

WORKDIR /
RUN mkdir -p /root/.m2 \
    && mkdir /root/.m2/repository
COPY ./streaming-atomfeed/settings.xml.template .
RUN sed -e "s/\${your-github-username}/$USERNAME/" -e "s/\${your-github-token}/$TOKEN/" settings.xml.template | tee /root/.m2/settings.xml

WORKDIR /app/openmrs-fhir-analytics
COPY . /app/openmrs-fhir-analytics
RUN mvn -B install -DskipTests

ENTRYPOINT mvn exec:java -pl streaming-atomfeed -Dexec.mainClass=org.openmrs.analytics.FhirStreaming -Dexec.args="${SOURCE_URL} ${SOURCE_USERNAME}/${SOURCE_PW} ${SINK_URL} ${SINK_USERNAME}/${SINK_PASSWORD}"