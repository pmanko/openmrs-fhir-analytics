package org.openmrs.analytics;

import org.hl7.fhir.r4.model.Resource;

public interface FhirStoreUtil {
	
	void uploadResourceToCloud(Resource resource);
}
