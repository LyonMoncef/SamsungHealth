---
type: spec-summary
slug: 2026-04-26-v2-auth-foundation
original_type: spec
status: delivered
source: ../../specs/2026-04-26-v2-auth-foundation
---

# Spec — 2026-04-26-v2-auth-foundation

Source : [[../../specs/2026-04-26-v2-auth-foundation]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/server/db/models|server/db/models.py]] — symbols: `User`, `RefreshToken`, `AuthEvent`
- [[../code/server/security/auth|server/security/auth.py]] — symbols: `hash_password`, `verify_password`, `_DUMMY_HASH`, `create_access_token`, `create_refresh_token`, `decode_access_token`, `rotate_refresh_token`, `revoke_refresh_token`, `get_current_user`, `_validate_jwt_secret_at_boot`, `_validate_registration_token`
- [[../code/server/security/redaction|server/security/redaction.py]] — symbols: `redact_sensitive_keys`, `_SENSITIVE_KEYS`
- [[../code/server/routers/auth|server/routers/auth.py]] — symbols: `router`, `register`, `login`, `refresh`, `logout`
- [[../code/server/routers/sleep|server/routers/sleep.py]] — symbols: `router`, `list_sleep`, `get_sleep_session`
- [[../code/server/routers/heartrate|server/routers/heartrate.py]] — symbols: `router`
- [[../code/server/routers/steps|server/routers/steps.py]] — symbols: `router`
- [[../code/server/routers/exercise|server/routers/exercise.py]] — symbols: `router`
- [[../code/server/routers/mood|server/routers/mood.py]] — symbols: `router`
- [[../code/server/main|server/main.py]] — symbols: `app`, `lifespan`
- [[../code/server/logging_config|server/logging_config.py]] — symbols: `_build_processors`
- [[../code/alembic/versions/0004_auth_foundation|alembic/versions/0004_auth_foundation.py]] — symbols: `upgrade`, `downgrade`
- [[../code/requirements.txt|requirements.txt]] — symbols: `argon2-cffi`, `PyJWT`

### Tests
- [[../code/tests/server/test_password_hashing|tests/server/test_password_hashing.py]] — classes: `TestArgon2Params`, `TestVerifyPassword`, `TestTimingEqualization` · methods: `test_hash_uses_argon2id`, `test_hash_params_match_rfc9106_profile2`, `test_verify_correct_password`, `test_verify_wrong_password_returns_false`, `test_verify_nonexistent_user_runs_dummy_hash`, `test_verify_constant_time_within_tolerance`
- [[../code/tests/server/test_jwt|tests/server/test_jwt.py]] — classes: `TestJwtSecretValidation`, `TestAccessToken`, `TestRefreshToken`, `TestJwtDecodeFootguns` · methods: `test_jwt_secret_required_at_boot`, `test_jwt_secret_min_256_bits`, `test_jwt_secret_rejects_changeme`, `test_create_access_token_includes_kid`, `test_decode_rejects_alg_none`, `test_decode_requires_explicit_algorithms`, `test_decode_requires_exp_iat_sub_claims`, `test_decode_rejects_missing_iss_aud`, `test_payload_contains_only_sub_no_pii`, `test_previous_secret_decode_only`
- [[../code/tests/server/test_auth_routes|tests/server/test_auth_routes.py]] — classes: `TestRegister`, `TestLogin`, `TestRefresh`, `TestLogout`, `TestUserEnumeration` · methods: `test_register_requires_admin_token`, `test_register_creates_user_returns_201`, `test_register_duplicate_email_409`, `test_register_invalid_token_403`, `test_login_returns_access_and_refresh`, `test_login_wrong_password_401_identical_message`, `test_login_unknown_user_401_identical_message`, `test_login_response_time_constant_within_tolerance`, `test_refresh_rotates_token`, `test_refresh_revoked_token_401`, `test_logout_revokes_refresh`, `test_logout_idempotent`
- [[../code/tests/server/test_health_routes_auth|tests/server/test_health_routes_auth.py]] — classes: `TestHealthRequiresAuth`, `TestUserDataIsolation` · methods: `test_sleep_get_without_token_401`, `test_heartrate_get_without_token_401`, `test_steps_get_without_token_401`, `test_exercise_get_without_token_401`, `test_mood_get_without_token_401`, `test_sleep_post_without_token_401`, `test_user_a_cannot_read_user_b_sleep`, `test_user_a_cannot_read_user_b_mood`, `test_user_a_post_associates_with_user_a_id`
- [[../code/tests/server/test_redaction|tests/server/test_redaction.py]] — classes: `TestRedactionProcessor` · methods: `test_redacts_password_key`, `test_redacts_token_key`, `test_redacts_authorization_key`, `test_redacts_jwt_key`, `test_redacts_secret_key`, `test_redacts_cookie_key`, `test_redaction_case_insensitive`, `test_redaction_nested_dict`, `test_redaction_preserves_non_sensitive_keys`
- [[../code/tests/server/test_auth_events|tests/server/test_auth_events.py]] — classes: `TestAuthEventLogging` · methods: `test_login_success_writes_event`, `test_login_failure_writes_event_with_email_hash_not_plain`, `test_register_writes_event`, `test_refresh_writes_event`
