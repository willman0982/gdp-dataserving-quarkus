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
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * S3 Service for S3a protocol operations on Isilon storage.
 * Provides comprehensive S3 operations including upload, download, 
 * signed URL generation, and file listing.
 * Supports multiple S3 buckets with different configurations.
 * 
 * <p><strong>Important:</strong> All methods require a bucketName parameter.
 * Default bucket operations have been removed to ensure explicit bucket specification.
 * Download presigned URLs include file existence validation before generation.
 */
@ApplicationScoped
public class S3Service {

    private Map<String, String> bucketNameToId = new ConcurrentHashMap<>();
    
    @Inject
    S3Config s3Config;

    @jakarta.annotation.PostConstruct
    void init() {
        // Initialize bucket name mappings
        if (s3Config.buckets() != null && !s3Config.buckets().isEmpty()) {
            s3Config.buckets().forEach((bucketId, bucketConfig) -> {
                // Store bucket name mapping
                bucketNameToId.put(bucketConfig.bucketName(), bucketId);
            });
        }
    }
    

    
    /**
     * Get the S3 client for a specific bucket
     * 
     * @param bucketName The bucket name (required)
     * @return The AmazonS3 client for the specified bucket
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public AmazonS3 getS3Client(String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        
        // Get bucket configuration
        String bucketId = bucketNameToId.get(bucketName);
        if (bucketId == null) {
            throw new IllegalArgumentException("No configuration found for bucket: " + bucketName);
        }
        
        S3Config.BucketConfig bucketConfig = s3Config.buckets().get(bucketId);
        if (bucketConfig == null) {
            throw new IllegalArgumentException("No bucket configuration found for bucketId: " + bucketId);
        }
        
        // Create S3 client dynamically
        BasicAWSCredentials credentials = null;
        if (bucketConfig.accessKey().isPresent() && bucketConfig.secretKey().isPresent()) {
            credentials = new BasicAWSCredentials(
                bucketConfig.accessKey().get(),
                bucketConfig.secretKey().get()
            );
        }
        
        if (credentials == null) {
            throw new IllegalArgumentException("No valid credentials found for bucket: " + bucketName);
        }
        
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials));
        
        // Configure endpoint - use bucket-specific if provided
        if (bucketConfig.endpointUrl().isPresent()) {
            clientBuilder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                    bucketConfig.endpointUrl().get(),
                    bucketConfig.region()
                )
            );
            clientBuilder.withPathStyleAccessEnabled(bucketConfig.pathStyleAccess());
        } else {
            clientBuilder.withRegion(bucketConfig.region());
        }
        
        return clientBuilder.build();
    }
    
    /**
     * Get the bucket ID for a specific bucket name (for internal use)
     * 
     * @param bucketName The bucket name (required)
     * @return The bucket ID
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public String getBucketId(String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        // First try to find the bucket ID by name
        String bucketId = bucketNameToId.get(bucketName);
        if (bucketId != null) {
            return bucketId;
        }
        // If not found, try to get the default bucket ID
        if (s3Config.buckets().containsKey("default")) {
            return "default";
        }
        // If no 'default' bucket, return the first configured bucket ID
        return s3Config.buckets().keySet().iterator().next();
    }
    
    /**
     * Get all configured bucket names
     * 
     * @return List of bucket names
     */
    public List<String> getBucketNames() {
        return List.copyOf(bucketNameToId.keySet());
    }





    /**
     * Upload a file to S3 from Path with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @param filePath Path to the local file
     * @return The S3 object URL
     * @throws S3Exception if upload fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public String uploadFile(String bucketName, String key, Path filePath) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(Files.size(filePath));
            metadata.setContentType(contentType);
            
            AmazonS3 s3Client = getS3Client(bucketName);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                key,
                filePath.toFile()
            );
            putObjectRequest.setMetadata(metadata);
            
            s3Client.putObject(putObjectRequest);
            
            return buildObjectUrl(bucketName, key);
        } catch (Exception e) {
            throw new S3Exception("Failed to upload file to S3 bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Upload a file to S3 from InputStream with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @param inputStream Input stream of the file content
     * @param contentLength Content length in bytes
     * @param contentType MIME type of the content
     * @return The S3 object URL
     * @throws S3Exception if upload fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public String uploadFile(String bucketName, String key, InputStream inputStream, long contentLength, String contentType) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType);
            
            AmazonS3 s3Client = getS3Client(bucketName);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                key,
                inputStream,
                metadata
            );
            
            s3Client.putObject(putObjectRequest);
            
            return buildObjectUrl(bucketName, key);
        } catch (Exception e) {
            throw new S3Exception("Failed to upload file to S3 bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }





    /**
     * Download a file from S3 with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @return S3Object containing the file content
     * @throws S3Exception if download fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public S3Object downloadFile(String bucketName, String key) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            AmazonS3 s3Client = getS3Client(bucketName);
            
            GetObjectRequest getObjectRequest = new GetObjectRequest(
                bucketName,
                key
            );
            
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to download file from S3 bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from S3 and save to local path with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @param localPath Local path to save the downloaded file
     * @throws S3Exception if download fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public void downloadFile(String bucketName, String key, Path localPath) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try (S3Object s3Object = downloadFile(bucketName, key)) {
            Files.copy(s3Object.getObjectContent(), localPath);
        } catch (IOException e) {
            throw new S3Exception("Failed to save downloaded file from bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Generate a pre-signed URL for downloading a file from S3
     * @param key The S3 object key (file path)
     * @return Pre-signed URL for downloading
     * @throws S3Exception if URL generation fails
     */

    
    /**
     * Generate a pre-signed URL for downloading a file from S3
     * @param bucketName The bucket name to use (required)
     * @param key The S3 object key (file path)
     * @param duration Duration for which the URL is valid
     * @return Pre-signed URL for downloading
     * @throws S3Exception if URL generation fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public URL generateDownloadSignedUrl(String bucketName, String key, Duration duration) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            // Check if file exists before generating presigned URL
            if (!fileExists(bucketName, key)) {
                throw new S3Exception("File does not exist: " + key + " in bucket " + bucketName);
            }
            
            AmazonS3 client = getS3Client(bucketName);
            
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += duration.toMillis();
            expiration.setTime(expTimeMillis);
            
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                bucketName, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
            
            return client.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to generate download signed URL for bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }


    
    /**
     * Generate a pre-signed URL for uploading a file to S3
     * @param bucketName The bucket name to use (required)
     * @param key The S3 object key (file path)
     * @param contentType MIME type of the content to be uploaded
     * @param duration Duration for which the URL is valid
     * @return Pre-signed URL for uploading
     * @throws S3Exception if URL generation fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public URL generateUploadSignedUrl(String bucketName, String key, String contentType, Duration duration) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            AmazonS3 client = getS3Client(bucketName);
            
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += duration.toMillis();
            expiration.setTime(expTimeMillis);
            
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                bucketName, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);
            
            return client.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to generate upload signed URL for bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }





    /**
     * List all files in a specific S3 bucket
     * @param bucketName The bucket name (required)
     * @return List of S3FileInfo objects
     * @throws S3Exception if listing fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public List<S3FileInfo> listFilesInBucket(String bucketName) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        return listFilesInBucket(bucketName, null);
    }

    /**
     * List files in a specific S3 bucket with a prefix
     * @param bucketName The bucket name (required)
     * @param prefix Prefix to filter files (can be null)
     * @return List of S3FileInfo objects
     * @throws S3Exception if listing fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public List<S3FileInfo> listFilesInBucket(String bucketName, String prefix) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            AmazonS3 s3Client = getS3Client(bucketName);
            
            ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withMaxKeys(1000);
            
            if (prefix != null && !prefix.isEmpty()) {
                request.withPrefix(prefix);
            }
            
            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            
            return result.getObjectSummaries().stream()
                .map(summary -> new S3FileInfo(
                    summary.getKey(),
                    summary.getETag(),
                    summary.getSize(),
                    summary.getLastModified().toInstant()
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new S3Exception("Failed to list files in bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    /**
     * List all file keys in a specific S3 bucket
     * @param bucketName The bucket name (required)
     * @return List of file keys
     * @throws S3Exception if listing fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public List<String> listFileKeysInBucket(String bucketName) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        return listFileKeysInBucket(bucketName, null);
    }

    /**
     * List file keys in a specific S3 bucket with a prefix
     * @param bucketName The bucket name (required)
     * @param prefix Prefix to filter files (can be null)
     * @return List of file keys
     * @throws S3Exception if listing fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public List<String> listFileKeysInBucket(String bucketName, String prefix) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        return listFilesInBucket(bucketName, prefix).stream()
            .map(S3FileInfo::getKey)
            .collect(Collectors.toList());
    }



    /**
     * Delete a file from S3 with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @throws S3Exception if deletion fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public void deleteFile(String bucketName, String key) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            if (!S3Utils.isValidS3Key(key)) {
                throw new S3Exception("Invalid S3 object key format: " + key);
            }
            
            AmazonS3 s3Client = getS3Client(bucketName);
            
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(
                bucketName,
                key
            );
            
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to delete file from S3 bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }



    /**
     * Check if a file exists in S3 with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @return true if file exists, false otherwise
     * @throws S3Exception if check fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public boolean fileExists(String bucketName, String key) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            if (!S3Utils.isValidS3Key(key)) {
                throw new S3Exception("Invalid S3 object key format: " + key);
            }
            
            AmazonS3 s3Client = getS3Client(bucketName);
            
            return s3Client.doesObjectExist(bucketName, key);
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw new S3Exception("Failed to check file existence in bucket " + bucketName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new S3Exception("Failed to check file existence in bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }



    /**
     * Get file metadata from S3 with specific bucket
     * @param bucketName The bucket name (required)
     * @param key The S3 object key (file path)
     * @return ObjectMetadata containing file information
     * @throws S3Exception if metadata retrieval fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public ObjectMetadata getFileMetadata(String bucketName, String key) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        try {
            if (!S3Utils.isValidS3Key(key)) {
                throw new S3Exception("Invalid S3 object key format: " + key);
            }
            
            AmazonS3 s3Client = getS3Client(bucketName);
            
            GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(
                bucketName,
                key
            );
            
            return s3Client.getObjectMetadata(getObjectMetadataRequest);
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                throw new S3Exception("File not found: " + key + " in bucket " + bucketName, e);
            }
            throw new S3Exception("Failed to get file metadata from bucket " + bucketName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new S3Exception("Failed to get file metadata from bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Copy a file within a specific bucket
     * @param bucketName The bucket name (required)
     * @param sourceKey Source S3 object key
     * @param destinationKey Destination S3 object key
     * @throws S3Exception if copy fails
     * @throws IllegalArgumentException if bucketName is null or empty
     */
    public void copyFile(String bucketName, String sourceKey, String destinationKey) throws S3Exception {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("bucketName is required and cannot be null or empty");
        }
        copyFile(bucketName, sourceKey, bucketName, destinationKey);
    }

    /**
     * Copy a file between buckets
     * @param sourceBucketName Source bucket name (required)
     * @param sourceKey Source S3 object key
     * @param destinationBucketName Destination bucket name (required)
     * @param destinationKey Destination S3 object key
     * @throws S3Exception if copy fails
     * @throws IllegalArgumentException if sourceBucketName or destinationBucketName is null or empty
     */
    public void copyFile(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws S3Exception {
        if (sourceBucketName == null || sourceBucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceBucketName is required and cannot be null or empty");
        }
        if (destinationBucketName == null || destinationBucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("destinationBucketName is required and cannot be null or empty");
        }
        try {
            if (!S3Utils.isValidS3Key(sourceKey) || !S3Utils.isValidS3Key(destinationKey)) {
                throw new S3Exception("Invalid S3 object key format");
            }
            
            AmazonS3 s3Client = getS3Client(sourceBucketName);
            
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
                sourceBucketName, sourceKey,
                destinationBucketName, destinationKey
            );
            
            s3Client.copyObject(copyObjectRequest);
        } catch (Exception e) {
            throw new S3Exception("Failed to copy file from " + sourceBucketName + "/" + sourceKey + 
                " to " + destinationBucketName + "/" + destinationKey + ": " + e.getMessage(), e);
        }
    }





    private String buildObjectUrl(String bucketName, String key) {
        // Get bucket configuration
        String bucketId = getBucketId(bucketName);
        if (bucketId == null) {
            throw new IllegalArgumentException("No bucket configuration found for bucket: " + bucketName);
        }
        
        if (s3Config.buckets() == null || !s3Config.buckets().containsKey(bucketId)) {
            throw new IllegalArgumentException("No bucket configuration found for bucketId: " + bucketId);
        }
        
        var bucketConfig = s3Config.buckets().get(bucketId);
        if (bucketConfig.endpointUrl().isPresent()) {
            return bucketConfig.endpointUrl().get() + "/" + bucketName + "/" + key;
        }
        
        // Fallback to default AWS S3 URL format
        return "https://s3.us-east-1.amazonaws.com/" + bucketName + "/" + key;
    }
}