package org.sunbird.contentvalidation.service.impl;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.contentvalidation.config.Configuration;
import org.sunbird.contentvalidation.config.Constants;
import org.sunbird.contentvalidation.service.StorageService;

import scala.Option;

@Service
public class StorageServiceImpl implements StorageService {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private Configuration configuration;

    private BaseStorageService storageService = null;

    @PostConstruct
    public void init() {
        if (storageService == null) {
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(
                    configuration.getCloudStorageTypeName(), configuration.getCloudStorageKey(),
                    configuration.getCloudStorageSecret().replace("\\n", "\n"),
                    Option.apply(configuration.getCloudStorageEndpoint()), Option.empty()));
        }
    }

    @Override
    public boolean downloadFile(String fileName, String containerName) {
        try {
            String objectKey = containerName + "/" + fileName;
            storageService.download(configuration.getCloudContainerName(), objectKey, Constants.LOCAL_BASE_PATH,
                    Option.apply(Boolean.FALSE));
            return true;
        } catch (Exception e) {
            logger.error("Failed to download the file: " + fileName + ", Exception: ", e);
            return false;
        }
    }

}
