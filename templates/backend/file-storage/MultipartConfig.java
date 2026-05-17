// @ax-template-meta: template_id=backend/file-storage/MultipartConfig layer=backend domain=file-storage
// evidence: FILE-UPLOAD-002 (max-file-size enforcement at Tomcat layer)
package com.ax.template.authblueprint.filestorage;

import org.springframework.boot.web.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

/**
 * MultipartConfig — Spring Boot multipart upload configuration.
 *
 * <p>Sets the maximum file size and request size for multipart uploads.
 * These limits map to blueprints/file-storage-manifest.yaml#upload.spring_multipart_config.
 *
 * <p>Fork instructions:
 * <ol>
 *   <li>Override with application.properties / application.yml:
 *       {@code spring.servlet.multipart.max-file-size=100MB}
 *       {@code spring.servlet.multipart.max-request-size=105MB}</li>
 *   <li>For large uploads, consider streaming (avoid loading into memory):
 *       use {@code Part} or {@code InputStream} instead of {@code MultipartFile}.</li>
 *   <li>For S3 direct uploads: use presigned POST policies instead of proxying
 *       through the backend (reduces latency and backend memory pressure).</li>
 * </ol>
 *
 * FILE-UPLOAD-002: {@code MultipartException} is mapped to HTTP 413 in GlobalExceptionHandler.
 */
@Configuration
public class MultipartConfig {

    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;   // 100 MB
    private static final long MAX_REQUEST_SIZE_BYTES = 105L * 1024 * 1024; // 105 MB (metadata overhead)

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        var config = new org.springframework.boot.web.servlet.MultipartConfigElement(
                /* location */ "",
                MAX_FILE_SIZE_BYTES,
                MAX_REQUEST_SIZE_BYTES,
                /* fileSizeThreshold */ 0
        );
        return config;
    }

    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
