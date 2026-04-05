# Chub Freshness Results

## Execution Scope
Running `chub` freshness checks for the planned auth blueprint stack: Spring Security / Spring Boot, React, and OpenAPI-related tooling.

## Commands Run and Results

### 1. Spring / Spring Boot / Spring Security
**Command:**
`chub search "spring-boot" --json`
`chub search "spring-security" --json`
`chub search "spring" --json`

**Result:** No results returned (0 results).
**Status:** `unavailable`

### 2. React
**Command:**
`chub search "react" --json`
`chub get react/react --lang js`

**Result:** React runtime and related packages found. The `react/react` doc was successfully fetched. It returns `react==19.2.4`.
**Status:** `fresh`

### 3. OpenAPI
**Command:**
`chub search "openapi" --json`

**Result:** Returned Python and AWS related packages (e.g., `django/ninja`, `fastapi/package`, `flask/smorest`, `aws/api-gateway`). None of the results are specifically for Spring Boot (like `springdoc-openapi`) or React-specific OpenAPI generation tooling.
**Status:** `unavailable` (for the specific Java/React ecosystem tools needed).

## Summary
- **react**: fresh (v19.2.4)
- **spring-boot**: unavailable
- **spring-security**: unavailable
- **openapi** (springdoc/swagger): unavailable
