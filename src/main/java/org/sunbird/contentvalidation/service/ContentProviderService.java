package org.sunbird.contentvalidation.service;

import java.io.InputStream;

import org.sunbird.contentvalidation.model.HierarchyResponse;

public interface ContentProviderService {

	public InputStream getContentFile(String downloadUrl);

	public HierarchyResponse getHeirarchyResponse(String rootOrg, String org, String contentId, String userId);

	public InputStream getContentFileV2(String downloadUrl);


}
