package org.openmrs.analytics;

import java.io.Serializable;

import com.google.auto.value.AutoValue;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

@DefaultCoder(SerializableCoder.class)
@AutoValue
abstract class SearchSegmentDescriptor implements Serializable {

	static SearchSegmentDescriptor create(String resourceType, int pageOffset, int count) {
		return new AutoValue_SearchSegmentDescriptor(resourceType, pageOffset, count);
	}

	abstract String pagesId();

	abstract int pageOffset();

	abstract int count();
}
