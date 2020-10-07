[![Build Status](https://travis-ci.org/GoogleCloudPlatform/openmrs-fhir-analytics.svg?branch=master)](https://travis-ci.org/GoogleCloudPlatform/openmrs-fhir-analytics)

**NOTE**: This is a work in progress and the current version is only for
demonstration purposes. Once these tools reach Alpha status, this note should
be removed. This is a collaboration between Google and OpenMRS community and
this repository might be moved to [OpenMRS org](https://github.com/openmrs)
in the future.

# What is this?
This repository includes tools for transforming [OpenMRS](openmrs.org) data
into a FHIR based warehouse. There are two aspects to this transformation:
- **Streaming mode**: In this mode, there is an intermediate binary that
continuously listens to changes in OpenMRS to translate new changes into FHIR
resources and upload them to the target data warehouse.
- **Bulk upload** (a.k.a. _batch mode_): This is used for reading the whole
content of OpenMRS MySQL database, transform it into FHIR resources, and upload
to the target data warehouse.

# Streaming mode (Atom Feed)
This is currently implemented as a stand alone app that sits between OpenMRS and
the data warehouse. It currently depends on the [Atom Feed module of OpenMRS](
https://wiki.openmrs.org/display/docs/Atom+Feed+Module). To use the  [Debezium](
https://debezium.io/documentation/reference/1.2/connectors/mysql.html) 
based streaming mode go to [Streaming mode (Debezium)](#streaming-mode-using-debezium) section.

The source of data is OpenMRS and the only sink currently implemented is
[GCP FHIR store](https://cloud.google.com/healthcare/docs/concepts/fhir) and
[BigQuery](https://cloud.google.com/bigquery) but it should be easy to add
other FHIR based sinks.

The steps for using this tool are:
- Add Atom Feed module to OpenMRS.
- Add FHIR2 module to OpenMRS and update Atom Feed config.
- Set up the Atom Feed client side database.
- Create the sink FHIR store and BigQuery dataset.
- Compile and run the streaming app.

## Add Atom Feed module to OpenMRS
Assuming that you are using the Reference App of OpenMRS (for example, installed
at `http://localhost:9016`), after login, go to
"System Administration" > "Manage Modules" > "Search from Addons" and search
for the Atom Feed module (e.g., install version 1.0.12).

For any changes in the OpenMRS database, this module creates entries with
payloads related to that change, including URLs for FHIR resources if the
change has corresponding FHIR resources.

## Add FHIR2 module to OpenMRS and update Atom Feed config
The [FHIR module in OpenMRS](
https://wiki.openmrs.org/display/projects/OpenMRS+FHIR+Module) is being
reimplemented in the `fhir2` module. You need to compile this module from
[source](https://github.com/openmrs/openmrs-module-fhir2) and install the
`omod/target/fhir2-1.0.0-SNAPSHOT.omod` module in OpenMRS (or copy that file to
the `modules` directory of your OpenMRS installation).

The URLs for FHIR resources of this module have the form `/ws/fhir2/R4/RESOURCE`
e.g., `http://localhost:9016/openmrs/ws/fhir2/R4/Patient`. Therefor we need to
update the Atom Feed module config to produce these URLs. To do this, from the
OpenMRS Ref. App home page, choose the "Atomfeed" option (this should appear
once the Atom Feed module is installed) and click on "Load Configuration". From
the top menu, choose the file provided in this repository at
[`utils/fhir2_atom_feed_config.json`](utils/fhir2_atom_feed_config.json) and
click "Import".

To check the above two steps, create a new Patient (or an Observation) and
verify that you can see that Patient (or Observation) at the aforementioned
FHIR URL. Then check the Atom Feed URL corresponding to your change, e.g.,

`http://localhost:9016/openmrs/ws/atomfeed/patient/1`

OR

`http://localhost:9016/openmrs/ws/atomfeed/observation/1`

## Set up the Atom Feed client side database
The Atomfeed client requires a database to store failed event and marker information, and we'll
use MySQL for this purpose. If you don't have an available MySQL service, you can start one up using docker:
`docker run -e "MYSQL_ROOT_PASSWORD=root" -p 127.0.0.1:3306:3306 --name=atomfeed-db -d mysql/mysql-server:latest`

Now you should have MySQL running on the default port 3306, and can run:

`mysql --user=USER --password=PASSWORD < utils/dbdump/create_db.sql`

This will create a database called `atomfeed_client` with required tables (the
`USER` should have permission to create databases). If you want to change the
default database name `atomfeed_client`, you can edit [`utils/dbdump/create_db.sql`](
utils/dbdump/create_db.sql) but then you need to change the database name in
[`src/main/resources/hibernate.default.properties`](
src/main/resources/hibernate.default.properties) accordingly.

## Create the sink FHIR store and BigQuery dataset
To set up GCP project that you can use as a sink FHIR store:

- Create a new project in [GCP](https://console.cloud.google.com). For an overview of projects, datasets and data stores check [this document](https://cloud.google.com/healthcare/docs/concepts/projects-datasets-data-stores).
- Enable Google Cloud Healthcare API in the project and create a Google Cloud Healthcare dataset
- Create a FHIR data store in the dataset with the R4 FHIR version
- Enable the BigQuery API in the project and dataset with the same name in the project
- Download, install, and initialize the `gcloud` cli: https://cloud.google.com/sdk/docs/quickstart
- Make sure you can authenticate with the project using the CLI: https://developers.google.com/identity/sign-in/web/sign-in
  * `gcloud init`
  * `gcloud auth application-default login` (might not be necessary)
  * Create a service account for the project, generate a key, and save it securely locally
  * Add the `bigquery.dataEditor` and `bigquery.jobUser` roles to the project in the `IAM & Admin`/`Roles` settings or using the cli:
    - `gcloud projects add-iam-policy-binding openmrs-260803 --role roles/bigquery.admin --member serviceAccount:openmrs-fhir-analytics@openmrs-260803.iam.gserviceaccount.com`
    - `gcloud projects add-iam-policy-binding openmrs-260803 --role roles/healthcare.datasetAdmin --member serviceAccount:openmrs-fhir-analytics@openmrs-260803.iam.gserviceaccount.com`
  * Activate the service account for your project using `gcloud auth activate-service-account <your-service-account> --key-file=<your-key-file> --project=<your project>`
  * Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable: https://cloud.google.com/docs/authentication/getting-started#setting_the_environment_variable
 
7. Use the script [`utils/create_fhir_store.sh`](utils/create_fhir_store.sh) to create
a FHIR store in this dataset which stream the changes to the BigQuery dataset
as well:
  `./utils/create_fhir_store.sh PROJECT LOCATION DATASET FHIR-STORE-NAME`
  - `PROJECT` is your GCP project.
  - `LOCATION` is GCP location where your dataset resides, e.g., `us-central1`.
  - `DATASET` is the name of the dataset you created.
  - `FHIR-STORE-NAME` is what it says.

  *Note: If you get `PERMISSION_DENIED` errors, make sure to `IAM & ADMIN`/`IAM`/`Members` and add the `bigquery.dataEditor` and `bigquery.jobUser` roles to the `Cloud Healthcare Service Agent` service account that shows up.*

You can run the script with no arguments to see a sample usage. After you create
the FHIR store, its full URL would be:

`https://healthcare.googleapis.com/v1/projects/PROJECT/locations/LOCATION/datasets/DATASET/fhirStores/FHIR-STORE-NAME`
                    
## Compile and run the streaming app
From the root of your git repo, run:

`mvn clean install`

and then:

```
mvn exec:java -pl streaming-atomfeed \
  -Dexec.mainClass=org.openmrs.analytics.FhirStreaming \`
  -Dexec.args="OPENMRS_URL OPENMRS_USER/OPENMRS_PASSWORD GCP_FHIR_STORE"`
```

- `OPENMRS_URL` is the path to your source OpenMRS instance (e.g., `http://localhost:9016/openmrs` in this case)
-  `OPENMRS_USER/OPENMRS_PASSWORD` is the username/password combination for accessing the OpenMRS APIs using BasicAuth.
- `GCP_FHIR_STORE` is the relative path of the FHIR store you set up in the
previous step, i.e., something like:
`projects/PROJECT/locations/LOCATION/datasets/DATASET/fhirStores/FHIR-STORE-NAME`
where all-caps segments are based on what you set up above.

To test your changes, create a new patient (or observation) in OpenMRS and check
that a corresponding Patient (or Observation) FHIR resource is created in the
GCP FHIR store and corresponding rows added to the BigQuery tables.

# Streaming mode using Debezium
The goal of the debezium-based streaming mode is to provide real-time downstream consumption of incremental updates, 
even for operations that were performed outside OpenMRS API, e.g.,  data cleaning, module operations,
 and data syncing/migration. It captures incremental updates from the MySQL database binlog then streams both FHIR and non-FHIR 
 data for downstream consumption. 
It is not based on Hibernate Interceptor or Event Module; therefore, all events are captured from day 0 and can be used 
independently without the need for a batch pipeline. 
  It guarantees tolerance to failure such that when the application is restarted or crashed, the pipeline will resume 
  from the last processed offset.
   
### Getting Started

 - Fire up OpenMRS Stack (containing FHIR2 module and demo data ~ 300,000 obs)

```
 docker-compose -f openmrs-compose.yaml up # change ports appropriately (optional)
```
 You should be able to  access OpenMRS via  http://localhost:8099/openmrs/ using refApp credentials i.e username is admin and password Admin123
 
 
  - Run the streaming pipeline using default config (pointed to openmrs-compose.yaml )
 
 ```
  $ mvn clean install
  $ mvn compile exec:java -pl streaming-binlog
  
 ```

 - Or customize the configuration (including gcpFhirStore, OpenMRS basicAuth)
 
 ```
  $ mvn compile exec:java -pl streaming-binlog -Ddatabase.hostname=localhost -Ddatabase.port=3306 -Ddatabase.user=root -Ddatabase.password=debezium -Ddatabase.dbname=mysql -Ddatabase.schema=openmrs -Ddatabase.serverId=77 -Ddatabase.offsetStorage=offset.dat -Ddatabase.databaseHistory=dbhistory.dat -Dopenmrs.username=admin -Dopenmrs.password=Admin123 -Dopenmrs.serverUrl=http://localhost:8099 -Dopenmrs.fhirBaseEndpoint=/openmrs/ws/fhir2/R4 -Dcloud.gcpFhirStore=projects/PROJECT/locations/LOCATION/datasets/DATASET/fhirStores/FHIRSTORENAME
 ```

 

### Debezium prerequisite
 The provided openmrs-compose.yaml (MySQL) has been configured to support debezium, however, 
 to connect to an existing MySQl instance, you'll need to configure your DB to 
 support row-based replication. There are a few configuration options required to ensure your database can 
participate in emitting binlog. 

- The source MySQL instance must have the following server configs set
 to generate binary logs in a format it can be consumed:
    - server_id = <value>
    - log_bin = <value>
    - binlog_format = row
    - binlog_row_image = full
- Depending on server defaults and table size, it may also be necessary to 
increase the binlog retention period.

An example of mysql.conf has bee provided with the above configurations

```
# ----------------------------------------------
# Enable the binlog for replication & CDC
# ----------------------------------------------

# Enable binary replication log and set the prefix, expiration, and log format.
# The prefix is arbitrary, expiration can be short for integration tests but would
# be longer on a production system. Row-level info is required for ingest to work.
# Server ID is required, but this will vary on production systems
server-id         = 223344
log_bin           = mysql-bin
expire_logs_days  = 1
binlog_format     = row

```

For more information, please visit https://debezium.io

# Batch mode 
The steps above for setting up a FHIR Store and a linked BigQuery dataset needs
to be followed. Once it is done, and after `mvn install`, the pipeline can be
run using a command like:

```
java -cp batch/target/fhir-batch-etl-bundled-0.1.0-SNAPSHOT.jar \
  org.openmrs.analytics.FhirEtl --serverUrl=http://localhost:9018 \
  --jsessionId=2950DA4C142EC44145978C02EDA0F311 \
  --searchList=Patient,Encounter,Observation --batchSize=20 \
 --targetParallelism=20 --gcpFhirStore=GCP_FHIR_STORE`
```
The `searchList` argument accepts a comma separated list of FHIR search URLs.
For example, one can use `Patient?given=Susan` to extract only Patient resources
that meet the `given=Susan` criteria.

# Using Docker compose
Alternatively you can spin up the entire pipeline using docker containers by running

#### 1. Fire up OpenMRS (containing FHIR2 module)

```
 docker-compose -f openmrs-compose.yaml up # change ports appropriately (optional)
```
 You should be able to access OpenMRS via  http://localhost:8099/openmrs/ using refApp credentials i.e username is admin and password Admin123
 Please remember to install OpenMRS demo data module!
 
 #### 2. Extract JSESSION_ID by authenticate against your OpenMRS instance using

```
 curl -u <username>:<password> http://<server name>/openmrs/ws/rest/v1/session 
```

#### 3. Configure ./docker-compose.yaml

Parameters (e.g port/url ) have been configured to point to http://localhost:8099/openmrs/ 

Remember to appropriately change other parameters such as JSESSION_ID (extracted in step #2 above), GCP and OpenMRS host.

#### 4. Fire up Batch Pipeline

```
 $ mvn clean install
 $ docker-compose up --build batch

``` 

#### 5. Fire up Streaming Pipeline (Debezium)

```
 $ mvn clean install
 $ docker-compose up --build streaming-binlog
```

#### 6. Fire up Streaming Pipeline (Atomfeed)

```
 $ mvn clean install
 $ docker-compose up -d --build streaming-atomfeed-db streaming-atomfeed
```
 
**TODO**: Add details on how this works and caveats!

