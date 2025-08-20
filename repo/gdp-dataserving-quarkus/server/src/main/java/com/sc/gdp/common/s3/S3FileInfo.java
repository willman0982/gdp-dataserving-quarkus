package com.sc.gdp.common.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Data transfer object for S3 file information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3FileInfo {
    private String key;
    private String etag;
    private long size;
    private Instant lastModified;
    private String storageClass;
    private String contentType;
    private Map<String, String> metadata;
    private String url;

    public S3FileInfo(String key, String etag, long size, Instant lastModified) {
        this.key = key;
        this.etag = etag;
        this.size = size;
        this.lastModified = lastModified;
    }


}