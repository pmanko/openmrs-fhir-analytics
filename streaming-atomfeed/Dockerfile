FROM alpine/git as clone
WORKDIR /app

# TODO: Look over fixes and create PRs as needed to remove this requirement
RUN ["git", "clone", "-b", "db-url-port-fix", "https://github.com/pmanko/atomfeed.git"]
RUN ["git", "clone", "-b", "isanteplus-fixes", "https://github.com/pmanko/openmrs-module-atomfeed.git"]

FROM maven:3.6.3-openjdk-8 as build
COPY --from=clone /app/atomfeed /app/atomfeed
COPY --from=clone /app/openmrs-module-atomfeed /app/openmrs-module-atomfeed

WORKDIR /app/atomfeed
#RUN mvn -B -f pom.xml dependency:go-offline
RUN mvn -B install -DskipTests
#RUN mvn clean install -DskipTests

WORKDIR /app/openmrs-module-atomfeed
#RUN mvn -B -f pom.xml dependency:go-offline
RUN mvn -B install -DskipTests

#COPY ./pom.xml /app/openmrs-fhir-analytics/pom.xml
#COPY ./common/pom.xml /app/openmrs-fhir-analytics/common/pom.xml
#COPY ./streaming-atomfeed/pom.xml /app/openmrs-fhir-analytics/streaming-atomfeed/pom.xml
#COPY ./streaming-binlog/pom.xml /app/openmrs-fhir-analytics/streaming-binlog/pom.xml
#COPY ./batch/pom.xml /app/openmrs-fhir-analytics/batch/pom.xml

WORKDIR /app/openmrs-fhir-analytics
COPY . /app/openmrs-fhir-analytics
RUN mvn -B install -DskipTests

# TODO: Demo GCP project authentication
#ARG CLOUD_SDK_VERSION=232.0.0
#ENV CLOUD_SDK_VERSION=$CLOUD_SDK_VERSION
#
#RUN apt-get -qqy update && apt-get install -qqy \
#        curl \
#        gcc \
#        python-dev \
#        python-setuptools \
#        apt-transport-https \
#        lsb-release \
#        openssh-client \
#        git \
#        gnupg \
#    && easy_install -U pip && \
#    pip install -U crcmod   && \
#    export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)" && \
#    echo "deb https://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" > /etc/apt/sources.list.d/google-cloud-sdk.list && \
#    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - && \
#    apt-get update && \
#    apt-get install -y google-cloud-sdk=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-app-engine-python=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-app-engine-python-extras=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-app-engine-java=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-app-engine-go=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-datalab=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-datastore-emulator=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-pubsub-emulator=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-bigtable-emulator=${CLOUD_SDK_VERSION}-0 \
#        google-cloud-sdk-cbt=${CLOUD_SDK_VERSION}-0 \
#        kubectl && \
#    gcloud config set core/disable_usage_reporting true && \
#    gcloud config set component_manager/disable_update_check true && \
#    gcloud config set metrics/environment github_docker_image && \
#    gcloud --version && kubectl version --client

ENTRYPOINT mvn exec:java -pl streaming-atomfeed -Dexec.mainClass=org.openmrs.analytics.FhirStreaming -Dexec.args="${SOURCE_URL} ${SOURCE_USERNAME}/${SOURCE_PW} ${SINK_URL}"