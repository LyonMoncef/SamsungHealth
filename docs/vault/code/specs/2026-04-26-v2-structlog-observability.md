---
type: spec-summary
slug: 2026-04-26-v2-structlog-observability
original_type: spec
status: delivered
source: ../../specs/2026-04-26-v2-structlog-observability
---

# Spec — 2026-04-26-v2-structlog-observability

Source : [[../../specs/2026-04-26-v2-structlog-observability]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/server/logging_config|server/logging_config.py]] — symbols: `configure_logging`, `get_logger`, `_processors`
- [[../code/server/middleware/request_context|server/middleware/request_context.py]] — symbols: `RequestContextMiddleware`, `request_id_var`, `user_id_var`
- [[../code/server/main|server/main.py]] — symbols: `app`, `lifespan`
- [[../code/requirements.txt|requirements.txt]] — symbols: `structlog`

### Tests
- [[../code/tests/server/test_logging_config|tests/server/test_logging_config.py]] — classes: `TestStructlogConfig`, `TestLogFields` · methods: `test_logger_emits_json`, `test_logger_includes_timestamp_iso8601`, `test_logger_includes_level`, `test_logger_includes_scope`, `test_log_level_from_env`, `test_pretty_renderer_in_dev_mode`
- [[../code/tests/server/test_request_context_middleware|tests/server/test_request_context_middleware.py]] — classes: `TestRequestContext`, `TestRequestIdHeader`, `TestLatency` · methods: `test_request_id_generated_when_absent`, `test_request_id_propagated_from_header`, `test_request_id_present_in_response_header`, `test_request_id_bound_to_logs`, `test_user_id_default_none`, `test_latency_ms_logged_on_request_complete`, `test_route_template_logged`
