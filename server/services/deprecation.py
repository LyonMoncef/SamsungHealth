"""
Phase D — déprécier les endpoints CSV import sans casser la rétrocompat.

L'app Android ne les appelle plus depuis Phase B (LocalImportService écrit
directement en Room). Ils restent disponibles pour clients tiers / scripts /
migration jusqu'à la `SUNSET_DATE` puis seront supprimés.

Cf. RFC 8594 (Sunset) + IETF draft `Deprecation` header.
"""
from __future__ import annotations

from fastapi import Response

from server.logging_config import get_logger

_log = get_logger(__name__)

# Date de retrait planifié — 6 mois après la mise en service de Phase B
# (référence : 2026-05-09 ; cible 2026-11-09).
SUNSET_DATE = "Mon, 09 Nov 2026 00:00:00 GMT"


def mark_deprecated(response: Response, endpoint: str, user_id: str | None = None) -> None:
    """Ajoute les headers `Deprecation` + `Sunset` et log un warning structuré."""
    response.headers["Deprecation"] = "true"
    response.headers["Sunset"] = SUNSET_DATE
    response.headers["Link"] = (
        '<https://github.com/LyonMoncef/SamsungHealth/blob/main/docs/vault/specs/'
        '2026-05-09-local-first-migration.md>; rel="deprecation"; type="text/markdown"'
    )
    _log.warning(
        "deprecated_endpoint_called",
        endpoint=endpoint,
        sunset=SUNSET_DATE,
        user_id=user_id,
        guidance="Use Android local imports (LocalImportService) instead",
    )
