package com.ax.template.authblueprint.problemdetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stamps a request-entry timestamp so {@link ProblemDemoAdvice} can record an HONEST
 * {@code problem_response_seconds} histogram (PROBLEM-OBSERVABILITY-001) spanning the full
 * request — including body binding and validation, which run before any controller method.
 *
 * <p>Scoped to {@code /api/problem-demo/**} via {@link #shouldNotFilter} so it adds no
 * overhead to other surfaces. Does NOT register a {@link org.springframework.boot.web.servlet.FilterRegistrationBean}
 * auto-registration concern because, as a path-gated {@code OncePerRequestFilter}, it is a
 * no-op everywhere else.
 *
 * <p>Spec: specs/problem-details-l0.yaml#PROBLEM-OBSERVABILITY-001.
 */
@Component
public class ProblemRequestTimer extends OncePerRequestFilter {

    static final String START_NANOS_ATTR = ProblemRequestTimer.class.getName() + ".startNanos";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(START_NANOS_ATTR, System.nanoTime());
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/problem-demo");
    }
}
