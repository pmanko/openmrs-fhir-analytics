// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.openmrs.analytics;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import io.debezium.data.Envelope.Operation;
import org.apache.avro.generic.GenericRecord;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.parquet.hadoop.ParquetWriter;
import org.hl7.fhir.dstu3.model.Resource;
import org.openmrs.analytics.model.EventConfiguration;
import org.openmrs.analytics.model.GeneralConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A Debezium events to FHIR URI mapper
public class FhirConverter implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(FhirConverter.class);
	
	private final OpenmrsUtil openmrsUtil;
	
	private final FhirStoreUtil fhirStoreUtil;
	
	private final ParquetUtil parquetUtil;
	
	private final GeneralConfiguration generalConfiguration;
	
	@VisibleForTesting
	FhirConverter() {
		this.openmrsUtil = null;
		this.fhirStoreUtil = null;
		this.parquetUtil = null;
		this.generalConfiguration = null;
		
	}
	
	public FhirConverter(OpenmrsUtil openmrsUtil, FhirStoreUtil fhirStoreUtil, ParquetUtil parquetUtil) throws IOException {
		// TODO add option for switching to Parquet-file outputs.
		this.openmrsUtil = openmrsUtil;
		this.fhirStoreUtil = fhirStoreUtil;
		this.parquetUtil = parquetUtil;
		this.generalConfiguration = getEventsToFhirConfig(System.getProperty("fhir.debeziumEventConfigPath"));
		
	}
	
	public void process(Exchange exchange) {
		Message message = exchange.getMessage();
		final Map payload = message.getBody(Map.class);
		final Map sourceMetadata = message.getHeader(DebeziumConstants.HEADER_SOURCE_METADATA, Map.class);
		String operation = message.getHeader(DebeziumConstants.HEADER_OPERATION, String.class);
		// for malformed event, return null
		if (sourceMetadata == null || payload == null || operation == null) {
			return;
		}
		if (!operation.equals(Operation.CREATE.code()) && !operation.equals(Operation.UPDATE.code())) {
			log.debug("Skipping non create/update operation " + message.getHeaders());
			return;
		}
		final String table = sourceMetadata.get("table").toString();
		log.debug("Processing Table --> " + table);
		final EventConfiguration config = generalConfiguration.getEventConfigurations().get(table);
		
		if (config == null || !config.getLinkTemplates().containsKey("fhir")) {
			log.trace("Skipping unmapped data ..." + table);
			return;
		}
		if (!config.isEnabled()) {
			log.trace("Skipping disabled events ..." + table);
			return;
		}
		final String uuid = payload.get("uuid").toString();
		final String fhirUrl = config.getLinkTemplates().get("fhir").replace("{uuid}", uuid);
		log.info("Fetching FHIR resource at " + fhirUrl);
		Resource resource = openmrsUtil.fetchFhirResource(fhirUrl);
		if (resource == null) {
			// TODO: check how this can be signalled to Camel to be retried.
			return;
		}
		
		if (parquetUtil.getParquetPath() != null) {
			try {
				final ParquetWriter<GenericRecord> parquetWriter = parquetUtil.getWriter(resource.fhirType());
				parquetWriter.write(parquetUtil.convertToAvro(resource));
			}
			catch (IOException e) {
				log.error(String.format("Cannot create ParquetWriter Exception: %s", e));
			}
		} else {
			fhirStoreUtil.uploadResourceToCloud(resource);
		}
	}
	
	@VisibleForTesting
	GeneralConfiguration getEventsToFhirConfig(String fileName) throws IOException {
		Gson gson = new Gson();
		Path pathToFile = Paths.get(fileName);
		try (Reader reader = Files.newBufferedReader(pathToFile.toAbsolutePath(), StandardCharsets.UTF_8)) {
			GeneralConfiguration generalConfiguration = gson.fromJson(reader, GeneralConfiguration.class);
			return generalConfiguration;
		}
	}
}
