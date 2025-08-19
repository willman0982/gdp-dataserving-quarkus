
package com.sc.gdp.common.s3;

/**
 * Custom exception for S3 operations
 */
public class S3Exception extends Exception {
    
    public S3Exception(String message) {
        super(message);
    }
    
    public S3Exception(String message, Throwable cause) {
        super(message, cause);
    }
    
    public S3Exception(Throwable cause) {
        super(cause);
    }
}