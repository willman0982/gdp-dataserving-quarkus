package com.sc.gdp.dataserving.rest;

import com.sc.gdp.common.s3.S3Service;
import com.sc.gdp.common.s3.S3Utils;
import com.sc.gdp.common.s3.S3Exception;
import com.sc.gdp.common.s3.S3PathParser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * REST API resource for S3 operations
 */
@Path("/api/s3")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "S3 Operations", description = "File management operations using S3-compatible storage")
public class S3Resource {

    private static final Logger logger = Logger.getLogger(S3Resource.class.getName());

    @Inject
    S3Service s3Service;

    /**
     * List all available bucket names
     * @return List of bucket names
     */
    @GET
    @Path("/buckets")
    @Operation(summary = "List all S3 buckets", description = "Retrieve a list of all available S3 buckets")
    @APIResponse(responseCode = "200", description = "Successfully retrieved bucket list",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = BucketListResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = ErrorResponse.class)))
    @RolesAllowed({"user", "admin"})
    public Response listBuckets() {
        try {
            List<String> bucketNames = s3Service.getBucketNames();
            return Response.ok(new BucketListResponse(bucketNames)).build();
        } catch (Exception e) {
            logger.severe("Failed to list buckets: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve bucket list: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Generate a presigned URL for downloading a file from S3
     * Supports both separate bucket+key parameters and full S3 paths (s3a://bucket/key)
     * @param key The S3 object key (file path) or full S3 path (s3a://bucket/key)
     * @param bucketName Optional bucket name (defaults to configured default bucket if not specified)
     * @param path Alternative parameter for full S3 path (s3a://bucket/key)
     * @return Presigned URL response
     */
    @GET
    @Path("/presigned-url/download")
    @Operation(summary = "Generate download presigned URL", 
               description = "Generate a temporary presigned URL for downloading a file from S3 storage. " +
                           "Supports both 'key + bucketName' parameters and full S3 paths like 's3a://bucket/key'.")
    @APIResponse(responseCode = "200", description = "Successfully generated presigned URL",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = PresignedUrlResponse.class)))
    @APIResponse(responseCode = "400", description = "Bad request - invalid key format",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = ErrorResponse.class)))
    @RolesAllowed({"user", "admin"})
    public Response generateDownloadPresignedUrl(
            @Parameter(description = "S3 object key (file path) or full S3 path (s3a://bucket/key)")
            @QueryParam("key") String key,
            @Parameter(description = "Bucket name (required)", required = true)
            @QueryParam("bucketName") String bucketName,
            @Parameter(description = "Full S3 path (s3a://bucket/key) - alternative to key+bucketName")
            @QueryParam("path") String path) {
        
        // Determine which parameter to use for path resolution
        String pathToResolve = null;
        if (path != null && !path.trim().isEmpty()) {
            pathToResolve = path.trim();
        } else if (key != null && !key.trim().isEmpty()) {
            pathToResolve = key.trim();
        }
        
        if (pathToResolve == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Either 'key' or 'path' parameter is required"))
                    .build();
        }

        // Validate bucketName is provided
        if (bucketName == null || bucketName.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("bucketName parameter is required"))
                    .build();
        }

        try {
            // Parse the path to extract bucket and key
            S3PathParser.S3PathInfo pathInfo = S3PathParser.parseBucketAndKey(bucketName.trim(), pathToResolve, "gdp");
            String resolvedBucketName = pathInfo.getBucketId();
            String resolvedKey = pathInfo.getKey();
            
            if (!S3Utils.isValidS3Key(resolvedKey)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid S3 object key format: " + resolvedKey))
                        .build();
            }

            URL presignedUrl = s3Service.generateDownloadSignedUrl(resolvedBucketName, resolvedKey, 
                    java.time.Duration.ofMinutes(60));
            logger.info("Generated download presigned URL for key: " + resolvedKey + " in bucket: " + resolvedBucketName);
            return Response.ok(new PresignedUrlResponse(presignedUrl.toString(), "download", resolvedKey, resolvedBucketName))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid path format: " + pathToResolve + ", error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid path format: " + e.getMessage()))
                    .build();
        } catch (S3Exception e) {
            logger.severe("Failed to generate download presigned URL for path: " + pathToResolve + 
                    ", error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate presigned URL: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Generate a presigned URL for uploading a file to S3
     * Supports both separate bucket+key parameters and full S3 paths (s3a://bucket/key)
     * @param request Upload presigned URL request
     * @return Presigned URL response
     */
    @POST
    @Path("/presigned-url/upload")
    @Operation(summary = "Generate upload presigned URL", 
               description = "Generate a temporary presigned URL for uploading a file to S3 storage. " +
                           "Supports both 'key + bucketId' parameters and full S3 paths like 's3a://bucket/key'.")
    @APIResponse(responseCode = "200", description = "Successfully generated presigned URL",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = PresignedUrlResponse.class)))
    @APIResponse(responseCode = "400", description = "Bad request - invalid request data",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                 schema = @Schema(implementation = ErrorResponse.class)))
    @RolesAllowed({"user", "admin"})
    public Response generateUploadPresignedUrl(
            @Parameter(description = "Upload request containing key, content type, and bucket ID", required = true)
            UploadPresignedUrlRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Request body is required"))
                    .build();
        }

        // Determine which parameter to use for path resolution
        String pathToResolve = null;
        if (request.getPath() != null && !request.getPath().trim().isEmpty()) {
            pathToResolve = request.getPath().trim();
        } else if (request.getKey() != null && !request.getKey().trim().isEmpty()) {
            pathToResolve = request.getKey().trim();
        }
        
        if (pathToResolve == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Either 'key' or 'path' field is required"))
                    .build();
        }

        // Validate bucketName is provided
        if (request.getBucketName() == null || request.getBucketName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("bucketName field is required"))
                    .build();
        }

        try {
            // Parse the path to extract bucket and key
            S3PathParser.S3PathInfo pathInfo = S3PathParser.parseBucketAndKey(
                request.getBucketName().trim(), pathToResolve, "gdp");
            String resolvedBucketName = pathInfo.getBucketId();
            String resolvedKey = pathInfo.getKey();
            
            if (!S3Utils.isValidS3Key(resolvedKey)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid S3 object key format: " + resolvedKey))
                        .build();
            }

            // Determine content type
            String contentType = request.getContentType();
            if (contentType == null || contentType.trim().isEmpty()) {
                contentType = S3Utils.determineContentType(resolvedKey);
            }

            URL presignedUrl = s3Service.generateUploadSignedUrl(resolvedBucketName, resolvedKey, contentType, 
                    java.time.Duration.ofMinutes(60));
            logger.info("Generated upload presigned URL for key: " + resolvedKey + 
                    ", contentType: " + contentType + ", bucket: " + resolvedBucketName);
            return Response.ok(new PresignedUrlResponse(presignedUrl.toString(), "upload", resolvedKey, contentType, resolvedBucketName))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid path format: " + pathToResolve + ", error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid path format: " + e.getMessage()))
                    .build();
        } catch (S3Exception e) {
            logger.severe("Failed to generate upload presigned URL for path: " + pathToResolve + 
                    ", error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate presigned URL: " + e.getMessage()))
                    .build();
        }
    }

    // Request/Response DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request for generating upload presigned URL")
    public static class UploadPresignedUrlRequest {
        @Schema(description = "S3 object key (file path) or full S3 path (s3a://bucket/key)", example = "documents/file.pdf")
        private String key;
        @Schema(description = "MIME content type of the file", example = "application/pdf")
        private String contentType;
        @Schema(description = "Bucket name (required)", example = "gdp", required = true)
        private String bucketName;
        @Schema(description = "Full S3 path (s3a://bucket/key) - alternative to key+bucketName", example = "s3a://mybucket/documents/file.pdf")
        private String path;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing presigned URL for S3 operations")
    public static class PresignedUrlResponse {
        @Schema(description = "The presigned URL", required = true, example = "https://s3.amazonaws.com/bucket/key?signature=...")
        private String url;
        @Schema(description = "Operation type (upload/download)", required = true, example = "download")
        private String operation;
        @Schema(description = "S3 object key", required = true, example = "documents/file.pdf")
        private String key;
        @Schema(description = "MIME content type", example = "application/pdf")
        private String contentType;
        @Schema(description = "Bucket name", required = true, example = "gdp")
        private String bucketName;
        @Schema(description = "URL expiration time in minutes", example = "60")
        private long expiresInMinutes;

        public PresignedUrlResponse(String url, String operation, String key) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.bucketName = "gdp";
            this.expiresInMinutes = 60; // Default from S3Config
        }
        
        public PresignedUrlResponse(String url, String operation, String key, String bucketName) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.bucketName = bucketName;
            this.expiresInMinutes = 60; // Default from S3Config
        }

        public PresignedUrlResponse(String url, String operation, String key, String contentType, String bucketName) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.contentType = contentType;
            this.bucketName = bucketName;
            this.expiresInMinutes = 60; // Default from S3Config
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing list of available S3 buckets")
    public static class BucketListResponse {
        @Schema(description = "List of bucket names", required = true, example = "[\"gdp\", \"reports\", \"archives\"]")
        private List<String> bucketNames;
    }

    @Data
    @Schema(description = "Error response containing error details")
    public static class ErrorResponse {
        @Schema(description = "Error message", required = true, example = "Invalid S3 object key format")
        private String error;
        @Schema(description = "Timestamp when error occurred", required = true, example = "1692518400000")
        private long timestamp;

        public ErrorResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}