package org.sunbird.contentvalidation.service;

public interface StorageService {
    public boolean downloadFile(String fileName, String containerName);

    String getSignedUrl(String container, String path, int ttl);
}
