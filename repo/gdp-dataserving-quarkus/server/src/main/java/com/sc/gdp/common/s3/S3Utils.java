package com.sc.gdp.common.s3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Utility class for S3 operations
 */
public class S3Utils {

    private static final Pattern VALID_S3_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9!_.*'()-/]+$");
    private static final int MAX_KEY_LENGTH = 1024;

    private S3Utils() {
        // Utility class - prevent instantiation
    }

    /**
     * Validate S3 object key
     * @param key The S3 object key to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidS3Key(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        if (key.length() > MAX_KEY_LENGTH) {
            return false;
        }
        
        // Check for invalid characters
        return VALID_S3_KEY_PATTERN.matcher(key).matches();
    }

    /**
     * Sanitize S3 object key by replacing invalid characters
     * @param key The S3 object key to sanitize
     * @return Sanitized key
     */
    public static String sanitizeS3Key(String key) {
        if (key == null) {
            return "";
        }
        
        // Replace invalid characters with underscores
        String sanitized = key.replaceAll("[^a-zA-Z0-9!_.*'()-/]", "_");
        
        // Trim to max length
        if (sanitized.length() > MAX_KEY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_KEY_LENGTH);
        }
        
        return sanitized;
    }

    /**
     * Generate a unique S3 key with timestamp
     * @param prefix Prefix for the key
     * @param filename Original filename
     * @return Unique S3 key
     */
    public static String generateUniqueKey(String prefix, String filename) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String sanitizedFilename = sanitizeS3Key(filename);
        
        if (prefix != null && !prefix.isEmpty()) {
            String sanitizedPrefix = sanitizeS3Key(prefix);
            return sanitizedPrefix + "/" + timestamp + "_" + sanitizedFilename;
        } else {
            return timestamp + "_" + sanitizedFilename;
        }
    }

    /**
     * Extract file extension from filename
     * @param filename The filename
     * @return File extension (without dot) or empty string if no extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }

    /**
     * Determine content type based on file extension
     * @param filename The filename
     * @return MIME content type
     */
    public static String determineContentType(String filename) {
        String extension = getFileExtension(filename);
        
        switch (extension) {
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "pdf":
                return "application/pdf";
            case "zip":
                return "application/zip";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "csv":
                return "text/csv";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Determine content type from file path
     * @param filePath The file path
     * @return MIME content type
     * @throws IOException if unable to probe content type
     */
    public static String determineContentType(Path filePath) throws IOException {
        String contentType = Files.probeContentType(filePath);
        if (contentType != null) {
            return contentType;
        }
        
        // Fallback to filename-based detection
        return determineContentType(filePath.getFileName().toString());
    }

    /**
     * Create S3FileInfo from basic parameters
     * @param key Object key
     * @param etag Object etag
     * @param size Object size
     * @param lastModified Last modified time
     * @param storageClass Storage class
     * @return S3FileInfo
     */
    public static S3FileInfo createS3FileInfo(String key, String etag, long size, Instant lastModified, String storageClass) {
        S3FileInfo fileInfo = new S3FileInfo(key, etag, size, lastModified, storageClass, null, null, null);
        return fileInfo;
    }

    /**
     * Format file size in human readable format
     * @param bytes File size in bytes
     * @return Formatted file size
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Check if a key represents a directory (ends with /)
     * @param key S3 object key
     * @return true if directory, false otherwise
     */
    public static boolean isDirectory(String key) {
        return key != null && key.endsWith("/");
    }

    /**
     * Get parent directory path from S3 key
     * @param key S3 object key
     * @return Parent directory path or empty string if no parent
     */
    public static String getParentPath(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        
        // Remove trailing slash if present
        String normalizedKey = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
        
        int lastSlashIndex = normalizedKey.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            return normalizedKey.substring(0, lastSlashIndex + 1);
        }
        
        return "";
    }

    /**
     * Get filename from S3 key
     * @param key S3 object key
     * @return Filename or the key itself if no path separator
     */
    public static String getFilename(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        
        // Remove trailing slash if present
        String normalizedKey = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
        
        int lastSlashIndex = normalizedKey.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < normalizedKey.length() - 1) {
            return normalizedKey.substring(lastSlashIndex + 1);
        }
        
        return normalizedKey;
    }
}