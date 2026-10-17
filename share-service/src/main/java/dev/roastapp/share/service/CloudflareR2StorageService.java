package dev.roastapp.share.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Service
public class CloudflareR2StorageService implements R2StorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareR2StorageService.class);

    private final String bucketName;
    private final S3Client s3Client;
    private final boolean configured;

    public CloudflareR2StorageService(
            @Value("${r2.endpoint:}") String endpoint,
            @Value("${r2.access-key-id:}") String accessKeyId,
            @Value("${r2.secret-access-key:}") String secretAccessKey,
            @Value("${r2.bucket-name:roast-cards}") String bucketName) {

        this.bucketName = bucketName;

        if (endpoint != null && !endpoint.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {

            S3Client client = null;
            try {
                client = S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                        .region(Region.of("auto"))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                        .build();
                log.info("Initialized Cloudflare R2 S3Client for bucket '{}'", bucketName);
            } catch (Exception e) {
                log.warn("Failed to initialize Cloudflare R2 client: {}", e.getMessage());
            }
            this.s3Client = client;
            this.configured = (client != null);
        } else {
            log.warn("Cloudflare R2 is not fully configured; cards will be served locally.");
            this.s3Client = null;
            this.configured = false;
        }
    }

    @Override
    public boolean uploadCard(String fileName, byte[] imageBytes) {
        if (!configured || s3Client == null) {
            log.info("Skipping R2 upload for {} (R2 not configured)", fileName);
            return false;
        }

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("image/png")
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(imageBytes));
            log.info("Successfully uploaded card {} to Cloudflare R2 bucket '{}'", fileName, bucketName);
            return true;
        } catch (Exception e) {
            log.error("Failed to upload card {} to Cloudflare R2: {}", fileName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }
}
