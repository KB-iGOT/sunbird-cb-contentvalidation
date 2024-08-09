package org.sunbird.contentvalidation.service.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.sunbird.contentvalidation.config.Configuration;
import org.sunbird.contentvalidation.config.Constants;
import org.sunbird.contentvalidation.model.*;
import org.sunbird.contentvalidation.repo.ContentValidationRepoServiceImpl;
import org.sunbird.contentvalidation.repo.PdfDocValidationRepository;
import org.sunbird.contentvalidation.repo.model.PdfDocValidationResponse;
import org.sunbird.contentvalidation.repo.model.PdfDocValidationResponsePrimaryKey;
import org.sunbird.contentvalidation.service.ContentProviderService;
import org.sunbird.contentvalidation.service.ContentValidationService;
import org.sunbird.contentvalidation.util.CommonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;

@Service
@Log4j2
public class ContentValidationServiceImpl implements ContentValidationService {

	@Autowired
	private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Configuration configuration;

	@Autowired
	private ContentProviderService contentProviderService;

	@Autowired
	ContentValidationRepoServiceImpl repoService;

	@Autowired
	private CommonUtils commonUtils;

	@Autowired
	private PdfDocValidationRepository pdfRepo;

	/**
	 * Get the profanity check list for pdf
	 *
	 * @param fileInputStream
	 * @return
	 * @throws IOException
	 */
	private ProfanityResponseWrapper getTheProfanityCheckList(InputStream fileInputStream, String fileName)
			throws IOException {
		List<Profanity> profanityList = new ArrayList<>();
		ProfanityResponseWrapper profanityResponseWrapper = new ProfanityResponseWrapper();
		PDDocument doc = null;
		PDFTextStripper pdfStripper = null;
		String pageText = null;
		try {
			doc = PDDocument.load(fileInputStream);
			int pageCount = doc.getPages().getCount();
			for (int i = 1; i <= pageCount; i++) {
				pdfStripper = new PDFTextStripper();
				pdfStripper.setStartPage(i);
				pdfStripper.setEndPage(i);
				pageText = pdfStripper.getText(doc);
				extractImagesAndUpdateTheResponse(doc, i, profanityResponseWrapper);
				if (!StringUtils.isEmpty(pageText)) {
					Profanity profanityResponse = getProfanityCheckForText(pageText);
					profanityList.add(profanityResponse);
					updateProfanityWordOccurence(profanityResponse, i, profanityResponseWrapper);
				}
			}

			profanityResponseWrapper.setTotalPageUploaded(pageCount);
			profanityResponseWrapper.setProfanityList(profanityList);
			profanityResponseWrapper.setFileName(fileName);
		} catch (IOException e) {
			log.error("Exception occured while reading the pdf file");
			throw new IOException(e);
		}
		return profanityResponseWrapper;
	}

	/**
	 *
	 * @param rootOrg
	 * @param org
	 * @param contentId
	 * @param userId
	 * @return Get the profanity response
	 */
	@Override
	public ProfanityResponseWrapper validateContent(String rootOrg, String org, String contentId, String userId)
			throws IOException {
		HierarchyResponse hierarchyResponse = contentProviderService.getHeirarchyResponse(rootOrg, org, contentId,
				userId);
		InputStream inputStream = contentProviderService.getContentFile(hierarchyResponse.getDownloadUrl());
		return getTheProfanityCheckList(inputStream, commonUtils.getFileName(hierarchyResponse.getArtifactUrl()));
	}

	/**
	 * Query to the profanity service and get the result
	 *
	 * @param text
	 * @return return the Profanity result
	 */
	public Profanity getProfanityCheckForText(String text) {
		HashMap<String, Object> requestObject = new HashMap<>();
		requestObject.put(Constants.TEXT_FIELD_CONSTANT, text);
		StringBuilder url = new StringBuilder();
		url.append(configuration.getProfanityServiceHost()).append(configuration.getProfanityServicePath());
		Object response = outboundRequestHandlerService.fetchResultUsingPost(url.toString(), requestObject);
		return mapper.convertValue(response, Profanity.class);
	}

	/**
	 * Provides the Profanity analysis for PDF content
	 * 
	 * @param contentPdfValidation - Contains PDF File details
	 * 
	 * @return Returns the validation details of the given PDF file
	 */
	public PdfDocValidationResponse validatePdfContent(ContentPdfValidation contentPdfValidation) throws IOException {
		StringBuilder logStr = null;
		PdfDocValidationResponse response =  null;
		if (log.isDebugEnabled()) {
			logStr = new StringBuilder();
			logStr.append("ValidatePDFContent request: ").append(mapper.writeValueAsString(contentPdfValidation));
		}
		long startTime = System.currentTimeMillis();
		InputStream inputStream = contentProviderService.getContentFile(contentPdfValidation.getPdfDownloadUrl());
		try {
			if (logStr != null) {
				logStr.append("Time taken to download PDF File: ").append(System.currentTimeMillis() - startTime)
						.append(" milliseconds");
			}
			String fileName = contentPdfValidation.getPdfDownloadUrl().split("/")[7];
			response = performProfanityAnalysis(inputStream, fileName,
					contentPdfValidation.getContentId());
			if (logStr != null) {
				logStr.append("Time take to validate PDF Content: ").append(System.currentTimeMillis() - startTime)
						.append(" milliseconds");
				log.debug(logStr.toString());
			}
		}
		finally {
			IOUtils.closeQuietly(inputStream);
		}
		return response;
	}

	public PdfDocValidationResponse validateLocalPdfContent(ContentPdfValidation contentPdfValidation)
			throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("validateLocalPdfContent request: {}", mapper.writeValueAsString(contentPdfValidation));
		}
		long startTime = System.currentTimeMillis();
		File pdfFile = new File(contentPdfValidation.getPdfDownloadUrl());
		PdfDocValidationResponse response = new PdfDocValidationResponse();
		if (pdfFile.exists()) {
			response = performProfanityAnalysis(new DataInputStream(new FileInputStream(pdfFile)), pdfFile.getName(),
					contentPdfValidation.getContentId());
		} else {
			log.error("File doesn't exist at path: {}", contentPdfValidation.getPdfDownloadUrl());
		}
		if (log.isDebugEnabled()) {
			log.debug("Time take to validate PDF Content: {} milliseconds", System.currentTimeMillis() - startTime);
			log.debug("ContentPdfValidationResponse: {}", mapper.writeValueAsString(response));
		}
		return response;
	}

	/**
	 * Extract the image and update the count of responsewrapper
	 *
	 * @param doc
	 * @param index
	 * @param responseWrapper
	 * @throws IOException
	 */
	private void extractImagesAndUpdateTheResponse(PDDocument doc, int index, ProfanityResponseWrapper responseWrapper)
			throws IOException {
		PDPage page = doc.getPages().get(index - 1);
		int totalImageOnthePage = 0;
		PDResources resources = page.getResources();
		for (COSName name : resources.getXObjectNames()) {
			PDXObject o = resources.getXObject(name);
			if (o instanceof PDImageXObject) {
				totalImageOnthePage++;
			}
		}
		if (totalImageOnthePage > 0) {
			if (CollectionUtils.isEmpty(responseWrapper.getImagesOccurrenceOnPageNo())) {
				responseWrapper.setImagesOccurrenceOnPageNo(new HashSet<>());
			}
			responseWrapper.getImagesOccurrenceOnPageNo().add(index);
			responseWrapper.setPagesWithImages(responseWrapper.getPagesWithImages() + 1);
		}
	}

	/**
	 * Update the word count and occurence on each page
	 *
	 * @param profanity
	 * @param pageNo
	 * @param responseWrapper
	 */
	private void updateProfanityWordOccurence(Profanity profanity, int pageNo,
			ProfanityResponseWrapper responseWrapper) {
		responseWrapper.setOverAllOffensivescore(responseWrapper.getOverAllOffensivescore()
				+ profanity.getOverallTextClassification().getProbability());
		if (profanity.getPossibleProfaneWordCount() > 0) {
			HashMap<String, ProfanityWordCount> wordCountMap = responseWrapper.getProfanityClassifications();
			ProfanityWordCount wordCount = null;
			int size = profanity.getPossibleProfanityFrequency().size();
			for (int i = 0; i < size; i++) {
				if (CollectionUtils.isEmpty(wordCountMap)) {
					wordCountMap = new HashMap<>();
				}
				String profaneWord = profanity.getPossibleProfanityFrequency().get(i).getWord();
				Integer totalWordCount = profanity.getPossibleProfanityFrequency().get(i)
						.getNoOfOccurrence();
				if (ObjectUtils.isEmpty(wordCountMap.get(profaneWord))) {
					wordCount = new ProfanityWordCount();
					wordCount.setOffenceCategory(profanity.getOverallTextClassification().getClassification());
					wordCount.setOccurenceOnPage(new HashSet<>());
					wordCount.getOccurenceOnPage().add(pageNo);
					wordCount.setTotalWordCount(totalWordCount);
					wordCountMap.put(profaneWord, wordCount);
					responseWrapper.setProfanityWordCount(responseWrapper.getProfanityWordCount() + 1);
				} else {
					wordCountMap.get(profaneWord)
							.setOffenceCategory(profanity.getOverallTextClassification().getClassification());
					wordCountMap.get(profaneWord).getOccurenceOnPage().add(pageNo);
				}
			}
			responseWrapper.setProfanityClassifications(wordCountMap);
		}
	}

	private PdfDocValidationResponse performProfanityAnalysis(InputStream inputStream, String fileName,
			String contentId) throws IOException {
		PdfDocValidationResponse response = new PdfDocValidationResponse();
		response.setPrimaryKey(
				PdfDocValidationResponsePrimaryKey.builder().contentId(contentId).pdfFileName(fileName).build());
		PDDocument doc = PDDocument.load(inputStream);

		try {
			Splitter splitter = new Splitter();
			List<PDDocument> docPages = splitter.split(doc);
			enrichTotalPages(contentId, fileName, docPages.size());
			PDFTextStripper pdfStripper = null;
			long totalTime = 0l;
			double overAllClassification = 0.0;
			for (int p = 0; p < docPages.size(); p++) {
				long startTime = System.currentTimeMillis();
				pdfStripper = new PDFTextStripper();
				pdfStripper.setAddMoreFormatting(false);
				pdfStripper.setLineSeparator(" ");
				String text = pdfStripper.getText(docPages.get(p));
				if (!StringUtils.isEmpty(text) && !commonUtils.emptyCheck(text)) {
					Profanity profanityResponse = getProfanityCheckForText(text);
					log.debug("Page wise analysis PageNo: {}, Analysis: {}", p,
							mapper.writeValueAsString(profanityResponse));
					if(!CollectionUtils.isEmpty(profanityResponse
							.getPossibleProfanityCategorical())){
						for (Map.Entry<String, ProfanityCategorial> profanityCategorial : profanityResponse
								.getPossibleProfanityCategorical().entrySet()) {
							Map.Entry<String, String> details = profanityCategorial.getValue().getDetails().entrySet().iterator().next();
							String category = details.getKey();
							ProfanityWordFrequency wordFrequency = new ProfanityWordFrequency();
							wordFrequency.setWord(profanityCategorial.getKey());
							wordFrequency.setNoOfOccurrence(profanityCategorial.getValue().getCount());
							wordFrequency.setCategory(category);
							wordFrequency.addPageOccurred(getPageNumberForIndex(p));
							response.addProfanityWordFrequency(wordFrequency);
							response.incrementProfanityWordCount();
						}
					}
					overAllClassification += profanityResponse.getOverallTextClassification().getProbability();
					if (StringUtils.isEmpty(response.getOverallTextClassification())) {
						response.setOverallTextClassification(
								profanityResponse.getOverallTextClassification().getClassification());
					} else {
						response.setOverallTextClassification(
								commonUtils.getProfanityClassification(response.getOverallTextClassification(),
										profanityResponse.getOverallTextClassification().getClassification()));
					}
				}
				response.incrementTotalNoOfPagesCompleted();
				//extractImagesAndUpdateThPdfeResponse(fileName, doc.getPages().get(p), p, response);
				long perPageTime = System.currentTimeMillis() - startTime;

				if (log.isDebugEnabled()) {
					log.debug("Time taken to perform Profanity Analysis for page {} is {} milliseconds",
							getPageNumberForIndex(p), perPageTime);
				}
				totalTime += perPageTime;
				repoService.updateContentValidationResult(response, false);
			}
			response.setScore(overAllClassification /  docPages.size());

			if (log.isDebugEnabled()) {
				log.debug("Time taken to perform Profanity Analysis for document {} is {} milliseconds", fileName,
						totalTime);
			}
			repoService.updateContentValidationResult(response, true);
		}
		finally {
			doc.close();
		}
		return response;
	}


	private int getPageNumberForIndex(int index) {
		return ++index;
	}

	/**
	 *
	 * @param page
	 * @param index
	 * @param response
	 */
	private void extractImagesAndUpdateThPdfeResponse(String fileName, PDPage page, int index, PdfDocValidationResponse response) {
		PDResources resources = page.getResources();
		int count = 0;
		try {
			for (COSName name : resources.getXObjectNames()) {
				PDXObject o = null;
				int imageNo = 0;
				Map<Integer, List<ProfanityImageAnalysis>> profanityImageAnalysisMap = new HashMap<>();
				Map<Integer, List<ProfanityIndiaMapAnalysis>> indiaAnalysisMap = new HashMap<>();
				List<ProfanityImageAnalysis> profanityImageAnalysisList = new ArrayList<>();
				List<ProfanityIndiaMapAnalysis> profanityIndiaMapList = new ArrayList<>();
				ProfanityImageAnalysis profanityImageAnalysis = null;
				ProfanityIndiaMapAnalysis profanityIndiaMapAnalysis = null;
				o = resources.getXObject(name);
				if (o instanceof PDImageXObject) {
					profanityImageAnalysis = new ProfanityImageAnalysis();
					profanityIndiaMapAnalysis = new ProfanityIndiaMapAnalysis();
					imageNo++;
					response.incrementTotalPagesImages();
					response.addImageOccurances(index);
					PDImageXObject image = (PDImageXObject) o;
					File f = File.createTempFile(fileName + "_" + index + "_", Integer.toString(count++) + ".png");
					ImageIO.write(image.getImage(), "png", f);
					log.info("---------------------------- Image Result -------------------------------");
					log.info("FileName : " + f.getName());
					Map<String, Object> extResponse = getProfanityCheckForImage(f.getName(), f);
					ImageResponse imageResponse = mapper.convertValue(extResponse, ImageResponse.class);
					log.info("" + imageResponse);
					if (!ObjectUtils.isEmpty(imageResponse.getPayload()) && !ObjectUtils.isEmpty(imageResponse.getPayload().getImageProfanity())) {
						profanityImageAnalysis.setClassification(imageResponse.getPayload().getClassification());
						profanityImageAnalysis.setImageNo(imageNo);
						profanityImageAnalysis.setSafe(imageResponse.getPayload().getImageProfanity().isSafe());
						profanityImageAnalysisList.add(profanityImageAnalysis);
					}
					if (!ObjectUtils.isEmpty(imageResponse.getPayload()) && !ObjectUtils.isEmpty(imageResponse.getPayload().getIndia_classification())) {
						profanityIndiaMapAnalysis.setImageNo(imageNo);
						profanityIndiaMapAnalysis.setProbability((float)(imageResponse.getPayload().getIndia_classification().get(0).getPercentage_probability() / 100));
						profanityIndiaMapAnalysis.setIncorrectPercentage((float)(imageResponse.getPayload().getIndia_classification().get(0).getClassification().getIncorrectPercentage() / 100));
						profanityIndiaMapAnalysis.setIsIndiaMapDetected(false);
						if(profanityIndiaMapAnalysis.getIncorrectPercentage() < .50){
							profanityIndiaMapAnalysis.setIsIndiaMapDetected(true);
						}
						profanityIndiaMapList.add(profanityIndiaMapAnalysis);
					}
				}
				if(!CollectionUtils.isEmpty(profanityImageAnalysisList)){
					profanityImageAnalysisMap.put(index+1, profanityImageAnalysisList);
					response.addProfanityImageAnalysis(profanityImageAnalysisMap);
				}
				if(!CollectionUtils.isEmpty(profanityIndiaMapList)){
					indiaAnalysisMap.put(index+1, profanityIndiaMapList);
					response.addProfanityIndiaMapAnalysis(indiaAnalysisMap);
				}
				}
			} catch (IOException e) {
			log.error("Error occured : {}", e);
		}
	}

	private void enrichTotalPages(String contentId, String fileName, int size) {
		try {
			PdfDocValidationResponse pdfResponse = pdfRepo.findProgressByContentIdAndPdfFileName(contentId, fileName);
			pdfResponse.setTotalPages(size);
			pdfRepo.save(pdfResponse);
		} catch (Exception e) {
			log.error("Exception occurred while reading the pdf", e);
		}
	}

	public Map<String, Object> getProfanityCheckForImage(String fileName, File imageFile) {
		StringBuilder url = new StringBuilder();
		url.append(configuration.getProfanityImageServiceHost()).append(configuration.getProfanityImageServicePath());
		Map<String, Object> response = outboundRequestHandlerService.fetchResultsForImages(url.toString(), imageFile, fileName);
		try {
			log.info(mapper.writeValueAsString(mapper.convertValue(response, Map.class)));
		} catch (JsonProcessingException e) {
			log.error(e);
		}
		return mapper.convertValue(response, Map.class);
	}
}
