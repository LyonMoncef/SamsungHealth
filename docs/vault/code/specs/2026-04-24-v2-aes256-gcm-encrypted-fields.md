---
type: spec-summary
slug: 2026-04-24-v2-aes256-gcm-encrypted-fields
original_type: spec
status: delivered
source: ../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields
---

# Spec — 2026-04-24-v2-aes256-gcm-encrypted-fields

Source : [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/server/security/__init__|server/security/__init__.py]]
- [[../code/server/security/crypto|server/security/crypto.py]] — symbols: `load_encryption_key`, `encrypt_field`, `decrypt_field`, `EncryptionConfigError`, `DecryptionError`
- [[../code/server/db/encrypted|server/db/encrypted.py]] — symbols: `EncryptedBytes`, `EncryptedString`, `EncryptedInt`
- [[../code/server/db/models|server/db/models.py]] — symbols: `Mood`
- [[../code/server/main|server/main.py]] — symbols: `app`, `_validate_encryption_at_boot`
- [[../code/server/routers/mood|server/routers/mood.py]] — symbols: `router`, `create_mood_entry`, `get_mood_entries`
- [[../code/server/models|server/models.py]] — symbols: `MoodIn`, `MoodOut`, `MoodBulkIn`
- [[../code/alembic/versions/0002_encrypt_mood|alembic/versions/0002_encrypt_mood.py]] — symbols: `upgrade`, `downgrade`

### Tests
- [[../code/tests/server/test_crypto_foundation|tests/server/test_crypto_foundation.py]] — classes: `TestLoadEncryptionKey`, `TestEncryptDecryptField`, `TestEncryptedTypeDecorator`, `TestBootValidation` · methods: `test_load_key_from_env`, `test_load_key_missing_env_raises`, `test_load_key_invalid_base64_raises`, `test_load_key_wrong_length_raises`, `test_load_key_all_zeros_raises`, `test_round_trip_bytes`, `test_round_trip_string`, `test_tamper_detected_invalid_tag`, `test_typedecorator_transparent_on_orm`, `test_app_boot_fails_fast_without_key`
- [[../code/tests/server/test_mood_encryption|tests/server/test_mood_encryption.py]] — classes: `TestMoodPersistenceEncrypted`, `TestMoodApiBackCompat`, `TestMoodErrorSanitization` · methods: `test_mood_notes_stored_as_bytes_in_pg`, `test_mood_round_trip_via_orm_transparent`, `test_mood_crypto_v_column_initialised_to_1`, `test_post_get_mood_round_trip`, `test_mood_response_shape_unchanged`, `test_decrypt_failure_returns_500_generic`
