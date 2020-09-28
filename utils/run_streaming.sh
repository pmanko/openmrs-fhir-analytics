#!/bin/bash
mvn exec:java -pl streaming -Dexec.mainClass=org.openmrs.analytics.FhirStreaming -Dexec.args="$SOURCE_URL $SOURCE_USERNAME/$SOURCE_PW $SINK_URL"
#mvn exec:java -pl streaming -Dexec.mainClass=org.openmrs.analytics.FhirStreaming -Dexec.args="http://52.37.13.123:8080/openmrs/ $SOURCE_USERNAME/$SOURCE_PW $SINK_URL"
# fg
