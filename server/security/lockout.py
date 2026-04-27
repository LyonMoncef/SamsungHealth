"""V2.3.3.1 — Soft backoff exponentiel + atomic counter + admin-lockout helpers.

- `register_failed_login` : UPDATE atomic with RETURNING (anti lost-update,
  pentester HIGH #3 fix) + soft sleep `min(2^(n-1), 60)s` (no hard auto lock,
  pentester HIGH #5 fix).
- `register_successful_login` : reset counter, but PRESERVE admin lock if
  `locked_until > now()` (race-guard, pentester MED M2).
- `is_user_locked` : True ONLY when admin manually set `locked_until > now()` —
  never via `failed_login_count`.
- `cleanup_stale_failed_login_counts` : cron-style purge for users with
  `last_failed_login_at < now() - 24h`.
"""
from __future__ import annotations

import time
from datetime import datetime, timedelta, timezone

from sqlalchemy import func, update
from sqlalchemy.orm import Session

from server.db.models import User
from server.logging_config import get_logger


_log = get_logger(__name__)


LOCKOUT_BACKOFF_BASE_SECONDS = 1
LOCKOUT_BACKOFF_MAX_SECONDS = 60
LOCKOUT_COUNTER_RESET_AFTER = timedelta(hours=24)


class AccountLockedError(Exception):
    """Raised when a user is admin-locked (locked_until > now)."""


def register_failed_login(db: Session, user: User) -> int:
    """Increment failed_login_count atomically + soft-sleep exponential backoff.

    Returns the new counter value. Never sets `locked_until` (no hard auto lock).
    """
    new_count = db.execute(
        update(User)
        .where(User.id == user.id)
        .values(
            failed_login_count=User.failed_login_count + 1,
            last_failed_login_at=func.now(),
        )
        .returning(User.failed_login_count)
    ).scalar_one()
    db.commit()

    # Soft backoff : 1, 2, 4, 8, 16, 32, 60, 60, … (cap at 60s).
    delay = min(
        LOCKOUT_BACKOFF_BASE_SECONDS * (2 ** (new_count - 1)),
        LOCKOUT_BACKOFF_MAX_SECONDS,
    )
    time.sleep(delay)
    return new_count


def register_successful_login(db: Session, user: User) -> None:
    """Reset counter + last_login_at. PRESERVE admin lock if active (race-guard)."""
    if user.locked_until is not None and user.locked_until > datetime.now(timezone.utc):
        # Admin lock active → do not clobber it.
        return
    db.execute(
        update(User)
        .where(User.id == user.id)
        .values(
            failed_login_count=0,
            locked_until=None,
            last_login_at=func.now(),
        )
    )
    db.commit()


def is_user_locked(user: User) -> bool:
    """True only when admin set `locked_until > now()`. Never via failed_login_count."""
    if user.locked_until is None:
        return False
    return user.locked_until > datetime.now(timezone.utc)


def cleanup_stale_failed_login_counts(db: Session) -> int:
    """Reset failed_login_count to 0 for users whose last fail was > 24h ago.

    Returns the number of rows updated.
    """
    cutoff = datetime.now(timezone.utc) - LOCKOUT_COUNTER_RESET_AFTER
    result = db.execute(
        update(User)
        .where(
            User.failed_login_count > 0,
            User.last_failed_login_at < cutoff,
        )
        .values(failed_login_count=0, last_failed_login_at=None)
    )
    db.commit()
    return result.rowcount or 0
