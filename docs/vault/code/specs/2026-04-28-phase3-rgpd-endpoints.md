---
type: spec-summary
slug: 2026-04-28-phase3-rgpd-endpoints
original_type: spec
status: ready
source: ../../specs/2026-04-28-phase3-rgpd-endpoints
---

# Spec — 2026-04-28-phase3-rgpd-endpoints

Source : [[../../specs/2026-04-28-phase3-rgpd-endpoints]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/server/routers/me|server/routers/me.py]] — symbols: `router`, `export_request`, `export_confirm`, `erase_request`, `erase_confirm`, `audit_log_my_account`
- [[../code/server/security/rgpd|server/security/rgpd.py]] — symbols: `build_user_export_zip`, `erase_user_cascade`, `_anonymize_auth_events`, `_safe_audit_event`, `EraseStats`
- [[../code/server/main|server/main.py]] — symbols: `app`

### Tests
- [[../code/tests/server/test_me_export|tests/server/test_me_export.py]] — classes: `TestExportRequestReauth`, `TestExportConfirmZip`, `TestExportContent`, `TestExportRateLimit`, `TestUserIsolation`, `TestExportRaceWithErase`
- [[../code/tests/server/test_me_erase|tests/server/test_me_erase.py]] — classes: `TestErasePreconditions`, `TestEraseCascadeAllTables`, `TestEraseAuthEventsAnonymized`, `TestEraseAntiAccident`, `TestEraseOAuthOnly`, `TestEraseTokenSingleUse`
- [[../code/tests/server/test_me_audit_log|tests/server/test_me_audit_log.py]] — classes: `TestAuditLogScope`, `TestAuditLogAdminFilter`, `TestAuditLogPagination`, `TestAuditLogIpHmac`, `TestAuditLogMetaCap`
