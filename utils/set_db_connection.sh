#!/bin/bash
cp ./streaming/src/main/resources/hibernate.default.properties.template ./streaming/src/main/resources/hibernate.default.properties

sed -i "s/<DB_USERNAME>/$DB_USERNAME/g" ./streaming/src/main/resources/hibernate.default.properties
sed -i "s/<DB_PASSWORD>/$DB_PASSWORD/g" ./streaming/src/main/resources/hibernate.default.properties

ESCAPED_REPLACE=$(printf '%s\n' "$DB_CONNECTION" | sed -e 's/[\/&]/\\&/g')
sed -i "s/<DB_CONNECTION>/$ESCAPED_REPLACE/g" ./streaming/src/main/resources/hibernate.default.properties

mvn -p streaming clean install -DskipTests
