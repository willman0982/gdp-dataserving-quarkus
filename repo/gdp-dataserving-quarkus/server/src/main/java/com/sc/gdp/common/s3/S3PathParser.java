package com.sc.gdp.common.s3;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for parsing S3 paths in different formats
 * Supports both separate bucket+key parameters and full S3 paths like "s3a://bucket/key"
 */
public class S3PathParser {

    private static final Pattern S3_URI_PATTERN = Pattern.compile("^s3a?://([^/]+)/(.*)$");
    private static final Pattern S3_HTTPS_PATTERN = Pattern.compile("^https?://([^/]+\\.)?s3[^/]*\\.amazonaws\\.com/([^/]+)/(.*)$");
    
    private S3PathParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parse S3 path information from various input formats
     * @param path The path to parse (can be s3a://bucket/key, bucket+key, or just key)
     * @param defaultBucketName Default bucket name to use if not specified in path
     * @return S3PathInfo containing bucket name and key
     */
    public static S3PathInfo parsePath(String path, String defaultBucketName) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        String trimmedPath = path.trim();
        
        // Check for s3a:// or s3:// URI format
        Matcher s3UriMatcher = S3_URI_PATTERN.matcher(trimmedPath);
        if (s3UriMatcher.matches()) {
            String bucketName = s3UriMatcher.group(1);
            String key = s3UriMatcher.group(2);
            return new S3PathInfo(bucketName, key, true);
        }
        
        // Check for HTTPS S3 URL format
        Matcher httpsS3Matcher = S3_HTTPS_PATTERN.matcher(trimmedPath);
        if (httpsS3Matcher.matches()) {
            String bucketName = httpsS3Matcher.group(2);
            String key = httpsS3Matcher.group(3);
            return new S3PathInfo(bucketName, key, true);
        }
        
        // Default case: treat as regular key with default bucket
        String bucketName = (defaultBucketName != null && !defaultBucketName.trim().isEmpty()) 
            ? defaultBucketName.trim() : "gdp";
        return new S3PathInfo(bucketName, trimmedPath, false);
    }

    /**
     * Parse S3 path with explicit bucket and key parameters
     * @param bucketName Bucket name (can be null)
     * @param key Object key
     * @param defaultBucketName Default bucket name to use if bucketName is null/empty
     * @return S3PathInfo containing resolved bucket name and key
     */
    public static S3PathInfo parseBucketAndKey(String bucketName, String key, String defaultBucketName) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        // First, check if the key is actually a full S3 URI that should be parsed
        if (isS3Uri(key)) {
            return parsePath(key, defaultBucketName);
        }
        
        // If bucketName is provided and not empty, use it with the key
        if (bucketName != null && !bucketName.trim().isEmpty()) {
            return new S3PathInfo(bucketName.trim(), key.trim(), false);
        }
        
        // Otherwise, use default bucket with the key
        String resolvedBucketName = (defaultBucketName != null && !defaultBucketName.trim().isEmpty()) 
            ? defaultBucketName.trim() : "gdp";
        return new S3PathInfo(resolvedBucketName, key.trim(), false);
    }

    /**
     * Check if a path is a full S3 URI (s3a://bucket/key format)
     * @param path The path to check
     * @return true if it's a full S3 URI, false otherwise
     */
    public static boolean isS3Uri(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        return S3_URI_PATTERN.matcher(path.trim()).matches() || 
               S3_HTTPS_PATTERN.matcher(path.trim()).matches();
    }

    /**
     * Convert bucket name and key to s3a:// URI format
     * @param bucketName Bucket name
     * @param key Object key
     * @return S3 URI in s3a://bucket/key format
     */
    public static String toS3Uri(String bucketName, String key) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        return "s3a://" + bucketName.trim() + "/" + key.trim();
    }

    /**
     * Data class to hold parsed S3 path information
     */
    public static class S3PathInfo {
        private final String bucketName;
        private final String key;
        private final boolean wasFullPath;

        public S3PathInfo(String bucketName, String key, boolean wasFullPath) {
            this.bucketName = bucketName;
            this.key = key;
            this.wasFullPath = wasFullPath;
        }

        public String getBucketId() {
            return bucketName;
        }

        public String getKey() {
            return key;
        }

        public boolean wasFullPath() {
            return wasFullPath;
        }

        @Override
        public String toString() {
            return "S3PathInfo{bucketName='" + bucketName + "', key='" + key + "', wasFullPath=" + wasFullPath + "}";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            S3PathInfo that = (S3PathInfo) obj;
            return wasFullPath == that.wasFullPath &&
                   java.util.Objects.equals(bucketName, that.bucketName) &&
                   java.util.Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(bucketName, key, wasFullPath);
        }
    }
}