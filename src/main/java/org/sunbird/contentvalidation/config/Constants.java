package org.sunbird.contentvalidation.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constants {

	private Constants() {
	}

	public static final String TEXT_FIELD_CONSTANT = "text";

	public static final List<String> MINIMUL_FIELDS = Collections
			.unmodifiableList(Arrays.asList("identifier", "duration", "downloadUrl", "description", "mimeType",
					"artifactUrl", "name", "status", "resourceType", "categoryType", "category"));

	public static final boolean FETCH_ON_LEVEL = false;

	public static final boolean FIELDS_PASSED = true;

	public static final boolean SKIP_ACCESS_CHECK = true;

	public static final String CONTENT_ID_REPLACER = "{contentId}";

	public static final String ROOT_ORG_CONSTANT = "rootOrg";

	public static final String ORG_CONSTANT = "org";

	public static final String USER_ID_CONSTANT = "userId";

	public static final String FIELD_PASSED_CONSTANT = "fieldsPassed";

	public static final String FETCH_ONE_LEVEL_CONSTANT = "fetchOneLevel";

	public static final String SKIP_ACCESS_CHECK_CONSTANT = "skipAccessCheck";

	public static final String FIELDS_CONSTANT = "fields";

	public static final String DOWNLOAD_URL_PREFIX = "/apis/proxies/v8/";

	public static final String SERVICE_ERROR_CONSTANT = "External Service threw an Exception:";

	public static final String EXTERNAL_SERVICE_ERROR_CODE = "Exception while querying the external service:";

	public static final String FETCH_RESULT_CONSTANT = ".fetchResult:";

	public static final String REQUEST_CONSTANT = "Request: ";
	
	public static final String RESPONSE_CONSTANT = "Response: ";

	public static final String URI_CONSTANT = "URI: ";

	public static final String WORD_CONSTANT = "word";

	public static final String NO_OF_OCCURRENCE_CONSTANT = "no_of_occurrence";

	public static final String NOT_OFFENSIVE_CLASSIFICATION = "Not Offensive";

	public static final String OFFENSIVE_CLASSIFICATION = "Offensive";

	public static final String EXTREMELY_OFFENSIVE_CLASSIFICATION = "Extremly Offensive";

	public static final int NOT_OFFENSIVE = 1;

	public static final int OFFENSIVE = 2;

	public static final int EXTREMELY_OFFENSIVE = 3;

	public static final String LOCAL_BASE_PATH= "/tmp/";
}
