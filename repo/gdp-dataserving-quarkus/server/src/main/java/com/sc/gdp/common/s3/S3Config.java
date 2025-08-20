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
     * Additional bucket configurations
     * Map of bucket-id to BucketConfig (bucket-id is used as configuration key)
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
        @WithName("bucket-name")
        String bucketName();
        
        /**
         * Optional endpoint URL specific to this bucket
         */
        @WithName("endpoint-url")
        Optional<String> endpointUrl();
        
        /**
         * Optional region specific to this bucket
         */
        @WithDefault("us-east-1")
        String region();
        
        /**
         * Optional access key specific to this bucket
         */
        @WithName("access-key")
        Optional<String> accessKey();
        
        /**
         * Optional secret key specific to this bucket
         */
        @WithName("secret-key")
        Optional<String> secretKey();
        
        /**
         * Whether to use path-style access for this bucket
         */
        @WithName("path-style-access")
        @WithDefault("true")
        boolean pathStyleAccess();
        
        /**
         * Signed URL duration in minutes for this bucket
         */
        @WithName("signed-url-duration-minutes")
        @WithDefault("60")
        int signedUrlDurationMinutes();
        
        /**
         * Connection timeout in milliseconds for this bucket
         */
        @WithName("connection-timeout-ms")
        @WithDefault("30000")
        int connectionTimeoutMs();
        
        /**
         * Socket timeout in milliseconds for this bucket
         */
        @WithName("socket-timeout-ms")
        @WithDefault("30000")
        int socketTimeoutMs();
        
        /**
         * Maximum retry attempts for this bucket
         */
        @WithName("max-retry-attempts")
        @WithDefault("3")
        int maxRetryAttempts();
    }
}