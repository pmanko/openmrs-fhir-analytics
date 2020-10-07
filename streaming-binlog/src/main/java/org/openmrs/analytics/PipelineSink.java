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

import java.util.Map;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A processor that sinks FHIR cloud and local
// TODO implement sink to local
public class PipelineSink implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(PipelineSink.class);
	
	// TODO: Autowire
	// FhirContext
	private static final FhirContext fhirContext = FhirContext.forR4();
	
	// Send to cloud TODO implement sink to local
	public void process(Exchange exchange) throws Exception {
		final Map kv = exchange.getMessage().getBody(Map.class);
		String resourceType = kv.get("resourceType").toString();
		String id = kv.get("id").toString();
		String fhirJson = exchange.getMessage().getBody(String.class);
		log.info("Sinking FHIR to Cloud ----> " + kv.get("resourceType") + "/" + kv.get("id"));
		
		Resource resource = (Resource) fhirContext.newJsonParser().parseResource(fhirJson);
		resource.setId(id);
		
		String fhirStoreUrl = System.getProperty("cloud.gcpFhirStore");
		
		// TODO: Autowire
		if (GcpStoreUtil.matchesGcpPattern(fhirStoreUrl))
			new GcpStoreUtil(fhirStoreUrl, fhirContext).uploadResourceToCloud(resource);
		else
			new FhirStoreUtil(fhirStoreUrl, fhirContext).uploadResourceToCloud(resource);
	}
}
