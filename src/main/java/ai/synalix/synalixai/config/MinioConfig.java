package ai.synalix.synalixai.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO client configuration
 * Configures the MinIO client and ensures required buckets exist
 */
@Configuration
@Slf4j
@Getter
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket.datasets}")
    private String datasetsBucket;

    @Value("${minio.bucket.checkpoints}")
    private String checkpointsBucket;

    @Value("${minio.presigned-url-expiry}")
    private int presignedUrlExpiry;

    /**
     * Creates and configures the MinIO client bean
     * Also initializes required buckets on startup
     *
     * @return configured MinioClient instance
     */
    @Bean
    public MinioClient minioClient() {
        var client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        initBuckets(client);
        
        return client;
    }

    /**
     * Initializes required buckets on application startup
     * Creates buckets if they don't exist
     *
     * @param client the MinIO client
     */
    private void initBuckets(MinioClient client) {
        try {
            createBucketIfNotExists(client, datasetsBucket);
            createBucketIfNotExists(client, checkpointsBucket);
            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinIO buckets: {}", e.getMessage());
            // Don't throw exception to allow application to start even if MinIO is unavailable
            // The error will be handled when actual operations are attempted
        }
    }

    /**
     * Creates a bucket if it doesn't already exist
     *
     * @param client     MinIO client
     * @param bucketName name of the bucket to create
     */
    private void createBucketIfNotExists(MinioClient client, String bucketName) {
        try {
            var exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.debug("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to create bucket {}: {}", bucketName, e.getMessage());
        }
    }
}