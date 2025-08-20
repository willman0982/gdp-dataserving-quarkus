package com.sc.gdp.common.s3;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Map;
import java.util.Optional;

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
     * Default S3 bucket name
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
     * Additional bucket configurations
     * Map of bucket-id to BucketConfig
     */
    @WithName("buckets")
    Map<String, BucketConfig> buckets();
    
    /**
     * Configuration for a specific bucket
     */
    interface BucketConfig {
        /**
         * Bucket name
         */
        @WithName("name")
        String name();
        
        /**
         * Optional endpoint URL specific to this bucket
         */
        Optional<String> endpointUrl();
        
        /**
         * Optional region specific to this bucket
         */
        @WithDefault("us-east-1")
        String region();
        
        /**
         * Optional access key specific to this bucket
         */
        Optional<String> accessKey();
        
        /**
         * Optional secret key specific to this bucket
         */
        Optional<String> secretKey();
        
        /**
         * Whether to use path-style access for this bucket
         */
        @WithDefault("true")
        boolean pathStyleAccess();
    }

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