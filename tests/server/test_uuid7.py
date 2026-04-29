"""
Tests RED — UUID v7 helper (spec: V2.1 postgres-migration).

Mappé sur frontmatter tested_by: tests/server/test_uuid7.py
Classes: TestUuid7
Methods: test_monotonic_within_ms, test_version_field_is_7, test_timestamp_extractable
"""
import time


class TestUuid7:
    def test_version_field_is_7(self):
        # spec: "UUID v7 version field" — §Tests d'acceptation #5
        from server.db.uuid7 import uuid7

        uid = uuid7()
        assert uid.version == 7

    def test_monotonic_within_ms(self):
        # spec: "UUID v7 monotone par milliseconde" — §Tests d'acceptation #4
        from server.db.uuid7 import uuid7

        # 100 UUID générés consécutivement (typiquement < 1 ms)
        uids = [uuid7() for _ in range(100)]
        sorted_uids = sorted(uids)
        assert uids == sorted_uids, "UUID v7 doit être strictement monotone en ordre de génération"

    def test_timestamp_extractable(self):
        # spec: "UUID v7 timestamp extractable" — §Tests d'acceptation #6
        from server.db.uuid7 import uuid7

        t_before_ms = int(time.time() * 1000)
        uid = uuid7()
        t_after_ms = int(time.time() * 1000)

        # UUID v7 : les 48 premiers bits (12 hex chars) encodent le timestamp Unix en ms
        ts_hex = uid.hex[:12]
        extracted_ms = int(ts_hex, 16)

        assert t_before_ms - 5 <= extracted_ms <= t_after_ms + 5, (
            f"Timestamp extrait {extracted_ms} doit être proche de {t_before_ms}..{t_after_ms}"
        )
