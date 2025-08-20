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
    private static final String TEST_BUCKET_ID = "test-bucket-id";
    private static final String TEST_CONTENT_TYPE = "text/plain";

    @BeforeEach
    void setUp() {
        // Setup is handled by Mockito annotations
    }

    @Test
    void testListBuckets() throws Exception {
        // Given
        List<String> bucketIds = Arrays.asList("default", "reports", "archives");
        when(s3Service.getBucketIds()).thenReturn(bucketIds);

        // When
        Response response = s3Resource.listBuckets();

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.BucketListResponse result = (S3Resource.BucketListResponse) response.getEntity();
        assertEquals(3, result.getBucketIds().size());
        assertEquals("default", result.getBucketIds().get(0));
        assertEquals("reports", result.getBucketIds().get(1));
        assertEquals("archives", result.getBucketIds().get(2));
        verify(s3Service).getBucketIds();
    }

    // Note: getBucketIds() doesn't throw S3Exception, so no exception test needed

    @Test
    void testGenerateDownloadPresignedUrl() throws Exception {
        // Given
        String expectedUrl = "https://s3.amazonaws.com/test-bucket/test/file.txt?signature=abc123";
        when(s3Service.generateDownloadSignedUrl(TEST_BUCKET_ID, TEST_KEY, java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(TEST_KEY, TEST_BUCKET_ID);

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("download", result.getOperation());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_BUCKET_ID, result.getBucketId());
        verify(s3Service).generateDownloadSignedUrl(TEST_BUCKET_ID, TEST_KEY, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithDefaultBucket() throws Exception {
        // Given
        String expectedUrl = "https://s3.amazonaws.com/default/test/file.txt?signature=abc123";
        when(s3Service.generateDownloadSignedUrl("default", TEST_KEY, java.time.Duration.ofMinutes(60)))
                .thenReturn(new URL(expectedUrl));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(TEST_KEY, "default");

        // Then
        assertEquals(200, response.getStatus());
        S3Resource.PresignedUrlResponse result = (S3Resource.PresignedUrlResponse) response.getEntity();
        assertEquals(expectedUrl, result.getUrl());
        assertEquals("download", result.getOperation());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals("default", result.getBucketId());
        verify(s3Service).generateDownloadSignedUrl("default", TEST_KEY, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateDownloadPresignedUrlWithInvalidKey() {
        // When
        Response response = s3Resource.generateDownloadPresignedUrl(null, TEST_BUCKET_ID);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertEquals("S3 object key is required", result.getError());
        verifyNoInteractions(s3Service);
    }

    @Test
    void testGenerateDownloadPresignedUrlWithS3Exception() throws Exception {
        // Given
        when(s3Service.generateDownloadSignedUrl(TEST_BUCKET_ID, TEST_KEY, java.time.Duration.ofMinutes(60)))
                .thenThrow(new S3Exception("Failed to generate presigned URL", new RuntimeException()));

        // When
        Response response = s3Resource.generateDownloadPresignedUrl(TEST_KEY, TEST_BUCKET_ID);

        // Then
        assertEquals(500, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Failed to generate presigned URL"));
        verify(s3Service).generateDownloadSignedUrl(TEST_BUCKET_ID, TEST_KEY, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrl() throws Exception {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey(TEST_KEY);
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketId(TEST_BUCKET_ID);
        
        String expectedUrl = "https://s3.amazonaws.com/test-bucket/test/file.txt?signature=upload123";
        when(s3Service.generateUploadSignedUrl(TEST_BUCKET_ID, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60)))
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
        assertEquals(TEST_BUCKET_ID, result.getBucketId());
        verify(s3Service).generateUploadSignedUrl(TEST_BUCKET_ID, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrlWithDefaultBucket() throws Exception {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey(TEST_KEY);
        request.setContentType(TEST_CONTENT_TYPE);
        // bucketId is null, should default to "default"
        
        String expectedUrl = "https://s3.amazonaws.com/default/test/file.txt?signature=upload123";
        when(s3Service.generateUploadSignedUrl("default", TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60)))
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
        assertEquals("default", result.getBucketId());
        verify(s3Service).generateUploadSignedUrl("default", TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60));
    }

    @Test
    void testGenerateUploadPresignedUrlWithInvalidRequest() {
        // Given - request with null key
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketId(TEST_BUCKET_ID);

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(400, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertEquals("S3 object key is required", result.getError());
        verifyNoInteractions(s3Service);
    }

    @Test
    void testGenerateUploadPresignedUrlWithS3Exception() throws Exception {
        // Given
        S3Resource.UploadPresignedUrlRequest request = new S3Resource.UploadPresignedUrlRequest();
        request.setKey(TEST_KEY);
        request.setContentType(TEST_CONTENT_TYPE);
        request.setBucketId(TEST_BUCKET_ID);
        
        when(s3Service.generateUploadSignedUrl(TEST_BUCKET_ID, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60)))
                .thenThrow(new S3Exception("Failed to generate upload URL", new RuntimeException()));

        // When
        Response response = s3Resource.generateUploadPresignedUrl(request);

        // Then
        assertEquals(500, response.getStatus());
        S3Resource.ErrorResponse result = (S3Resource.ErrorResponse) response.getEntity();
        assertTrue(result.getError().contains("Failed to generate upload URL"));
        verify(s3Service).generateUploadSignedUrl(TEST_BUCKET_ID, TEST_KEY, TEST_CONTENT_TYPE, java.time.Duration.ofMinutes(60));
    }
}