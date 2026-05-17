/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLoggingAspect
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-RECORD-001
 *               specs/audit-log-l0.yaml#AUDIT-RECORD-003
 *               blueprints/audit-log-manifest.yaml#aop_wiring
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — Aspect-Oriented Programming with Spring (@Around, ProceedingJoinPoint)"
 *     url: "https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html"
 *   - source_type: external
 *     citation: "Spring Security Reference — SecurityContextHolder.getContext().getAuthentication()"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Annotate service methods with @Audited(action="CREATE", resourceType="payment")
 *   to automatically record an audit log entry on each invocation.
 *   The aspect extracts actorId from SecurityContext and actorIp from RequestContextHolder.
 *
 *   @Audited annotation must be placed on service methods (not controllers) to capture
 *   the business operation level, not the HTTP level.
 */
package com.example.app.auditlog;

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

import java.lang.annotation.*;
import java.lang.reflect.Method;

/**
 * AuditLoggingAspect — AOP interceptor for methods annotated with {@link Audited}.
 *
 * <p>On each intercepted method invocation:
 * <ol>
 *   <li>Proceeds with the original method call.
 *   <li>On SUCCESS: records an audit entry with outcome=SUCCESS.
 *   <li>On FAILURE (exception): records an audit entry with outcome=FAILURE,
 *       then re-throws the exception so the caller sees the original error.
 *   <li>If audit persistence itself fails: swallows the exception, logs at ERROR.
 *       The original result/exception is still propagated (AUDIT-RECORD-003).
 * </ol>
 *
 * <p>Resource ID extraction (blueprints/audit-log-manifest.yaml#aop_wiring):
 * The aspect checks method arguments for a field named "id" or a method "getId()".
 * Annotate the parameter with {@link ResourceId} for explicit extraction.
 * Falls back to "unknown" if no ID can be determined.
 */
@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

    private final AuditLogService auditLogService;

    public AuditLoggingAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Intercepts all methods annotated with {@link Audited}.
     */
    @Around("@annotation(com.example.app.auditlog.AuditLoggingAspect.Audited)")
    public Object auditMethod(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        String actorId = resolveActorId();
        String actorIp = resolveActorIp();
        String correlationId = resolveCorrelationId();
        String resourceId = resolveResourceId(pjp, audited.resourceIdParam());

        Throwable failure = null;
        try {
            Object result = pjp.proceed();
            recordAudit(audited, actorId, actorIp, correlationId, resourceId,
                AuditLog.Outcome.SUCCESS, null);
            return result;
        } catch (Throwable t) {
            failure = t;
            recordAudit(audited, actorId, actorIp, correlationId, resourceId,
                AuditLog.Outcome.FAILURE, t);
            throw t;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void recordAudit(Audited audited, String actorId, String actorIp,
                             String correlationId, String resourceId,
                             AuditLog.Outcome outcome, Throwable failure) {
        try {
            AuditLog entry = AuditLog.builder()
                .actorId(actorId)
                .actorIp(actorIp)
                .action(audited.action())
                .resourceType(audited.resourceType())
                .resourceId(resourceId)
                .outcome(outcome)
                .correlationId(correlationId)
                .build();
            auditLogService.record(entry);
        } catch (Exception e) {
            // Non-blocking: swallow audit persistence failures (AUDIT-RECORD-003)
            log.error("Failed to record audit log: action={} resource={}/{} actor={}",
                audited.action(), audited.resourceType(), resourceId, actorId, e);
        }
    }

    private String resolveActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String resolveActorIp() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveCorrelationId() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String corr = attrs.getRequest().getHeader("X-Correlation-Id");
            return corr != null ? corr : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveResourceId(ProceedingJoinPoint pjp, String resourceIdParam) {
        if (!resourceIdParam.isBlank()) {
            // Explicit param name hint — scan method args for @ResourceId
        }
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return "unknown";

        // Try first arg: look for getId() method or 'id' field
        Object firstArg = args[0];
        if (firstArg == null) return "unknown";
        try {
            Method getId = firstArg.getClass().getMethod("getId");
            Object id = getId.invoke(firstArg);
            return id != null ? id.toString() : "unknown";
        } catch (Exception ignored) {}

        // If first arg is a String or UUID, use directly
        return firstArg.toString();
    }

    // ─── Annotations ─────────────────────────────────────────────────────────

    /**
     * Marks a service method for automatic audit logging.
     *
     * <pre>{@code
     * @Audited(action = "CREATE", resourceType = "payment")
     * public PaymentResponse createPayment(CreatePaymentRequest request) { ... }
     * }</pre>
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Audited {
        /** Action verb (CREATE, UPDATE, DELETE, LOGIN, EXPORT, etc.). */
        String action();

        /** Resource type (payment, user, item, etc.). */
        String resourceType();

        /**
         * Optional hint: parameter name containing the resource ID.
         * If blank, the aspect auto-detects getId() on the first argument.
         */
        String resourceIdParam() default "";
    }

    /**
     * Marks a method parameter as the source of the resource ID for audit logging.
     *
     * <pre>{@code
     * @Audited(action = "DELETE", resourceType = "item")
     * public void deleteItem(@ResourceId Long itemId) { ... }
     * }</pre>
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface ResourceId {}
}
