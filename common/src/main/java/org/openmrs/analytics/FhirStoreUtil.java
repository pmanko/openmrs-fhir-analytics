package org.openmrs.analytics;

import java.util.Collection;
import java.util.Collections;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FhirStoreUtil {
	
	private static final Logger log = LoggerFactory.getLogger(FhirStoreUtil.class);
	
	protected FhirContext fhirContext;
	
	protected String sinkUrl;
	
	public FhirStoreUtil(String sinkUrl, FhirContext fhirContext) throws IllegalArgumentException {
		this.fhirContext = fhirContext;
		this.sinkUrl = sinkUrl;
	}
	
	public void uploadResourceToCloud(Resource resource) {
		try {
			updateFhirResource(sinkUrl, resource, Collections.<IClientInterceptor> emptyList());
		}
		catch (Exception e) {
			System.out.println(String.format("Exception while sending to sink: %s", e.toString()));
		}
	}
	
	protected void updateFhirResource(String sinkUrl, Resource resource, Collection<IClientInterceptor> interceptors) {
		IGenericClient client = fhirContext.newRestfulGenericClient(sinkUrl);
		
		for (IClientInterceptor interceptor : interceptors) {
			client.registerInterceptor(interceptor);
		}
		
		// Initialize the client, which will be used to interact with the service.
		MethodOutcome outcome = client.update().resource(resource).withId(resource.getIdElement().getIdPart()).prettyPrint()
		        .encodedJson().execute();
		
		System.out.println("FHIR resource created at" + sinkUrl + "? " + outcome.getCreated());
		
		log.debug("Update FHIR resource response: " + outcome.getOperationOutcome().toString());
	}
}
