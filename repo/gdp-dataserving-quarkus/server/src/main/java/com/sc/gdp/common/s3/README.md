# S3 Common Library

A comprehensive S3 library for handling S3a protocol operations on Isilon storage systems. This library provides a unified interface for S3 operations including file upload, download, signed URL generation, and file management.

**Note**: This library uses AWS SDK for Java v1 (version 1.12.788) for compatibility with existing systems. The implementation has been updated from AWS SDK v2 to v1 to ensure better compatibility with legacy infrastructure.

## Features

- **File Operations**: Upload, download, copy, and delete files
- **Signed URLs**: Generate pre-signed URLs for secure file access
- **File Listing**: List files with optional prefix filtering
- **Metadata Management**: Retrieve file metadata and check file existence
- **S3a Protocol Support**: Optimized for Isilon storage systems
- **Configuration Management**: Type-safe configuration using Quarkus ConfigMapping
- **Utility Functions**: File type detection, key validation, and formatting helpers

## Components

### Core Classes

- **`S3Service`**: Main service class providing all S3 operations
- **`S3Config`**: Configuration interface for S3 settings
- **`S3FileInfo`**: Data transfer object for file information
- **`S3Utils`**: Utility class with helper methods
- **`S3Exception`**: Custom exception for S3 operations

## Configuration

### Single Bucket Configuration

Add the following configuration to your `application.properties`:

```properties
# Default S3 Configuration
s3.endpoint-url=http://your-isilon-endpoint:9020
s3.access-key=your-access-key
s3.secret-key=your-secret-key
s3.region=us-east-1
s3.bucket-name=your-bucket-name
s3.path-style-access=true
s3.signed-url-duration-minutes=60
s3.connection-timeout-ms=30000
s3.socket-timeout-ms=30000
s3.max-retry-attempts=3
```

### Multiple Bucket Configuration

To configure multiple S3 buckets, use the pattern `s3.buckets.[bucket-id].[property]`:

```properties
# Default bucket configuration (required)
s3.endpoint-url=http://localhost:9000
s3.access-key=minioadmin
s3.secret-key=minioadmin
s3.bucket-name=gdp-dataserving
s3.region=us-east-1
s3.path-style-access=true

# Reports bucket with different credentials
s3.buckets.reports.name=gdp-reports
s3.buckets.reports.access-key=reports-user
s3.buckets.reports.secret-key=reports-password

# Archives bucket on AWS S3
s3.buckets.archives.name=gdp-archives
s3.buckets.archives.endpoint-url=https://s3.amazonaws.com
s3.buckets.archives.region=us-west-2
s3.buckets.archives.access-key=AKIAIOSFODNN7EXAMPLE
s3.buckets.archives.secret-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
s3.buckets.archives.path-style-access=false

# Backup bucket (inherits most settings from default)
s3.buckets.backup.name=gdp-backup
```

### Configuration Properties

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `endpoint-url` | S3 endpoint URL for Isilon | - | Yes |
| `access-key` | S3 access key | - | Yes |
| `secret-key` | S3 secret key | - | Yes |
| `region` | S3 region | `us-east-1` | No |
| `bucket-name` | S3 bucket name | - | Yes |
| `path-style-access` | Use path-style access | `true` | No |
| `signed-url-duration-minutes` | Default signed URL duration | `60` | No |
| `connection-timeout-ms` | Connection timeout | `30000` | No |
| `socket-timeout-ms` | Socket timeout | `30000` | No |
| `max-retry-attempts` | Maximum retry attempts | `3` | No |

## Usage Examples

### Basic File Operations

```java
@Inject
S3Service s3Service;

// Upload a file
String fileUrl = s3Service.uploadFile("documents/report.pdf", Paths.get("/local/path/report.pdf"));

// Download a file
try (S3Object s3Object = s3Service.downloadFile("documents/report.pdf")) {
    // Process the file content using s3Object.getObjectContent()
}

// Download to local file
s3Service.downloadFile("documents/report.pdf", Paths.get("/local/path/downloaded-report.pdf"));

// Check if file exists
boolean exists = s3Service.fileExists("documents/report.pdf");

// Delete a file
s3Service.deleteFile("documents/report.pdf");
```

### Upload from InputStream

```java
// Upload from InputStream
try (InputStream inputStream = new FileInputStream(localFile)) {
    String contentType = S3Utils.determineContentType("document.pdf");
    long contentLength = localFile.length();
    
    String fileUrl = s3Service.uploadFile(
        "documents/document.pdf", 
        inputStream, 
        contentLength, 
        contentType
    );
}
```

### Signed URLs

```java
// Generate download URL (valid for 1 hour by default)
URL downloadUrl = s3Service.generateDownloadSignedUrl("documents/report.pdf");

// Generate download URL with custom duration
URL downloadUrl = s3Service.generateDownloadSignedUrl(
    "documents/report.pdf", 
    Duration.ofHours(2)
);

// Generate upload URL
URL uploadUrl = s3Service.generateUploadSignedUrl(
    "documents/new-report.pdf", 
    "application/pdf"
);
```

### File Listing

```java
// List all files
List<S3FileInfo> allFiles = s3Service.listFiles();

// List files with prefix
List<S3FileInfo> documents = s3Service.listFiles("documents/");

// Get just the file keys
List<String> fileKeys = s3Service.listFileKeys("documents/");

// Process file information
for (S3FileInfo fileInfo : documents) {
    System.out.println("File: " + fileInfo.getKey());
    System.out.println("Size: " + S3Utils.formatFileSize(fileInfo.getSize()));
    System.out.println("Modified: " + fileInfo.getLastModified());
}
```

### File Metadata

```java
// Get file metadata
ObjectMetadata metadata = s3Service.getFileMetadata("documents/report.pdf");
System.out.println("Content Type: " + metadata.getContentType());
System.out.println("Content Length: " + metadata.getContentLength());
System.out.println("Last Modified: " + metadata.getLastModified());

// Get file size
long fileSize = s3Service.getFileSize("documents/report.pdf");
```

### File Operations

```java
// Copy a file
s3Service.copyFile("documents/report.pdf", "backup/report-backup.pdf");
```

## Multi-Bucket Usage

When multiple buckets are configured, you can specify which bucket to use for operations by providing a bucket ID.

### Multi-Bucket File Operations

```java
@Inject
S3Service s3Service;

// Upload to specific bucket
String fileUrl = s3Service.uploadFile("reports", "monthly/report.pdf", Paths.get("/local/path/report.pdf"));

// Download from specific bucket
s3Service.downloadFile("archives", "2023/data.csv", Paths.get("/local/path/data.csv"));

// Check if file exists in specific bucket
boolean exists = s3Service.fileExists("backup", "documents/report.pdf");

// Delete from specific bucket
s3Service.deleteFile("reports", "old/report.pdf");
```

### Multi-Bucket Signed URLs

```java
// Generate download URL for specific bucket
URL downloadUrl = s3Service.generateDownloadSignedUrl("reports", "monthly/report.pdf", Duration.ofHours(1));

// Generate upload URL for specific bucket
URL uploadUrl = s3Service.generateUploadSignedUrl("archives", "2024/data.csv", "text/csv", Duration.ofMinutes(30));

// Using default bucket (same as before)
URL defaultUrl = s3Service.generateDownloadSignedUrl("documents/report.pdf");
```

### Multi-Bucket File Listing

```java
// List files in specific bucket
List<S3FileInfo> reportsFiles = s3Service.listFiles("reports", "monthly/");

// List all files in specific bucket
List<S3FileInfo> allArchives = s3Service.listFiles("archives");

// Get file keys from specific bucket
List<String> backupKeys = s3Service.listFileKeys("backup", "documents/");
```

### REST API Usage

The S3 REST endpoints support bucket selection:

```bash
# List available buckets
curl -X GET "http://localhost:8082/s3/buckets"

# Generate download URL for default bucket
curl -X GET "http://localhost:8082/s3/download/presigned?key=file.pdf"

# Generate download URL for specific bucket
curl -X GET "http://localhost:8082/s3/download/presigned?key=file.pdf&bucketId=reports"

# Generate upload URL for specific bucket
curl -X POST "http://localhost:8082/s3/upload/presigned" \
  -H "Content-Type: application/json" \
  -d '{"key": "new-file.pdf", "contentType": "application/pdf", "bucketId": "archives"}'
```

### Utility Functions

```java
// Validate S3 key
boolean isValid = S3Utils.isValidS3Key("documents/report.pdf");

// Sanitize S3 key
String sanitized = S3Utils.sanitizeS3Key("documents/report with spaces.pdf");

// Generate unique key
String uniqueKey = S3Utils.generateUniqueKey("documents", "report.pdf");

// Determine content type
String contentType = S3Utils.determineContentType("report.pdf");
String contentTypeFromPath = S3Utils.determineContentType(Paths.get("/path/to/file.pdf"));

// Format file size
String formattedSize = S3Utils.formatFileSize(1024 * 1024); // "1.0 MB"

// Extract filename from key
String filename = S3Utils.getFilename("documents/subfolder/report.pdf"); // "report.pdf"

// Get parent path
String parentPath = S3Utils.getParentPath("documents/subfolder/report.pdf"); // "documents/subfolder/"
```

## Error Handling

All S3 operations throw `S3Exception` for error conditions:

```java
try {
    String fileUrl = s3Service.uploadFile("documents/report.pdf", filePath);
    System.out.println("File uploaded successfully: " + fileUrl);
} catch (S3Exception e) {
    System.err.println("Failed to upload file: " + e.getMessage());
    // Handle the error appropriately
}
```

## Integration with Quarkus

The library is designed to work seamlessly with Quarkus:

```java
@ApplicationScoped
public class DocumentService {
    
    @Inject
    S3Service s3Service;
    
    public String uploadDocument(String filename, InputStream content, long size) throws S3Exception {
        String key = S3Utils.generateUniqueKey("documents", filename);
        String contentType = S3Utils.determineContentType(filename);
        
        return s3Service.uploadFile(key, content, size, contentType);
    }
    
    public List<S3FileInfo> listDocuments() throws S3Exception {
        return s3Service.listFiles("documents/");
    }
}
```

## Dependencies

The library requires the following dependencies in your `pom.xml`:

```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
</dependency>
```

And the AWS SDK BOM in your parent `pom.xml`:

```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-bom</artifactId>
    <version>${aws.sdk.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

## Best Practices

1. **Key Naming**: Use forward slashes for directory-like structure
2. **Content Type**: Always specify appropriate content types for uploads
3. **Error Handling**: Wrap S3 operations in try-catch blocks
4. **Resource Management**: Use try-with-resources for InputStreams
5. **Key Validation**: Use `S3Utils.isValidS3Key()` to validate keys before operations
6. **Unique Keys**: Use `S3Utils.generateUniqueKey()` to avoid naming conflicts

## Performance Considerations

- Use streaming for large files to avoid memory issues
- Configure appropriate timeouts for your network environment
- Consider using multipart uploads for very large files (future enhancement)
- Cache file metadata when possible to reduce API calls

## Security

- Store credentials securely (use environment variables or secure configuration)
- Use signed URLs for temporary access instead of exposing direct S3 URLs
- Implement proper access controls at the application level
- Validate file types and sizes before upload

## Troubleshooting

### Common Issues

1. **Connection Timeouts**: Increase `connection-timeout-ms` and `socket-timeout-ms`
2. **Invalid Keys**: Use `S3Utils.sanitizeS3Key()` to clean problematic characters
3. **Authentication Errors**: Verify access key, secret key, and endpoint URL
4. **Path Style Access**: Ensure `path-style-access: true` for Isilon systems

### Logging

Enable debug logging for AWS SDK:

```yaml
quarkus:
  log:
    category:
      "com.amazonaws":
        level: DEBUG
```

## Version History

### v1.1.0 (Current)
- **Breaking Change**: Migrated from AWS SDK v2 to AWS SDK v1 (1.12.788)
- Updated all API calls to use AWS SDK v1 patterns
- Changed dependency from `software.amazon.awssdk:s3` to `com.amazonaws:aws-java-sdk-s3`
- Updated method signatures and return types
- Improved compatibility with legacy systems

### v1.0.0
- Initial implementation with AWS SDK v2
- Basic S3 operations support
- Signed URL generation
- File management utilities

## Migration Notes

If you're upgrading from a previous version that used AWS SDK v2, please note the following changes:

1. **Dependencies**: Update your `pom.xml` to use `com.amazonaws:aws-java-sdk-s3` instead of `software.amazon.awssdk:s3`
2. **Return Types**: `HeadObjectResponse` is now `ObjectMetadata`, `ResponseInputStream<GetObjectResponse>` is now `S3Object`
3. **Method Calls**: Metadata access methods have changed (e.g., `contentType()` → `getContentType()`)
4. **Logging**: Update logging configuration from `software.amazon.awssdk` to `com.amazonaws`

## Future Enhancements

- Multipart upload support for large files
- Async operations support
- Batch operations
- File versioning support
- Advanced retry strategies
- Metrics and monitoring integration
- Migration path back to AWS SDK v2 when infrastructure supports it