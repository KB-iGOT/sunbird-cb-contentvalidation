package org.sunbird.contentvalidation.service;

import java.io.IOException;

import org.sunbird.contentvalidation.model.ContentPdfValidation;
import org.sunbird.contentvalidation.model.ProfanityResponseWrapper;
import org.sunbird.contentvalidation.repo.model.PdfDocValidationResponse;

public interface ContentValidationService {

	public ProfanityResponseWrapper validateContent(String rootOrg, String org, String contentId, String userId)
			throws IOException;

	public PdfDocValidationResponse validatePdfContent(ContentPdfValidation contentPdfValidation)
			throws IOException;

	public PdfDocValidationResponse validateLocalPdfContent(ContentPdfValidation contentPdfValidation)
			throws IOException;

	public PdfDocValidationResponse validatePdfContentV2(ContentPdfValidation contentPdfValidation)
			throws IOException;
}
