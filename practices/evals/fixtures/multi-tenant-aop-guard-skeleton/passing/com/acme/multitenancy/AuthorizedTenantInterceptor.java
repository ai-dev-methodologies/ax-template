package com.acme.multitenancy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Service-boundary AOP guard. Runs around every method annotated with
 * {@link AuthorizedTenant} and rejects requests whose
 * TenantContext.current() does not match the resource's tenant_id
 * (the parameter annotated with {@link TenantId}).
 *
 * Failure modes (load-bearing, exhaustive):
 *   - TenantContext empty           -> TenantContextMissingException (500)
 *   - parameter type not UUID        -> IllegalStateException on first call
 *   - missing @TenantId param        -> IllegalStateException (misuse)
 *   - context.UUID != param.UUID     -> TenantBoundaryViolationException (404)
 *   - context.UUID == param.UUID     -> proceed
 *
 * Generic detail message on the cross-tenant branch is mandatory: never
 * include tenant_id or resource id, existence-leakage prevention.
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#aop-guard.interceptor_skeleton
 * with <root> = acme.
 */
@Aspect
@Component
public class AuthorizedTenantInterceptor {

    @Around("@annotation(com.acme.multitenancy.AuthorizedTenant)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID currentTenant = TenantContext.current().orElseThrow(() ->
            new TenantContextMissingException(
                "TenantContext empty at @AuthorizedTenant boundary: "
                    + joinPoint.getSignature().toShortString()));

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        UUID resourceTenant = null;
        for (int i = 0; i < parameters.length; i++) {
            if (hasTenantIdAnnotation(parameters[i])) {
                if (!(args[i] instanceof UUID)) {
                    throw new IllegalStateException(
                        "@TenantId parameter must be UUID; got "
                            + (args[i] == null ? "null" : args[i].getClass().getName())
                            + " in " + method);
                }
                resourceTenant = (UUID) args[i];
                break;
            }
        }

        if (resourceTenant == null) {
            throw new IllegalStateException(
                "@AuthorizedTenant method missing @TenantId parameter: " + method);
        }

        if (!currentTenant.equals(resourceTenant)) {
            throw new TenantBoundaryViolationException("Resource not found");
        }

        return joinPoint.proceed();
    }

    private static boolean hasTenantIdAnnotation(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType() == TenantId.class) {
                return true;
            }
        }
        return false;
    }
}
