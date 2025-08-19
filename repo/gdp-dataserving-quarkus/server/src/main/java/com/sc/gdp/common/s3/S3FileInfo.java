package com.sc.gdp.common.s3;

import java.time.Instant;
import java.util.Map;

/**
 * Data transfer object for S3 file information
 */
public class S3FileInfo {
    private String key;
    private String etag;
    private long size;
    private Instant lastModified;
    private String storageClass;
    private String contentType;
    private Map<String, String> metadata;
    private String url;

    public S3FileInfo() {}

    public S3FileInfo(String key, String etag, long size, Instant lastModified) {
        this.key = key;
        this.etag = etag;
        this.size = size;
        this.lastModified = lastModified;
    }

    // Getters and setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "S3FileInfo{" +
                "key='" + key + '\'' +
                ", etag='" + etag + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                ", storageClass='" + storageClass + '\'' +
                ", contentType='" + contentType + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}