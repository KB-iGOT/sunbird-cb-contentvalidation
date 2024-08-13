package org.sunbird.contentvalidation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.sunbird.contentvalidation.config.Configuration;
import org.sunbird.contentvalidation.config.Constants;
import org.sunbird.contentvalidation.model.HierarchyResponse;
import org.sunbird.contentvalidation.service.ContentProviderService;
import org.sunbird.contentvalidation.service.StorageService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.NoSuchElementException;

@Service
public class ContentProviderServiceImpl implements ContentProviderService {

    @Autowired
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    private Configuration configuration;

    @Autowired
    private ObjectMapper mapper;

    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private StorageService storageService;


    @Override
    public InputStream getContentFile(String downloadUrl) {
        if (downloadUrl.contains(Constants.DOWNLOAD_URL_PREFIX)) {
            downloadUrl = downloadUrl.replace(Constants.DOWNLOAD_URL_PREFIX, configuration.getContentServiceHost());
        }
        byte[] byteStream = outboundRequestHandlerService.fetchByteStream(getSignedUrl(downloadUrl));
        return new ByteArrayInputStream(byteStream);
    }

    @Override
    public HierarchyResponse getHeirarchyResponse(String rootOrg, String org, String contentId, String userId) {
        StringBuilder url = new StringBuilder();
        url.append(configuration.getLexCoreServiceHost()).append(configuration.getHeirarchySearchPath().replace(Constants.CONTENT_ID_REPLACER, contentId));
        HashMap<String, Object> request = new HashMap<>();
        request.put(Constants.ROOT_ORG_CONSTANT, rootOrg);
        request.put(Constants.ORG_CONSTANT, org);
        request.put(Constants.USER_ID_CONSTANT, userId);
        request.put(Constants.FIELD_PASSED_CONSTANT, Constants.FIELDS_PASSED);
        request.put(Constants.FETCH_ONE_LEVEL_CONSTANT, Constants.FETCH_ON_LEVEL);
        request.put(Constants.SKIP_ACCESS_CHECK_CONSTANT, Constants.SKIP_ACCESS_CHECK);
        request.put(Constants.FIELDS_CONSTANT, Constants.MINIMUL_FIELDS);
        Object recieviedResponse = outboundRequestHandlerService.fetchResultUsingPost(url.toString(), request);
        HierarchyResponse response = mapper.convertValue(recieviedResponse, HierarchyResponse.class);
        if (ObjectUtils.isEmpty(response) || StringUtils.isEmpty(response.getDownloadUrl())) {
            throw new NoSuchElementException();
        }
        return response;
    }

    private String getSignedUrl(String downloadUrl) {
        logger.info("downloadFile Url: " + downloadUrl);
        if (downloadUrl.startsWith("http")) {
            try {
                String uri = StringUtils.substringAfter(new URL(downloadUrl).getPath(), "/");
                String container = StringUtils.substringBefore(uri, "/");
                String relativePath = StringUtils.substringAfter(uri, "/");
                logger.info("Got filePath with relative path: " + relativePath);
                String downloadPath = storageService.getSignedUrl(container, relativePath, 30);
                logger.info("The download path: " + downloadPath);
                return downloadPath;
            } catch (MalformedURLException e) {
                logger.error("url is not proper{}", downloadUrl, e);
                throw new RuntimeException(e);
            }
        } else {
            return downloadUrl;
        }
    }

    @Override
    public InputStream getContentFileV2(String downloadUrl) {
        String fileName = null;
        try {
            String uri = null;
            uri = StringUtils.substringAfter(new URL(downloadUrl).getPath(), "/");
            String filePath = StringUtils.substringAfter(uri, "/");
            logger.info("subContainerName: " + filePath);
            String subContainerName = StringUtils.substringBefore(filePath, "/");
            logger.info("subContainerName: " + subContainerName);
            String fileNamePath = StringUtils.substringAfter(filePath, "/");
            logger.info("The download path: " + fileNamePath);
            storageService.downloadFile(fileNamePath, subContainerName);
            fileName = StringUtils.substringAfterLast(filePath, "/");
            logger.info("The fileName: " + fileName);
            Path tmpPath = Paths.get(Constants.LOCAL_BASE_PATH + fileName);
            return Files.newInputStream(tmpPath);
        } catch (Exception e) {
            logger.error("Error while processing request and not able to process file", e);
            throw new RuntimeException(e);
        } finally {
            try {
                File file = new File(Constants.LOCAL_BASE_PATH + fileName);
                if (file.exists()) {
                    logger.info("The fileName tmp path is: " + file.getAbsolutePath());
                    file.delete();
                }
            } catch (Exception e1) {
            }
        }
    }
}
