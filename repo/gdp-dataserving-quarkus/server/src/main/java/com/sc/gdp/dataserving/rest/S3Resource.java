package com.sc.gdp.dataserving.rest;

import com.sc.gdp.common.s3.S3Service;
import com.sc.gdp.common.s3.S3Utils;
import com.sc.gdp.common.s3.S3Exception;

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
     * List all available bucket IDs
     * @return List of bucket IDs
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
        List<String> bucketIds = s3Service.getBucketIds();
        return Response.ok(new BucketListResponse(bucketIds)).build();
    }

    /**
     * Generate a presigned URL for downloading a file from S3
     * @param key The S3 object key (file path)
     * @param bucketId Optional bucket ID (defaults to "default" if not specified)
     * @return Presigned URL response
     */
    @GET
    @Path("/presigned-url/download")
    @Operation(summary = "Generate download presigned URL", 
               description = "Generate a temporary presigned URL for downloading a file from S3 storage")
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
            @Parameter(description = "S3 object key (file path)", required = true)
            @QueryParam("key") String key,
            @Parameter(description = "Bucket ID (defaults to 'default')")
            @QueryParam("bucketId") @DefaultValue("default") String bucketId) {
        
        if (key == null || key.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("S3 object key is required"))
                    .build();
        }

        if (!S3Utils.isValidS3Key(key)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid S3 object key format"))
                    .build();
        }

        try {
            URL presignedUrl = s3Service.generateDownloadSignedUrl(bucketId, key, 
                    java.time.Duration.ofMinutes(60));
            logger.info("Generated download presigned URL for key: " + key + " in bucket: " + bucketId);
            return Response.ok(new PresignedUrlResponse(presignedUrl.toString(), "download", key, bucketId))
                    .build();
        } catch (S3Exception e) {
            logger.severe("Failed to generate download presigned URL for key: " + key + 
                    " in bucket: " + bucketId + ", error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate presigned URL: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Generate a presigned URL for uploading a file to S3
     * @param request Upload presigned URL request
     * @return Presigned URL response
     */
    @POST
    @Path("/presigned-url/upload")
    @Operation(summary = "Generate upload presigned URL", 
               description = "Generate a temporary presigned URL for uploading a file to S3 storage")
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
        if (request == null || request.getKey() == null || request.getKey().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("S3 object key is required"))
                    .build();
        }

        String key = request.getKey().trim();
        if (!S3Utils.isValidS3Key(key)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid S3 object key format"))
                    .build();
        }

        // Determine content type
        String contentType = request.getContentType();
        if (contentType == null || contentType.trim().isEmpty()) {
            contentType = S3Utils.determineContentType(key);
        }
        
        // Get bucket ID (default to "default" if not specified)
        String bucketId = request.getBucketId();
        if (bucketId == null || bucketId.trim().isEmpty()) {
            bucketId = "default";
        }

        try {
            URL presignedUrl = s3Service.generateUploadSignedUrl(bucketId, key, contentType, 
                    java.time.Duration.ofMinutes(60));
            logger.info("Generated upload presigned URL for key: " + key + 
                    ", contentType: " + contentType + ", bucket: " + bucketId);
            return Response.ok(new PresignedUrlResponse(presignedUrl.toString(), "upload", key, contentType, bucketId))
                    .build();
        } catch (S3Exception e) {
            logger.severe("Failed to generate upload presigned URL for key: " + key + 
                    ", bucket: " + bucketId + ", error: " + e.getMessage());
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
        @Schema(description = "S3 object key (file path)", required = true, example = "documents/file.pdf")
        private String key;
        @Schema(description = "MIME content type of the file", example = "application/pdf")
        private String contentType;
        @Schema(description = "Bucket ID (defaults to 'default')", example = "default")
        private String bucketId;
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
        @Schema(description = "Bucket ID", required = true, example = "default")
        private String bucketId;
        @Schema(description = "URL expiration time in minutes", example = "60")
        private long expiresInMinutes;

        public PresignedUrlResponse(String url, String operation, String key) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.bucketId = "default";
            this.expiresInMinutes = 60; // Default from S3Config
        }
        
        public PresignedUrlResponse(String url, String operation, String key, String bucketId) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.bucketId = bucketId;
            this.expiresInMinutes = 60; // Default from S3Config
        }

        public PresignedUrlResponse(String url, String operation, String key, String contentType, String bucketId) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.contentType = contentType;
            this.bucketId = bucketId;
            this.expiresInMinutes = 60; // Default from S3Config
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing list of available S3 buckets")
    public static class BucketListResponse {
        @Schema(description = "List of bucket IDs", required = true, example = "[\"default\", \"reports\", \"archives\"]")
        private List<String> bucketIds;
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