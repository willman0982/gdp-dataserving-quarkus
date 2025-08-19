package com.sc.gdp.common.s3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S3 Service for S3a protocol operations on Isilon storage.
 * Provides comprehensive S3 operations including upload, download, 
 * signed URL generation, and file listing.
 */
@ApplicationScoped
public class S3Service {

    private AmazonS3 s3Client;
    
    @Inject
    S3Config s3Config;

    @jakarta.annotation.PostConstruct
    void init() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
            s3Config.accessKey(), 
            s3Config.secretKey()
        );
        
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
            
        if (s3Config.endpointUrl() != null && !s3Config.endpointUrl().isEmpty()) {
            clientBuilder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(s3Config.endpointUrl(), s3Config.region())
            ).withPathStyleAccessEnabled(s3Config.pathStyleAccess());
        } else {
            clientBuilder.withRegion(s3Config.region());
        }
        
        this.s3Client = clientBuilder.build();
    }

    /**
     * Upload a file to S3
     * @param key The S3 object key (file path)
     * @param filePath Local file path to upload
     * @return The S3 object URL
     * @throws S3Exception if upload fails
     */
    public String uploadFile(String key, Path filePath) throws S3Exception {
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                s3Config.bucketName(),
                key,
                filePath.toFile()
            ).withMetadata(metadata);

            s3Client.putObject(putObjectRequest);
            
            return buildObjectUrl(key);
        } catch (Exception e) {
            throw new S3Exception("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Upload a file to S3 from InputStream
     * @param key The S3 object key (file path)
     * @param inputStream Input stream of the file content
     * @param contentLength Content length in bytes
     * @param contentType MIME type of the content
     * @return The S3 object URL
     * @throws S3Exception if upload fails
     */
    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) throws S3Exception {
        try {
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                s3Config.bucketName(),
                key,
                inputStream,
                metadata
            );

            s3Client.putObject(putObjectRequest);
            
            return buildObjectUrl(key);
        } catch (Exception e) {
            throw new S3Exception("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from S3
     * @param key The S3 object key (file path)
     * @return S3Object containing the file content
     * @throws S3Exception if download fails
     */
    public S3Object downloadFile(String key) throws S3Exception {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(
                s3Config.bucketName(),
                key
            );

            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from S3 and save to local path
     * @param key The S3 object key (file path)
     * @param localPath Local path to save the downloaded file
     * @throws S3Exception if download fails
     */
    public void downloadFile(String key, Path localPath) throws S3Exception {
        try (S3Object s3Object = downloadFile(key)) {
            Files.copy(s3Object.getObjectContent(), localPath);
        } catch (IOException e) {
            throw new S3Exception("Failed to save downloaded file: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a pre-signed URL for downloading a file from S3
     * @param key The S3 object key (file path)
     * @return Pre-signed URL for downloading
     * @throws S3Exception if URL generation fails
     */
    public URL generateDownloadSignedUrl(String key) throws S3Exception {
        return generateDownloadSignedUrl(key, Duration.ofMinutes(s3Config.signedUrlDurationMinutes()));
    }

    /**
     * Generate a pre-signed URL for downloading a file from S3
     * @param key The S3 object key (file path)
     * @param duration Duration for which the URL is valid
     * @return Pre-signed URL for downloading
     * @throws S3Exception if URL generation fails
     */
    public URL generateDownloadSignedUrl(String key, Duration duration) throws S3Exception {
        try {
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += duration.toMillis();
            expiration.setTime(expTimeMillis);
            
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                s3Config.bucketName(),
                key
            ).withMethod(HttpMethod.GET)
             .withExpiration(expiration);
            
            return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to generate download signed URL: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a pre-signed URL for uploading a file to S3
     * @param key The S3 object key (file path)
     * @param contentType MIME type of the content to be uploaded
     * @return Pre-signed URL for uploading
     * @throws S3Exception if URL generation fails
     */
    public URL generateUploadSignedUrl(String key, String contentType) throws S3Exception {
        return generateUploadSignedUrl(key, contentType, Duration.ofMinutes(s3Config.signedUrlDurationMinutes()));
    }

    /**
     * Generate a pre-signed URL for uploading a file to S3
     * @param key The S3 object key (file path)
     * @param contentType MIME type of the content to be uploaded
     * @param duration Duration for which the URL is valid
     * @return Pre-signed URL for uploading
     * @throws S3Exception if URL generation fails
     */
    public URL generateUploadSignedUrl(String key, String contentType, Duration duration) throws S3Exception {
        try {
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += duration.toMillis();
            expiration.setTime(expTimeMillis);
            
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                s3Config.bucketName(),
                key
            ).withMethod(HttpMethod.PUT)
             .withExpiration(expiration)
             .withContentType(contentType);
            
            return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to generate upload signed URL: " + e.getMessage(), e);
        }
    }

    /**
     * List files in S3 bucket
     * @return List of S3FileInfo objects
     * @throws S3Exception if listing fails
     */
    public List<S3FileInfo> listFiles() throws S3Exception {
        return listFiles(null);
    }

    /**
     * List files in S3 bucket with prefix
     * @param prefix Prefix to filter objects (can be null for all objects)
     * @return List of S3FileInfo objects
     * @throws S3Exception if listing fails
     */
    public List<S3FileInfo> listFiles(String prefix) throws S3Exception {
        try {
            ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(s3Config.bucketName());
            
            if (prefix != null && !prefix.isEmpty()) {
                request.withPrefix(prefix);
            }

            ObjectListing response = s3Client.listObjects(request);
            
            return response.getObjectSummaries().stream()
                .map(s3Object -> S3Utils.createS3FileInfo(
                    s3Object.getKey(),
                    s3Object.getETag(),
                    s3Object.getSize(),
                    s3Object.getLastModified().toInstant(),
                    s3Object.getStorageClass()
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new S3Exception("Failed to list files from S3: " + e.getMessage(), e);
        }
    }

    /**
     * List file keys in S3 bucket
     * @return List of S3 object keys
     * @throws S3Exception if listing fails
     */
    public List<String> listFileKeys() throws S3Exception {
        return listFileKeys(null);
    }

    /**
     * List file keys in S3 bucket with prefix
     * @param prefix Prefix to filter objects (can be null for all objects)
     * @return List of S3 object keys
     * @throws S3Exception if listing fails
     */
    public List<String> listFileKeys(String prefix) throws S3Exception {
        return listFiles(prefix).stream()
                .map(S3FileInfo::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Delete a file from S3
     * @param key The S3 object key (file path)
     * @throws S3Exception if deletion fails
     */
    public void deleteFile(String key) throws S3Exception {
        try {
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(
                    s3Config.bucketName(),
                    key
            );

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a file exists in S3
     * @param key The S3 object key (file path)
     * @return true if file exists, false otherwise
     * @throws S3Exception if check fails
     */
    public boolean fileExists(String key) throws S3Exception {
        try {
            GetObjectMetadataRequest headObjectRequest = new GetObjectMetadataRequest(
                    s3Config.bucketName(),
                    key
            );

            s3Client.getObjectMetadata(headObjectRequest);
            return true;
        } catch (AmazonServiceException e) {
             if (e.getStatusCode() == 404) {
                 return false;
             }
             throw new S3Exception("Failed to check file existence in S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new S3Exception("Failed to check file existence in S3: " + e.getMessage(), e);
        }
    }

    /**
     * Get file metadata from S3
     * @param key The S3 object key (file path)
     * @return HeadObjectResponse containing file metadata
     * @throws S3Exception if metadata retrieval fails
     */
    public ObjectMetadata getFileMetadata(String key) throws S3Exception {
        try {
            GetObjectMetadataRequest headObjectRequest = new GetObjectMetadataRequest(
                    s3Config.bucketName(),
                    key
            );

            return s3Client.getObjectMetadata(headObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to get file metadata from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Copy a file within S3
     * @param sourceKey Source object key
     * @param destinationKey Destination object key
     * @throws S3Exception if copy fails
     */
    public void copyFile(String sourceKey, String destinationKey) throws S3Exception {
        try {
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
                    s3Config.bucketName(),
                    sourceKey,
                    s3Config.bucketName(),
                    destinationKey
            );

            s3Client.copyObject(copyObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to copy file in S3: " + e.getMessage(), e);
        }
    }

    /**
     * Get the size of a file in S3
     * @param key The S3 object key (file path)
     * @return File size in bytes
     * @throws S3Exception if size retrieval fails
     */
    public long getFileSize(String key) throws S3Exception {
        ObjectMetadata metadata = getFileMetadata(key);
        return metadata.getContentLength();
    }

    /**
     * Build object URL for the given key
     * @param key The S3 object key
     * @return Object URL
     */
    private String buildObjectUrl(String key) {
        if (s3Config.endpointUrl() != null && !s3Config.endpointUrl().isEmpty()) {
            return String.format("%s/%s/%s", s3Config.endpointUrl(), s3Config.bucketName(), key);
        } else {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", s3Config.bucketName(), s3Config.region(), key);
        }
    }

    @jakarta.annotation.PreDestroy
    void cleanup() {
        if (s3Client != null) {
            s3Client.shutdown();
        }
    }
}