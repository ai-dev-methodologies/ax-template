# Draft File: blueprints/auth-manifest.yaml

```yaml
template_id: auth-blueprint
family: scaffold
status: draft
confidence: medium
stack:
  frontend: react
  backend: spring-boot
  contract: openapi
  repo_shape: single-repo

when_to_use:
  - new project
  - auth/security baseline needed
  - React + Spring Boot single repo로 시작할 때

not_for:
  - brownfield migration
  - enterprise SSO platform replacement
  - broad identity platform

provider_policy:
  enabled_candidates:
    - google
    - kakao
    - email
  disabled_behavior: structured_error
  schema_is_static: true

auth_state:
  access_token:
    delivery: response_body_or_memory
    storage: in_memory
  refresh_token:
    delivery: http_only_cookie
    storage: stateful
    rotation: grace_window
  me_endpoint:
    path: /auth/me
    returns:
      - user_id
      - email
      - roles
      - provider_links
      - verification_state

rbac:
  roles:
    - admin
    - manager
    - member
  role_management_out_of_scope: true

account_linking:
  mode: explicit
  auto_merge_by_email: false
  required_when_same_email_different_provider: true

verification_policy:
  email_verification_required: true
  unverified_state_separated: true
  resend_supported: true
  expiry_notice_required: true
  idempotent_verify: true

rate_limits:
  login:
    limit: 5
    window: 15m
    key: ip_plus_identifier
  resend_verification:
    limit: 3
    window: 10m
    key: email

security_defaults:
  csrf: required
  cors: required
  hardcoded_secrets: reject
  custom_jwt_filter_default: reject
  stateless_refresh_token_default: reject

testing_baseline:
  backend_integration: required
  frontend_auth_state: required
  verify_triplet: required
  key_flow_e2e: required
  real_oauth_browser_e2e_v1: excluded

verification_checkpoints:
  - security
  - contract
  - rbac

must_not:
  - business_logic_in_controllers
  - custom_jwt_filter_as_default
  - stateless_refresh_token
  - fail_open_verify
  - provider_schema_split_by_flag

reject_if:
  - missing_csrf_cors_defaults
  - missing_account_linking_policy
  - missing_rate_limits
  - missing_unverified_state_handling
  - refresh_without_server_side_state
  - contract_not_source_of_truth

source_precedence:
  1: latest_official_docs
  2: approved_github_refs
  3: practical_reference_refs

official_doc_refs:
  - spring.io/spring-security
  - spring.io/spring-boot
  - react.dev
  - swagger.io/specification

approved_github_refs:
  - spring-authorization-server/samples
  - jhipster generated output reference
  - baeldung spring security examples
  - orval

practical_refs:
  - full-stack-fastapi-template (react/client pattern only)
  - ixartz/SaaS-Boilerplate (ux/testing pattern only)

last_reviewed_at: null
```
