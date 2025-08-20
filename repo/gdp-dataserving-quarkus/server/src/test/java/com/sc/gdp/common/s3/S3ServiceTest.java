package com.sc.gdp.common.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

    @InjectMocks
    private S3Service s3Service;

    @Mock
    private AmazonS3 mockS3Client;

    @Mock
    private S3Config mockS3Config;

    @Mock
    private S3Config.BucketConfig mockBucketConfig;

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_KEY = "test/file.txt";
    private static final String TEST_BUCKET_NAME = "test-bucket-name";
    private static final String TEST_CONTENT = "test content";
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Mock S3Config with bucket-based configuration
        lenient().when(mockBucketConfig.bucketName()).thenReturn(TEST_BUCKET);
        lenient().when(mockBucketConfig.endpointUrl()).thenReturn(java.util.Optional.of("http://localhost:9000"));
        lenient().when(mockBucketConfig.accessKey()).thenReturn(java.util.Optional.of("test-access-key"));
        lenient().when(mockBucketConfig.secretKey()).thenReturn(java.util.Optional.of("test-secret-key"));
        lenient().when(mockBucketConfig.region()).thenReturn("us-east-1");
        lenient().when(mockBucketConfig.pathStyleAccess()).thenReturn(true);
        lenient().when(mockBucketConfig.signedUrlDurationMinutes()).thenReturn(60);
        lenient().when(mockBucketConfig.connectionTimeoutMs()).thenReturn(30000);
        lenient().when(mockBucketConfig.socketTimeoutMs()).thenReturn(30000);
        lenient().when(mockBucketConfig.maxRetryAttempts()).thenReturn(3);
        
        // Mock the buckets map to return the bucket config for all test bucket names
        java.util.Map<String, S3Config.BucketConfig> bucketsMap = java.util.Map.of(
            "gdp", mockBucketConfig,
            TEST_BUCKET, mockBucketConfig,
            TEST_BUCKET_NAME, mockBucketConfig
        );
        lenient().when(mockS3Config.buckets()).thenReturn(bucketsMap);
        
        // Note: With the refactored implementation, S3 clients are created dynamically
        // No need to inject a default S3 client as it no longer exists
        
        // Mock the bucketNameToId map to ensure bucket names are recognized
        java.lang.reflect.Field bucketNameToIdField = S3Service.class.getDeclaredField("bucketNameToId");
        bucketNameToIdField.setAccessible(true);
        java.util.Map<String, String> bucketNameToIdMap = new java.util.concurrent.ConcurrentHashMap<>();
        bucketNameToIdMap.put(TEST_BUCKET, "gdp");
        bucketNameToIdMap.put(TEST_BUCKET_NAME, "gdp");
        bucketNameToIdMap.put("gdp", "gdp");
        bucketNameToIdField.set(s3Service, bucketNameToIdMap);
    }

    @Test
    void testUploadFileWithPath() throws Exception {
        // Given
        Path testPath = tempDir.resolve("test-file.txt");
        Files.write(testPath, TEST_CONTENT.getBytes());
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_BUCKET_NAME, TEST_KEY, testPath);
            assertNotNull(result);
        });

        verify(mockS3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    void testUploadFileWithInputStream() throws Exception {
        // Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        long contentLength = TEST_CONTENT.length();
        String contentType = "text/plain";
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_BUCKET_NAME, TEST_KEY, inputStream, contentLength, contentType);
            assertNotNull(result);
        });

        verify(mockS3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    void testUploadFileWithBucketName() throws Exception {
        // Given
        Path testPath = tempDir.resolve("test-file.txt");
        Files.write(testPath, TEST_CONTENT.getBytes());
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_BUCKET_NAME, TEST_KEY, testPath);
            assertNotNull(result);
        });
    }

    @Test
    void testUploadFileWithBucketNameAndInputStream() throws Exception {
        // Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        long contentLength = TEST_CONTENT.length();
        String contentType = "text/plain";
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_BUCKET_NAME, TEST_KEY, inputStream, contentLength, contentType);
            assertNotNull(result);
        });
    }

    @Test
    void testDownloadFile() throws Exception {
        // Given
        S3Object s3Object = new S3Object();
        s3Object.setKey(TEST_KEY);
        s3Object.setObjectContent(new ByteArrayInputStream(TEST_CONTENT.getBytes()));
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

        // When
        S3Object result = s3Service.downloadFile(TEST_BUCKET_NAME, TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(TEST_KEY, result.getKey());
        verify(mockS3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testDownloadFileWithBucketName() throws Exception {
        // Given
        S3Object s3Object = new S3Object();
        s3Object.setKey(TEST_KEY);
        s3Object.setObjectContent(new ByteArrayInputStream(TEST_CONTENT.getBytes()));
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

        // When
        S3Object result = s3Service.downloadFile(TEST_BUCKET_NAME, TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(TEST_KEY, result.getKey());
    }

    @Test
    void testListFiles() throws Exception {
        // Given
        ListObjectsV2Result result = new ListObjectsV2Result();
        S3ObjectSummary summary1 = new S3ObjectSummary();
        summary1.setKey("file1.txt");
        summary1.setETag("etag1");
        summary1.setSize(100L);
        summary1.setLastModified(new Date());
        summary1.setStorageClass("STANDARD");

        S3ObjectSummary summary2 = new S3ObjectSummary();
        summary2.setKey("file2.txt");
        summary2.setETag("etag2");
        summary2.setSize(200L);
        summary2.setLastModified(new Date());
        summary2.setStorageClass("STANDARD");

        result.getObjectSummaries().addAll(Arrays.asList(summary1, summary2));
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);

        // When
        List<S3FileInfo> listResult = s3Service.listFilesInBucket(TEST_BUCKET_NAME);

        // Then
        assertNotNull(listResult);
        assertEquals(2, listResult.size());
        assertEquals("file1.txt", listResult.get(0).getKey());
        assertEquals("file2.txt", listResult.get(1).getKey());
        verify(mockS3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void testListFilesWithPrefix() throws Exception {
        // Given
        String prefix = "documents/";
        ListObjectsV2Result result = new ListObjectsV2Result();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("documents/file.txt");
        summary.setETag("etag");
        summary.setSize(100L);
        summary.setLastModified(new Date());
        summary.setStorageClass("STANDARD");
        result.getObjectSummaries().add(summary);
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);

        // When
        List<S3FileInfo> listResult = s3Service.listFilesInBucket(TEST_BUCKET_NAME, prefix);

        // Then
        assertNotNull(listResult);
        assertEquals(1, listResult.size());
        assertEquals("documents/file.txt", listResult.get(0).getKey());
        verify(mockS3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void testListFilesInBucket() throws Exception {
        // Given
        ListObjectsV2Result result = new ListObjectsV2Result();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("bucket-file.txt");
        summary.setETag("etag");
        summary.setSize(100L);
        summary.setLastModified(new Date());
        summary.setStorageClass("STANDARD");
        result.getObjectSummaries().add(summary);
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);

        // When
        List<S3FileInfo> listResult = s3Service.listFilesInBucket(TEST_BUCKET_NAME);

        // Then
        assertNotNull(listResult);
        assertEquals(1, listResult.size());
        assertEquals("bucket-file.txt", listResult.get(0).getKey());
    }

    @Test
    void testListFileKeys() throws Exception {
        // Given
        ListObjectsV2Result result = new ListObjectsV2Result();
        S3ObjectSummary summary1 = new S3ObjectSummary();
        summary1.setKey("key1.txt");
        summary1.setETag("etag1");
        summary1.setSize(100L);
        summary1.setLastModified(new Date());
        summary1.setStorageClass("STANDARD");

        S3ObjectSummary summary2 = new S3ObjectSummary();
        summary2.setKey("key2.txt");
        summary2.setETag("etag2");
        summary2.setSize(200L);
        summary2.setLastModified(new Date());
        summary2.setStorageClass("STANDARD");

        result.getObjectSummaries().addAll(Arrays.asList(summary1, summary2));
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);

        // When
        List<String> listResult = s3Service.listFileKeysInBucket(TEST_BUCKET_NAME);

        // Then
        assertNotNull(listResult);
        assertEquals(2, listResult.size());
        assertEquals("key1.txt", listResult.get(0));
        assertEquals("key2.txt", listResult.get(1));
        verify(mockS3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void testListFileKeysInBucket() throws Exception {
        // Given
        ListObjectsV2Result result = new ListObjectsV2Result();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("bucket-file.txt");
        summary.setETag("etag");
        summary.setSize(100L);
        summary.setLastModified(new Date());
        summary.setStorageClass("STANDARD");
        result.getObjectSummaries().add(summary);
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);

        // When
        List<String> listResult = s3Service.listFileKeysInBucket(TEST_BUCKET_NAME);

        // Then
        assertNotNull(listResult);
        assertEquals(1, listResult.size());
        assertEquals("bucket-file.txt", listResult.get(0));
    }

    @Test
    void testDeleteFile() throws Exception {
        // Given
        doNothing().when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

        // When & Then
        assertDoesNotThrow(() -> s3Service.deleteFile(TEST_BUCKET_NAME, TEST_KEY));
        verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFileWithBucketName() throws Exception {
        // Given
        doNothing().when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

        // When & Then
        assertDoesNotThrow(() -> s3Service.deleteFile(TEST_BUCKET_NAME, TEST_KEY));
    }

    @Test
    void testFileExists() throws Exception {
        // Given
        when(mockS3Client.doesObjectExist(anyString(), anyString())).thenReturn(true);

        // When
        boolean result = s3Service.fileExists(TEST_BUCKET_NAME, TEST_KEY);

        // Then
        assertTrue(result);
        verify(mockS3Client).doesObjectExist(anyString(), anyString());
    }

    @Test
    void testFileExistsNotFound() throws Exception {
        // Given
        when(mockS3Client.doesObjectExist(anyString(), anyString())).thenReturn(false);

        // When
        boolean result = s3Service.fileExists(TEST_BUCKET_NAME, TEST_KEY);

        // Then
        assertFalse(result);
        verify(mockS3Client).doesObjectExist(anyString(), anyString());
    }

    @Test
    void testFileExistsWithBucketName() throws Exception {
        // Given
        when(mockS3Client.doesObjectExist(anyString(), anyString())).thenReturn(true);

        // When
        boolean result = s3Service.fileExists(TEST_BUCKET_NAME, TEST_KEY);

        // Then
        assertTrue(result);
        verify(mockS3Client).doesObjectExist(anyString(), anyString());
    }

    @Test
    void testGetFileMetadata() throws Exception {
        // Arrange
        ObjectMetadata mockMetadata = mock(ObjectMetadata.class);
        when(mockMetadata.getContentLength()).thenReturn(100L);
        when(mockMetadata.getContentType()).thenReturn("text/plain");
        
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(mockMetadata);
        
        // Act
        ObjectMetadata result = s3Service.getFileMetadata(TEST_BUCKET_NAME, TEST_KEY);
        
        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getContentLength());
        assertEquals("text/plain", result.getContentType());
        verify(mockS3Client).getObjectMetadata(any(GetObjectMetadataRequest.class));
    }

    @Test
    void testGetFileMetadataWithBucketName() throws Exception {
        // Given
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(200L);
        metadata.setContentType("application/pdf");
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

        // When
        ObjectMetadata result = s3Service.getFileMetadata(TEST_BUCKET_NAME, TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(200L, result.getContentLength());
        assertEquals("application/pdf", result.getContentType());
    }

    @Test
    void testCopyFile() throws Exception {
        // Given
        String sourceKey = "source/file.txt";
        String destinationKey = "destination/file.txt";
        CopyObjectResult copyResult = new CopyObjectResult();
        when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyResult);

        // When & Then
        assertDoesNotThrow(() -> s3Service.copyFile(TEST_BUCKET_NAME, sourceKey, destinationKey));
        verify(mockS3Client).copyObject(any(CopyObjectRequest.class));
    }

    @Test
    void testCopyFileWithBucketName() throws Exception {
        // Given
        String sourceKey = "source/file.txt";
        String destinationKey = "destination/file.txt";
        CopyObjectResult copyResult = new CopyObjectResult();
        when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyResult);

        // When & Then
        assertDoesNotThrow(() -> s3Service.copyFile(TEST_BUCKET_NAME, sourceKey, destinationKey));
    }

    @Test
    void testCopyFileBetweenBuckets() throws Exception {
        // Given
        String sourceBucketName = "source-bucket";
        String destinationBucketName = "destination-bucket";
        String sourceKey = "source/file.txt";
        String destinationKey = "destination/file.txt";
        CopyObjectResult copyResult = new CopyObjectResult();
        when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyResult);

        // When & Then
        assertDoesNotThrow(() -> s3Service.copyFile(sourceBucketName, sourceKey, destinationBucketName, destinationKey));
    }



    @Test
    void testUploadFileException() throws IOException {
        // Given
        Path testPath = tempDir.resolve("test-file.txt");
        Files.write(testPath, TEST_CONTENT.getBytes());
        lenient().when(mockS3Client.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Upload failed"));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.uploadFile(TEST_BUCKET_NAME, TEST_KEY, testPath);
        });
        assertTrue(exception.getMessage().contains("Failed to upload file to S3"));
    }

    @Test
    void testDownloadFileException() {
        // Given
        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("Download failed"));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.downloadFile(TEST_BUCKET_NAME, TEST_KEY);
        });
        assertTrue(exception.getMessage().contains("Failed to download file from S3"));
    }

    @Test
    void testListFilesException() {
        // Given
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(new RuntimeException("List failed"));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.listFilesInBucket(TEST_BUCKET_NAME);
        });
        assertTrue(exception.getMessage().contains("Failed to list files in bucket"));
    }

    @Test
    void testDeleteFileException() {
        // Given
        doThrow(new RuntimeException("Delete failed"))
                .when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.deleteFile(TEST_BUCKET_NAME, TEST_KEY);
        });
        assertTrue(exception.getMessage().contains("Failed to delete file from S3"));
    }
}