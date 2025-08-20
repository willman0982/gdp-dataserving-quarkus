package com.sc.gdp.common.s3;

import com.sc.gdp.dataserving.rest.S3Resource;
import com.sc.gdp.common.s3.S3Service;
import com.sc.gdp.common.s3.S3Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3Resource REST endpoints
 * Tests the actual methods available in S3Resource: listBuckets, generateDownloadPresignedUrl, generateUploadPresignedUrl
 */
@ExtendWith(MockitoExtension.class)
public class S3ResourceTest {

    @InjectMocks
    private S3Resource s3Resource;

    @Mock
    private S3Service s3Service;

    private static final String TEST_KEY = "test/file.txt";
    private static final String TEST_BUCKET_NAME = "test-bucket-name";
    private static final String TEST_CONTENT_TYPE = "text/plain";

    @BeforeEach
    void setUp() {
        // Setup is handled by Mockito annotations
    }

    @Test
    void testListBuckets() throws Exception {
        // Given
        List<String> bucketNames = Arrays.asList("gdp", "reports", "archives");
        when(s3Service.getBucketNames()).thenReturn(bucketNames);

        // When
        Response response = s3Resource.listBuckets();

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.BucketListResponse result = (S3Resource.BucketListResponse) response.getEntity();
        assertEquals(3, result.getBucketNames().size());
        assertEquals("gdp", result.getBucketNames().get(0));
        assertEquals("reports", result.getBucketNames().get(1));
        assertEquals("archives", result.getBucketNames().get(2));
        verify(s3Service).getBucketNames();
    }

    // Note: getBucketNames() doesn't throw S3Exception, so no exception test needed

    @Test
    void testGenerateDownloadPresignedUrl() throws Exception {
        // Given
        String expectedUrl = "https://s3.amazonaws.com/test-bucket/test/file.txt?signature=abc123";
        when(s3Service.generateDownloadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(TEST_KEY, TEST_BUCKET_NAME, null);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("download", result.getOperation());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_BUCKET_NAME, result.getBucketName());
        verify(s3Service).generateDownloadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithDefaultBucket() throws Exception {
        // Given
        String expectedUrl = "https://example.com/presigned-url";
        when(s3Service.generateDownloadSignedUrl("gdp", TEST_KEY, java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL("https://example.com/presigned-url"));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(TEST_KEY, "gdp", null);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("download", result.getOperation());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals("gdp", result.getBucketName());
        verify(s3Service).generateDownloadSignedUrl("gdp", TEST_KEY, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithInvalidKey() {
        // When
        Response response = s3Resource.generateDownloadPresignedUrl(null, "gdp", null);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Either 'key' or 'path' parameter is required"));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithS3Exception() throws Exception {
        // Given
        when(s3Service.generateDownloadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, java.time.Duration.ofMinutes(60)))
                .thenThrow(new S3Exception("Failed to generate presigned URL", new RuntimeException()));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(TEST_KEY, TEST_BUCKET_NAME, null);

        // Then
        assertEquals(500, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Failed to generate presigned URL"));
        verify(s3Service).generateDownloadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrl() throws Exception {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey(TEST_KEY);
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketName(TEST_BUCKET_NAME);
        
        String expectedUrl = "https://s3.amazonaws.com/test-bucket/test/file.txt?signature=upload123";
        when(s3Service.generateUploadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("upload", result.getOperation());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_CONTENT_TYPE, result.getContentType());
        assertEquals(TEST_BUCKET_NAME, result.getBucketName());
        verify(s3Service).generateUploadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrlWithDefaultBucket() throws Exception {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey(TEST_KEY);
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketName("gdp"); // bucketName is now mandatory
        
        String expectedUrl = "https://s3.amazonaws.com/gdp/test/file.txt?signature=upload123";
        when(s3Service.generateUploadSignedUrl("gdp", TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("upload", result.getOperation());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_CONTENT_TYPE, result.getContentType());
        assertEquals("gdp", result.getBucketName());
        verify(s3Service).generateUploadSignedUrl("gdp", TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrlWithInvalidRequest() {
        // Given - request with null key
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketName(TEST_BUCKET_NAME);

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Either 'key' or 'path' field is required"));
        verifyNoInteractions(s3Service);
    }

    @Test
    void testGenerateUploadPresignedUrlWithS3Exception() throws Exception {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey(TEST_KEY);
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketName(TEST_BUCKET_NAME);
        
        when(s3Service.generateUploadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60)))
                .thenThrow(new S3Exception("Failed to generate upload URL", new RuntimeException()));

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(500, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Failed to generate upload URL"));
        verify(s3Service).generateUploadSignedUrl(TEST_BUCKET_NAME, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60));
    }

    // Tests for flexible path format support

    @Test
    void testGenerateDownloadPresignedUrlWithFullS3Path() throws Exception {
        // Given
        String s3Path = "s3a://mybucket/documents/file.pdf";
        String expectedUrl = "https://s3.amazonaws.com/mybucket/documents/file.pdf?signature=abc123";
        when(s3Service.generateDownloadSignedUrl("mybucket", "documents/file.pdf", java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(null, "gdp", s3Path);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("download", result.getOperation());
        assertEquals("documents/file.pdf", result.getKey());
        assertEquals("mybucket", result.getBucketName());
        verify(s3Service).generateDownloadSignedUrl("mybucket", "documents/file.pdf", java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithS3Protocol() throws Exception {
        // Given
        String s3Path = "s3://testbucket/data/report.csv";
        String expectedUrl = "https://s3.amazonaws.com/testbucket/data/report.csv?signature=xyz789";
        when(s3Service.generateDownloadSignedUrl("testbucket", "data/report.csv", java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(null, "gdp", s3Path);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("download", result.getOperation());
        assertEquals("data/report.csv", result.getKey());
        assertEquals("testbucket", result.getBucketName());
        verify(s3Service).generateDownloadSignedUrl("testbucket", "data/report.csv", java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithInvalidS3Path() {
        // Given
        String invalidPath = "invalid://path/file.txt";

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(null, "gdp", invalidPath);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Invalid path format") || result.getError().contains("Invalid S3 object key format"));
    }

    @Test
    void testGenerateUploadPresignedUrlWithFullS3Path() throws Exception {
        // Given
        String s3Path = "s3a://uploads/images/photo.jpg";
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setPath(s3Path);
        request.setContentType("image/jpeg");
        request.setBucketName("uploads"); // bucketName is now mandatory
        
        String expectedUrl = "https://s3.amazonaws.com/uploads/images/photo.jpg?signature=def456";
        when(s3Service.generateUploadSignedUrl("uploads", "images/photo.jpg", "image/jpeg", java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("upload", result.getOperation());
        assertEquals("images/photo.jpg", result.getKey());
        assertEquals("uploads", result.getBucketName());
        assertEquals("image/jpeg", result.getContentType());
        verify(s3Service).generateUploadSignedUrl("uploads", "images/photo.jpg", "image/jpeg", java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrlWithPathOverridesKey() throws Exception {
        // Given - both path and key provided, path should take precedence
        String s3Path = "s3a://priority/folder/file.txt";
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey("ignored/key.txt");
        request.setPath(s3Path);
        request.setContentType("text/plain");
        request.setBucketName("ignored-bucket");
        
        String expectedUrl = "https://s3.amazonaws.com/priority/folder/file.txt?signature=ghi789";
        when(s3Service.generateUploadSignedUrl("priority", "folder/file.txt", "text/plain", java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("priority", result.getBucketName());
        assertEquals("folder/file.txt", result.getKey());
        verify(s3Service).generateUploadSignedUrl("priority", "folder/file.txt", "text/plain", java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrlWithInvalidS3Path() {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setPath("invalid@#$%path");  // Use truly invalid S3 key characters
        request.setContentType("text/plain");
        request.setBucketName("gdp"); // bucketName is now mandatory
        
        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Invalid path format") || result.getError().contains("Invalid S3 object key format"));
        verifyNoMoreInteractions(s3Service);
    }

    @Test
    void testGenerateUploadPresignedUrlWithNoKeyOrPath() {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setContentType("text/plain");
        // No key or path set

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Either 'key' or 'path' field is required"));
    }
}