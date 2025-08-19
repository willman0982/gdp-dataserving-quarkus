package com.sc.gdp.common.s3;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration mapping for S3 settings
 */
@ConfigMapping(prefix = "s3")
public interface S3Config {

    /**
     * S3 endpoint URL (for S3-compatible services like Isilon)
     */
    @WithName("endpoint-url")
    String endpointUrl();

    /**
     * S3 access key
     */
    @WithName("access-key")
    String accessKey();

    /**
     * S3 secret key
     */
    @WithName("secret-key")
    String secretKey();

    /**
     * S3 region
     */
    @WithDefault("us-east-1")
    String region();

    /**
     * S3 bucket name
     */
    @WithName("bucket-name")
    String bucketName();

    /**
     * Whether to use path-style access
     */
    @WithName("path-style-access")
    @WithDefault("true")
    boolean pathStyleAccess();

    /**
     * Signed URL duration in minutes
     */
    @WithName("signed-url-duration-minutes")
    @WithDefault("60")
    int signedUrlDurationMinutes();

    /**
     * Connection timeout in milliseconds
     */
    @WithName("connection-timeout-ms")
    @WithDefault("30000")
    int connectionTimeoutMs();

    /**
     * Socket timeout in milliseconds
     */
    @WithName("socket-timeout-ms")
    @WithDefault("30000")
    int socketTimeoutMs();

    /**
     * Maximum retry attempts
     */
    @WithName("max-retry-attempts")
    @WithDefault("3")
    int maxRetryAttempts();
}