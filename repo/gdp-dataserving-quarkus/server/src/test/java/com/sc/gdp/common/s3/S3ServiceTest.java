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
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_KEY = "test/file.txt";
    private static final String TEST_BUCKET_ID = "test-bucket-id";
    private static final String TEST_CONTENT = "test content";

    @BeforeEach
    void setUp() {
        // Mock S3Config with lenient stubbing to avoid unnecessary stubbing errors
        lenient().when(mockS3Config.bucketName()).thenReturn(TEST_BUCKET);
        lenient().when(mockS3Config.endpointUrl()).thenReturn("http://localhost:9000");
        lenient().when(mockS3Config.accessKey()).thenReturn("test-access-key");
        lenient().when(mockS3Config.secretKey()).thenReturn("test-secret-key");
        lenient().when(mockS3Config.region()).thenReturn("us-east-1");
        lenient().when(mockS3Config.pathStyleAccess()).thenReturn(true);
    }

    @Test
    void testUploadFileWithPath() throws Exception {
        // Given
        Path testPath = Paths.get("test-file.txt");
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_KEY, testPath);
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
            String result = s3Service.uploadFile(TEST_KEY, inputStream, contentLength, contentType);
            assertNotNull(result);
        });

        verify(mockS3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    void testUploadFileWithBucketId() throws Exception {
        // Given
        Path testPath = Paths.get("test-file.txt");
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_BUCKET_ID, TEST_KEY, testPath);
            assertNotNull(result);
        });
    }

    @Test
    void testUploadFileWithBucketIdAndInputStream() throws Exception {
        // Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        long contentLength = TEST_CONTENT.length();
        String contentType = "text/plain";
        PutObjectResult putObjectResult = new PutObjectResult();
        when(mockS3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);

        // When & Then
        assertDoesNotThrow(() -> {
            String result = s3Service.uploadFile(TEST_BUCKET_ID, TEST_KEY, inputStream, contentLength, contentType);
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
        S3Object result = s3Service.downloadFile(TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(TEST_KEY, result.getKey());
        verify(mockS3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testDownloadFileWithBucketId() throws Exception {
        // Given
        S3Object s3Object = new S3Object();
        s3Object.setKey(TEST_KEY);
        s3Object.setObjectContent(new ByteArrayInputStream(TEST_CONTENT.getBytes()));
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

        // When
        S3Object result = s3Service.downloadFile(TEST_BUCKET_ID, TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(TEST_KEY, result.getKey());
    }

    @Test
    void testListFiles() throws Exception {
        // Given
        ObjectListing objectListing = new ObjectListing();
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

        objectListing.getObjectSummaries().addAll(Arrays.asList(summary1, summary2));
        when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(objectListing);

        // When
        List<S3FileInfo> result = s3Service.listFiles();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0).getKey());
        assertEquals("file2.txt", result.get(1).getKey());
        verify(mockS3Client).listObjects(any(ListObjectsRequest.class));
    }

    @Test
    void testListFilesWithPrefix() throws Exception {
        // Given
        String prefix = "documents/";
        ObjectListing objectListing = new ObjectListing();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("documents/file.txt");
        summary.setETag("etag");
        summary.setSize(100L);
        summary.setLastModified(new Date());
        summary.setStorageClass("STANDARD");
        objectListing.getObjectSummaries().add(summary);
        when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(objectListing);

        // When
        List<S3FileInfo> result = s3Service.listFiles(prefix);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("documents/file.txt", result.get(0).getKey());
        verify(mockS3Client).listObjects(any(ListObjectsRequest.class));
    }

    @Test
    void testListFilesInBucket() throws Exception {
        // Given
        ObjectListing objectListing = new ObjectListing();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("bucket-file.txt");
        summary.setETag("etag");
        summary.setSize(100L);
        summary.setLastModified(new Date());
        summary.setStorageClass("STANDARD");
        objectListing.getObjectSummaries().add(summary);
        when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(objectListing);

        // When
        List<S3FileInfo> result = s3Service.listFilesInBucket(TEST_BUCKET_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("bucket-file.txt", result.get(0).getKey());
    }

    @Test
    void testListFileKeys() throws Exception {
        // Given
        ObjectListing objectListing = new ObjectListing();
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

        objectListing.getObjectSummaries().addAll(Arrays.asList(summary1, summary2));
        when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(objectListing);

        // When
        List<String> result = s3Service.listFileKeys();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0));
        assertEquals("file2.txt", result.get(1));
        verify(mockS3Client).listObjects(any(ListObjectsRequest.class));
    }

    @Test
    void testListFileKeysInBucket() throws Exception {
        // Given
        ObjectListing objectListing = new ObjectListing();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("bucket-file.txt");
        summary.setETag("etag");
        summary.setSize(100L);
        summary.setLastModified(new Date());
        summary.setStorageClass("STANDARD");
        objectListing.getObjectSummaries().add(summary);
        when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(objectListing);

        // When
        List<String> result = s3Service.listFileKeysInBucket(TEST_BUCKET_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("bucket-file.txt", result.get(0));
    }

    @Test
    void testDeleteFile() throws Exception {
        // Given
        doNothing().when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

        // When & Then
        assertDoesNotThrow(() -> s3Service.deleteFile(TEST_KEY));
        verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFileWithBucketId() throws Exception {
        // Given
        doNothing().when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

        // When & Then
        assertDoesNotThrow(() -> s3Service.deleteFile(TEST_BUCKET_ID, TEST_KEY));
    }

    @Test
    void testFileExists() throws Exception {
        // Given
        ObjectMetadata metadata = new ObjectMetadata();
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

        // When
        boolean result = s3Service.fileExists(TEST_KEY);

        // Then
        assertTrue(result);
        verify(mockS3Client).getObjectMetadata(any(GetObjectMetadataRequest.class));
    }

    @Test
    void testFileExistsNotFound() throws Exception {
        // Given
        AmazonServiceException exception = new AmazonServiceException("Not Found");
        exception.setStatusCode(404);
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenThrow(exception);

        // When
        boolean result = s3Service.fileExists(TEST_KEY);

        // Then
        assertFalse(result);
        verify(mockS3Client).getObjectMetadata(any(GetObjectMetadataRequest.class));
    }

    @Test
    void testFileExistsWithBucketId() throws Exception {
        // Given
        ObjectMetadata metadata = new ObjectMetadata();
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

        // When
        boolean result = s3Service.fileExists(TEST_BUCKET_ID, TEST_KEY);

        // Then
        assertTrue(result);
    }

    @Test
    void testGetFileMetadata() throws Exception {
        // Given
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(100L);
        metadata.setContentType("text/plain");
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

        // When
        ObjectMetadata result = s3Service.getFileMetadata(TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getContentLength());
        assertEquals("text/plain", result.getContentType());
        verify(mockS3Client).getObjectMetadata(any(GetObjectMetadataRequest.class));
    }

    @Test
    void testGetFileMetadataWithBucketId() throws Exception {
        // Given
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(200L);
        metadata.setContentType("application/pdf");
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

        // When
        ObjectMetadata result = s3Service.getFileMetadata(TEST_BUCKET_ID, TEST_KEY);

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
        assertDoesNotThrow(() -> s3Service.copyFile(sourceKey, destinationKey));
        verify(mockS3Client).copyObject(any(CopyObjectRequest.class));
    }

    @Test
    void testCopyFileWithBucketId() throws Exception {
        // Given
        String sourceKey = "source/file.txt";
        String destinationKey = "destination/file.txt";
        CopyObjectResult copyResult = new CopyObjectResult();
        when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyResult);

        // When & Then
        assertDoesNotThrow(() -> s3Service.copyFile(TEST_BUCKET_ID, sourceKey, destinationKey));
    }

    @Test
    void testCopyFileBetweenBuckets() throws Exception {
        // Given
        String sourceBucketId = "source-bucket";
        String destinationBucketId = "destination-bucket";
        String sourceKey = "source/file.txt";
        String destinationKey = "destination/file.txt";
        CopyObjectResult copyResult = new CopyObjectResult();
        when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyResult);

        // When & Then
        assertDoesNotThrow(() -> s3Service.copyFile(sourceBucketId, sourceKey, destinationBucketId, destinationKey));
    }

    @Test
    void testGetFileSize() throws Exception {
        // Given
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(1024L);
        when(mockS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(metadata);

        // When
        long result = s3Service.getFileSize(TEST_KEY);

        // Then
        assertEquals(1024L, result);
        verify(mockS3Client).getObjectMetadata(any(GetObjectMetadataRequest.class));
    }

    @Test
    void testUploadFileException() {
        // Given
        Path testPath = Paths.get("test-file.txt");
        when(mockS3Client.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Upload failed"));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.uploadFile(TEST_KEY, testPath);
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
            s3Service.downloadFile(TEST_KEY);
        });
        assertTrue(exception.getMessage().contains("Failed to download file from S3"));
    }

    @Test
    void testListFilesException() {
        // Given
        when(mockS3Client.listObjects(any(ListObjectsRequest.class)))
                .thenThrow(new RuntimeException("List failed"));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.listFiles();
        });
        assertTrue(exception.getMessage().contains("Failed to list files from S3"));
    }

    @Test
    void testDeleteFileException() {
        // Given
        doThrow(new RuntimeException("Delete failed"))
                .when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

        // When & Then
        S3Exception exception = assertThrows(S3Exception.class, () -> {
            s3Service.deleteFile(TEST_KEY);
        });
        assertTrue(exception.getMessage().contains("Failed to delete file from S3"));
    }
}