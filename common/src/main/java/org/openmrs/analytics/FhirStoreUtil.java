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
import java.net.URISyntaxException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FhirStoreUtil {
	
	protected static final Logger log = LoggerFactory.getLogger(FhirStoreUtil.class);
	
	protected static FhirContext fhirContext = FhirContext.forR4();
	
	protected String targetFhirStoreUrl;
	
	protected String sourceFhirUrl;
	
	protected String sourceUser;
	
	protected String sourcePw;
	
	public FhirStoreUtil(String targetFhirStoreUrl, String sourceFhirUrl, String sourceUser, String sourcePw) {
		this.targetFhirStoreUrl = targetFhirStoreUrl;
		this.sourceFhirUrl = sourceFhirUrl;
		this.sourceUser = sourceUser;
		this.sourcePw = sourcePw;
	}
	
	public void uploadResourceToCloud(String resourceId, Resource resource) {
		try {
			updateFhirResource(this.targetFhirStoreUrl, resourceId, resource);
		}
		catch (Exception e) {
			System.out.println(String.format("Exception while sending to sink: %s", e.toString()));
		}
	}
	
	private void updateFhirResource(String fhirStoreName, String resourceId, Resource resource)
	        throws IOException, URISyntaxException {
		IGenericClient client = fhirContext.newRestfulGenericClient(this.targetFhirStoreUrl);
		
		resource.setId(resourceId);
		
		// Initialize the client, which will be used to interact with the service.
		MethodOutcome outcome = client.update().resource(resource).withId(resourceId).prettyPrint().encodedJson().execute();
		
		System.out.println("Update FHIR resource create at" + this.targetFhirStoreUrl + "? " + outcome.getCreated());
	}
	
	public FhirContext getFhirContext() {
		return fhirContext;
	}
	
	public IGenericClient getSourceClient() {
		IClientInterceptor authInterceptor = new BasicAuthInterceptor(this.sourceUser, this.sourcePw);
		fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
		
		IGenericClient client = fhirContext.getRestfulClientFactory().newGenericClient(this.sourceFhirUrl);
		client.registerInterceptor(authInterceptor);
		
		return client;
	}
	
}
