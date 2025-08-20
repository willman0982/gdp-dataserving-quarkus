package com.sc.gdp.common.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for S3PathParser
 */
class S3PathParserTest {

    @Test
    @DisplayName("Parse s3a:// URI format")
    void testParseS3aUri() {
        S3PathParser.S3PathInfo result = S3PathParser.parsePath("s3a://mybucket/path/to/file.txt", "gdp");
        
        assertEquals("mybucket", result.getBucketId());
        assertEquals("path/to/file.txt", result.getKey());
        assertTrue(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse s3:// URI format")
    void testParseS3Uri() {
        S3PathParser.S3PathInfo result = S3PathParser.parsePath("s3://testbucket/documents/report.pdf", "gdp");
        
        assertEquals("testbucket", result.getBucketId());
        assertEquals("documents/report.pdf", result.getKey());
        assertTrue(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse HTTPS S3 URL format")
    void testParseHttpsS3Url() {
        S3PathParser.S3PathInfo result = S3PathParser.parsePath(
            "https://s3.amazonaws.com/mybucket/folder/file.jpg", "gdp");
        
        assertEquals("mybucket", result.getBucketId());
        assertEquals("folder/file.jpg", result.getKey());
        assertTrue(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse HTTPS S3 URL with region")
    void testParseHttpsS3UrlWithRegion() {
        S3PathParser.S3PathInfo result = S3PathParser.parsePath(
            "https://mybucket.s3.us-west-2.amazonaws.com/mybucket/data/file.csv", "gdp");
        
        assertEquals("mybucket", result.getBucketId());
        assertEquals("data/file.csv", result.getKey());
        assertTrue(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse regular key with default bucket")
    void testParseRegularKey() {
        S3PathParser.S3PathInfo result = S3PathParser.parsePath("documents/file.txt", "gdp");

        assertEquals("gdp", result.getBucketId());
        assertEquals("documents/file.txt", result.getKey());
        assertFalse(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse regular key with custom default bucket")
    void testParseRegularKeyWithCustomDefault() {
        S3PathParser.S3PathInfo result = S3PathParser.parsePath("images/photo.png", "photos");
        
        assertEquals("photos", result.getBucketId());
        assertEquals("images/photo.png", result.getKey());
        assertFalse(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse bucket and key with explicit bucket")
    void testParseBucketAndKeyExplicit() {
        S3PathParser.S3PathInfo result = S3PathParser.parseBucketAndKey("mybucket", "path/file.txt", "gdp");
        
        assertEquals("mybucket", result.getBucketId());
        assertEquals("path/file.txt", result.getKey());
        assertFalse(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse bucket and key with null bucket falls back to path parsing")
    void testParseBucketAndKeyNullBucket() {
        S3PathParser.S3PathInfo result = S3PathParser.parseBucketAndKey(null, "s3a://testbucket/file.txt", "gdp");
        
        assertEquals("testbucket", result.getBucketId());
        assertEquals("file.txt", result.getKey());
        assertTrue(result.wasFullPath());
    }

    @Test
    @DisplayName("Parse bucket and key with empty bucket falls back to path parsing")
    void testParseBucketAndKeyEmptyBucket() {
        S3PathParser.S3PathInfo result = S3PathParser.parseBucketAndKey("", "regular/key.txt", "fallback");
        
        assertEquals("fallback", result.getBucketId());
        assertEquals("regular/key.txt", result.getKey());
        assertFalse(result.wasFullPath());
    }

    @Test
    @DisplayName("Check if path is S3 URI - positive cases")
    void testIsS3UriPositive() {
        assertTrue(S3PathParser.isS3Uri("s3://bucket/key"));
        assertTrue(S3PathParser.isS3Uri("s3a://bucket/key"));
        assertTrue(S3PathParser.isS3Uri("https://s3.amazonaws.com/bucket/key"));
        assertTrue(S3PathParser.isS3Uri("https://bucket.s3.us-east-1.amazonaws.com/bucket/key"));
    }

    @Test
    @DisplayName("Check if path is S3 URI - negative cases")
    void testIsS3UriNegative() {
        assertFalse(S3PathParser.isS3Uri("regular/key"));
        assertFalse(S3PathParser.isS3Uri("file.txt"));
        assertFalse(S3PathParser.isS3Uri("https://example.com/file"));
        assertFalse(S3PathParser.isS3Uri(""));
        assertFalse(S3PathParser.isS3Uri(null));
    }

    @Test
    @DisplayName("Convert to S3 URI format")
    void testToS3Uri() {
        String result = S3PathParser.toS3Uri("mybucket", "path/to/file.txt");
        assertEquals("s3a://mybucket/path/to/file.txt", result);
    }

    @Test
    @DisplayName("Convert to S3 URI format with trimming")
    void testToS3UriWithTrimming() {
        String result = S3PathParser.toS3Uri(" mybucket ", " path/to/file.txt ");
        assertEquals("s3a://mybucket/path/to/file.txt", result);
    }

    @Test
    @DisplayName("Parse path with null input throws exception")
    void testParsePathNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.parsePath(null, "gdp");
        });
    }

    @Test
    @DisplayName("Parse path with empty input throws exception")
    void testParsePathEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.parsePath("", "gdp");
        });
    }

    @Test
    @DisplayName("Parse bucket and key with null key throws exception")
    void testParseBucketAndKeyNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.parseBucketAndKey("bucket", null, "gdp");
        });
    }

    @Test
    @DisplayName("Parse bucket and key with empty key throws exception")
    void testParseBucketAndKeyEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.parseBucketAndKey("bucket", "", "gdp");
        });
    }

    @Test
    @DisplayName("Convert to S3 URI with null bucket throws exception")
    void testToS3UriNullBucket() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.toS3Uri(null, "key");
        });
    }

    @Test
    @DisplayName("Convert to S3 URI with empty bucket throws exception")
    void testToS3UriEmptyBucket() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.toS3Uri("", "key");
        });
    }

    @Test
    @DisplayName("Convert to S3 URI with null key throws exception")
    void testToS3UriNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.toS3Uri("bucket", null);
        });
    }

    @Test
    @DisplayName("Convert to S3 URI with empty key throws exception")
    void testToS3UriEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            S3PathParser.toS3Uri("bucket", "");
        });
    }

    @Test
    @DisplayName("S3PathInfo equals and hashCode")
    void testS3PathInfoEqualsAndHashCode() {
        S3PathParser.S3PathInfo info1 = new S3PathParser.S3PathInfo("bucket", "key", true);
        S3PathParser.S3PathInfo info2 = new S3PathParser.S3PathInfo("bucket", "key", true);
        S3PathParser.S3PathInfo info3 = new S3PathParser.S3PathInfo("bucket", "key", false);
        
        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
        assertNotEquals(info1, info3);
        assertNotEquals(info1.hashCode(), info3.hashCode());
    }

    @Test
    @DisplayName("S3PathInfo toString")
    void testS3PathInfoToString() {
        S3PathParser.S3PathInfo info = new S3PathParser.S3PathInfo("bucket", "key", true);
        String result = info.toString();
        
        assertTrue(result.contains("bucket"));
        assertTrue(result.contains("key"));
        assertTrue(result.contains("true"));
    }
}