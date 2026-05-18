/**
 * @ax-template-meta
 * template_id: backend/data/OptimisticLockingPolicy
 * layer: backend-infrastructure
 * domain: data
 * anchors_rule: persistence-optimistic-locking.md (PRACTICES-PERS-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Hibernate ORM 6 User Guide — @Version: a numeric or timestamp version field causes Hibernate to append 'AND version = ?' to every UPDATE/DELETE; concurrent commits with a stale version throw OptimisticLockException"
 *     url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — ObjectOptimisticLockingFailureException wraps JPA OptimisticLockException; handle in service layer with a retry or return HTTP 409 to the client"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Register OptimisticLockingPolicy as a Spring @ControllerAdvice or compose it with
 *   GlobalExceptionHandler to return HTTP 409 Conflict on concurrent modification.
 *   Service methods that update high-contention entities (e.g. wallet balance, inventory)
 *   should catch ObjectOptimisticLockingFailureException and retry up to MAX_RETRIES times.
 */
package com.example.app.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.function.Supplier;

/**
 * Policy class for handling optimistic locking failures across the application.
 *
 * <h3>Two components:</h3>
 * <ol>
 *   <li>{@link #handleOptimisticLockFailure} — {@code @ExceptionHandler} that translates
 *       {@code ObjectOptimisticLockingFailureException} to HTTP 409 Conflict with an
 *       RFC 7807 problem detail body. Register as a {@code @RestControllerAdvice} or
 *       compose into the existing {@code GlobalExceptionHandler}.</li>
 *   <li>{@link #withRetry} — service-layer utility to retry a write operation up to
 *       {@code maxAttempts} times before propagating the exception. Use for high-contention
 *       paths (e.g. wallet balance increment, inventory reservation).</li>
 * </ol>
 *
 * <h3>HTTP contract</h3>
 * A {@code 409 Conflict} response signals to the client that the resource was concurrently
 * modified and the current request should be retried with a fresh read of the resource.
 */
@RestControllerAdvice
public class OptimisticLockingPolicy {

    private static final Logger log = LoggerFactory.getLogger(OptimisticLockingPolicy.class);

    /**
     * Translates a JPA optimistic locking failure to HTTP 409 Conflict.
     *
     * <p>The response body follows RFC 7807 Problem Details. Callers that
     * receive 409 should re-read the resource and retry the operation with the
     * fresh {@code version} value.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockFailure(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure — concurrent modification detected", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("urn:problem:concurrent-modification"));
        pd.setTitle("Concurrent modification");
        pd.setDetail("The resource was modified by another request. Reload and retry.");
        return pd;
    }

    /**
     * Executes the given write operation, retrying on optimistic locking failure.
     *
     * <p>Suitable for high-contention paths where a single retry is preferable
     * to surfacing a 409 to the client. Use sparingly — blind retries can mask
     * application-level concurrency bugs.
     *
     * @param operation   the write operation to attempt (must be idempotent under retry)
     * @param maxAttempts maximum number of attempts (1 = no retry)
     * @param <T>         return type of the operation
     * @return the result of the first successful attempt
     * @throws OptimisticLockingFailureException if all attempts fail
     */
    public static <T> T withRetry(Supplier<T> operation, int maxAttempts) {
        OptimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (OptimisticLockingFailureException ex) {
                last = ex;
                log.debug("Optimistic lock retry {}/{}", attempt, maxAttempts);
            }
        }
        throw last;
    }
}
