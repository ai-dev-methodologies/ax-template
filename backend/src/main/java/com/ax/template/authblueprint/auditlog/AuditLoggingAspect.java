package com.ax.template.authblueprint.auditlog;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

/**
 * Intercepts {@link Audited} methods and records a non-blocking audit log entry.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-RECORD-001 — captures actor + action + resource + outcome + timestamp</li>
 *   <li>AUDIT-RECORD-002 — only invokes {@code save()} (no update path)</li>
 *   <li>AUDIT-RECORD-003 — non-blocking: business method completes even if audit save throws</li>
 * </ul>
 */
@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);
    private static final String UNKNOWN = "unknown";

    private final AuditLogService auditLogService;
    private final AuditLogPiiRedactor piiRedactor;

    public AuditLoggingAspect(AuditLogService auditLogService, AuditLogPiiRedactor piiRedactor) {
        this.auditLogService = auditLogService;
        this.piiRedactor = piiRedactor;
    }

    @Around("@annotation(com.ax.template.authblueprint.auditlog.Audited)")
    public Object aroundAudited(ProceedingJoinPoint joinPoint) throws Throwable {
        Audited audited = audited(joinPoint);
        String resourceId = extractResourceId(joinPoint);
        String actorUserId = currentActorUserId();
        String actorIp = piiRedactor.redactIp(currentActorIp());
        String userAgent = currentUserAgent();

        AuditOutcome outcome = AuditOutcome.SUCCESS;
        Throwable thrown = null;
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            outcome = AuditOutcome.FAILURE;
            thrown = t;
            result = null;
        }

        // Record the audit log entry — failures here MUST NOT block the business
        // method (AUDIT-RECORD-003). The service uses REQUIRES_NEW transaction
        // propagation so an audit-write failure does not roll back the caller.
        try {
            auditLogService.record(
                AuditLog.builder()
                    .id(UUID.randomUUID())
                    .actorUserId(actorUserId)
                    .actorIp(actorIp)
                    .action(audited.action())
                    .resourceType(audited.resourceType())
                    .resourceId(resourceId)
                    .outcome(outcome)
                    .timestamp(Instant.now())
                    .userAgent(userAgent)
                    .build()
            );
        } catch (Exception ex) {
            log.error("audit-log persistence failed (non-blocking) action={} resource={}:{}",
                audited.action(), audited.resourceType(), resourceId, ex);
        }

        if (thrown != null) throw thrown;
        return result;
    }

    private Audited audited(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        return method.getAnnotation(Audited.class);
    }

    /**
     * Resource ID extraction strategy (manifest aop_wiring.resource_id_extraction):
     * <ol>
     *   <li>Parameter annotated with {@link ResourceId}</li>
     *   <li>{@code getId()} method on the first argument</li>
     *   <li>An {@code id} field on the first argument</li>
     *   <li>{@code "unknown"}</li>
     * </ol>
     */
    private String extractResourceId(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation a : paramAnnotations[i]) {
                if (a instanceof ResourceId && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
        }

        if (args.length > 0 && args[0] != null) {
            Object first = args[0];
            try {
                Method getId = first.getClass().getMethod("getId");
                Object v = getId.invoke(first);
                if (v != null) return String.valueOf(v);
            } catch (NoSuchMethodException ignored) {
                // fall through
            } catch (Exception ignored) {
                // fall through
            }
            try {
                var idField = first.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object v = idField.get(first);
                if (v != null) return String.valueOf(v);
            } catch (NoSuchFieldException ignored) {
                // fall through
            } catch (Exception ignored) {
                // fall through
            }
        }

        return UNKNOWN;
    }

    private String currentActorUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return (name == null || name.isBlank() || "anonymousUser".equals(name)) ? null : name;
    }

    private String currentActorIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Use the RIGHTMOST entry — the hop appended by the immediate trusted
            // proxy. The leftmost X-Forwarded-For value is fully client-controllable
            // (spoofable); only the right end is written by infrastructure you trust.
            // Multi-proxy deployments MUST count trusted hops from the right, or use
            // Spring's server.forward-headers-strategy / ForwardedHeaderFilter, to
            // resolve the true client. Never trust the leftmost value for a security IP.
            int lastComma = forwarded.lastIndexOf(',');
            return lastComma >= 0 ? forwarded.substring(lastComma + 1).trim() : forwarded.trim();
        }
        return req.getRemoteAddr();
    }

    private String currentUserAgent() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
