#!/bin/bash
mvn exec:java -pl streaming-atomfeed -Dexec.mainClass=org.openmrs.analytics.FhirStreaming -Dexec.args="$SOURCE_URL $SOURCE_USERNAME/$SOURCE_PW $SINK_URL"