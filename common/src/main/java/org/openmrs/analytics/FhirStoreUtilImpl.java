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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FhirStoreUtilImpl implements FhirStoreUtil {
	
	private static final Logger log = LoggerFactory.getLogger(FhirStoreUtilImpl.class);
	
	private FhirContext fhirContext;
	
	private String sinkUrl;
	
	FhirStoreUtilImpl(String sinkUrl, FhirContext fhirContext) throws IllegalArgumentException {
		this.fhirContext = fhirContext;
		this.sinkUrl = sinkUrl;
	}
	
	@Override
	public void uploadResourceToCloud(Resource resource) {
		try {
			updateFhirResource(sinkUrl, resource);
		}
		catch (Exception e) {
			System.out.println(String.format("Exception while sending to sink: %s", e.toString()));
		}
	}
	
	private void updateFhirResource(String sinkUrl, Resource resource) {
		IGenericClient client = fhirContext.newRestfulGenericClient(sinkUrl);
		
		// Initialize the client, which will be used to interact with the service.
		MethodOutcome outcome = client.update().resource(resource).withId(resource.getIdElement().getIdPart()).prettyPrint()
		        .encodedJson().execute();
		
		System.out.println("FHIR resource created at" + sinkUrl + "? " + outcome.getCreated());
		
		log.debug("Update FHIR resource response: " + outcome.getOperationOutcome().toString());
	}
	
}
