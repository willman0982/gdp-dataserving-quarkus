package com.sc.gdp.dataserving.rest;

import com.sc.gdp.common.s3.S3Service;
import com.sc.gdp.common.s3.S3Utils;
import com.sc.gdp.common.s3.S3Exception;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URL;
import java.util.logging.Logger;

/**
 * REST API resource for S3 operations
 */
@Path("/api/s3")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class S3Resource {

    private static final Logger logger = Logger.getLogger(S3Resource.class.getName());

    @Inject
    S3Service s3Service;

    /**
     * Generate a presigned URL for downloading a file from S3
     * @param key The S3 object key (file path)
     * @return Presigned URL response
     */
    @GET
    @Path("/presigned-url/download")
    @RolesAllowed({"user", "admin"})
    public Response generateDownloadPresignedUrl(@QueryParam("key") String key) {
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
            URL presignedUrl = s3Service.generateDownloadSignedUrl(key);
            logger.info("Generated download presigned URL for key: " + key);
            return Response.ok(new PresignedUrlResponse(presignedUrl.toString(), "download", key))
                    .build();
        } catch (S3Exception e) {
            logger.severe("Failed to generate download presigned URL for key: " + key + ", error: " + e.getMessage());
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
    @RolesAllowed({"user", "admin"})
    public Response generateUploadPresignedUrl(UploadPresignedUrlRequest request) {
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

        try {
            URL presignedUrl = s3Service.generateUploadSignedUrl(key, contentType);
            logger.info("Generated upload presigned URL for key: " + key + ", contentType: " + contentType);
            return Response.ok(new PresignedUrlResponse(presignedUrl.toString(), "upload", key, contentType))
                    .build();
        } catch (S3Exception e) {
            logger.severe("Failed to generate upload presigned URL for key: " + key + ", error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate presigned URL: " + e.getMessage()))
                    .build();
        }
    }

    // Request/Response DTOs
    public static class UploadPresignedUrlRequest {
        private String key;
        private String contentType;

        public UploadPresignedUrlRequest() {}

        public UploadPresignedUrlRequest(String key, String contentType) {
            this.key = key;
            this.contentType = contentType;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    public static class PresignedUrlResponse {
        private String url;
        private String operation;
        private String key;
        private String contentType;
        private long expiresInMinutes;

        public PresignedUrlResponse() {}

        public PresignedUrlResponse(String url, String operation, String key) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.expiresInMinutes = 60; // Default from S3Config
        }

        public PresignedUrlResponse(String url, String operation, String key, String contentType) {
            this.url = url;
            this.operation = operation;
            this.key = key;
            this.contentType = contentType;
            this.expiresInMinutes = 60; // Default from S3Config
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public long getExpiresInMinutes() {
            return expiresInMinutes;
        }

        public void setExpiresInMinutes(long expiresInMinutes) {
            this.expiresInMinutes = expiresInMinutes;
        }
    }

    public static class ErrorResponse {
        private String error;
        private long timestamp;

        public ErrorResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}