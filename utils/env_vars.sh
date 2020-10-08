#!/bin/bash

export SOURCE_URL=https://isanteplusdemo.com/openmrs
export SOURCE_FEED_ENDPOINT=/ws/atomfeed
export SOURCE_FHIR_ENDPOINT=/ws/fhir2/R4
export SOURCE_PW=Admin123
export SOURCE_USERNAME=admin
export SINK_URL=http://18.158.139.243:8092/hapi-fhir-jpaserver/fhir
export DB_USERNAME=admin
export DB_PASSWORD=hapifhirjpa
export DB_CONNECTION='jdbc:mysql://openmrs-hapi-db.chijnl5rwr2c.eu-central-1.rds.amazonaws.com:3306/atomfeed_client?autoReconnect=true&useSSL=false'