package org.openmrs.analytics;

import ca.uhn.fhir.context.FhirContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FhirStoreUtilTest {
	
	@Mock
	FhirContext fhirContext;
	
	@Before
	public void setup() {
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorWithMalformedStore() {
		FhirStoreUtil fhirStoreUtil = new GcpStoreUtil("test", fhirContext);
	}
	
	@Test
	public void testConstructor() {
		FhirStoreUtil fhirStoreUtil = new GcpStoreUtil(
		        "projects/my_project-123/locations/us-central1/datasets/openmrs_fhir_test/fhirStores/test", fhirContext);
	}
}
