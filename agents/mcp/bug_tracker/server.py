"""
MCP server — Bug Tracker
Logs, queries, and updates bug reports. Writes to SQLite (docs/vault/bugs/bugs.db)
and generates markdown vault notes (docs/vault/bugs/<date>-<slug>.md).
All reasoning stays in the agent; this server handles only mechanical persistence.
"""

import json
import sqlite3
from datetime import date, datetime
from pathlib import Path
from typing import Optional

from mcp.server.fastmcp import FastMCP

PROJECT_ROOT = Path(__file__).parent.parent.parent.parent  # SamsungHealth/
BUGS_DIR = PROJECT_ROOT / "docs" / "vault" / "bugs"
DB_PATH = BUGS_DIR / "bugs.db"

mcp = FastMCP("bug-tracker")

# ──────────────────────────────────────────────
# DB init
# ──────────────────────────────────────────────

def _get_conn() -> sqlite3.Connection:
    BUGS_DIR.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("""
        CREATE TABLE IF NOT EXISTS bugs (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            slug        TEXT    UNIQUE NOT NULL,
            title       TEXT    NOT NULL,
            date        TEXT    NOT NULL,
            severity    TEXT    NOT NULL DEFAULT 'medium',
            status      TEXT    NOT NULL DEFAULT 'open',
            symptom     TEXT,
            repro_steps TEXT,
            expected    TEXT,
            actual      TEXT,
            root_cause  TEXT,
            components  TEXT,
            fix_applied TEXT,
            fix_commit  TEXT,
            test_scenarios TEXT,
            created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
        )
    """)
    conn.commit()
    return conn


def _write_markdown(row: dict) -> Path:
    slug = row["slug"]
    today = row["date"]
    path = BUGS_DIR / f"{today}-{slug}.md"

    components = json.loads(row.get("components") or "[]")
    test_scenarios = json.loads(row.get("test_scenarios") or "[]")
    repro_steps = json.loads(row.get("repro_steps") or "[]")
    components_str = ", ".join(components) if components else "—"
    repro_md = "\n".join(f"{i+1}. {s}" for i, s in enumerate(repro_steps)) if repro_steps else "—"
    tests_md = "\n".join(f"- {s}" for s in test_scenarios) if test_scenarios else "—"

    content = f"""---
title: "{row['title']}"
date: {today}
severity: {row['severity']}
status: {row['status']}
components: [{components_str}]
fix_commit: {row.get('fix_commit') or 'null'}
---

# Bug — {row['title']}

## Symptôme

{row.get('symptom') or '—'}

## Repro

{repro_md}

## Attendu vs Réel

**Attendu :** {row.get('expected') or '—'}
**Réel :** {row.get('actual') or '—'}

## Root cause

{row.get('root_cause') or '—'}

## Fix appliqué

{row.get('fix_applied') or 'Aucun — à traiter'}

## Scénarios de tests à écrire

{tests_md}
"""
    path.write_text(content, encoding="utf-8")
    return path


# ──────────────────────────────────────────────
# Tools
# ──────────────────────────────────────────────

@mcp.tool()
def bug_log(
    title: str,
    slug: str,
    symptom: str,
    repro_steps: list[str],
    expected: str,
    actual: str,
    root_cause: str,
    components: list[str],
    test_scenarios: list[str],
    severity: str = "medium",
    fix_applied: Optional[str] = None,
) -> dict:
    """
    Log a new bug report. Writes to SQLite DB and generates a markdown vault note.
    severity: low | medium | high | critical
    Returns the created bug record with its vault path.
    """
    today = date.today().isoformat()
    status = "fixed" if fix_applied else "open"
    row = {
        "slug": slug,
        "title": title,
        "date": today,
        "severity": severity,
        "status": status,
        "symptom": symptom,
        "repro_steps": json.dumps(repro_steps),
        "expected": expected,
        "actual": actual,
        "root_cause": root_cause,
        "components": json.dumps(components),
        "fix_applied": fix_applied,
        "fix_commit": None,
        "test_scenarios": json.dumps(test_scenarios),
    }
    with _get_conn() as conn:
        conn.execute("""
            INSERT OR REPLACE INTO bugs
            (slug, title, date, severity, status, symptom, repro_steps, expected,
             actual, root_cause, components, fix_applied, fix_commit, test_scenarios)
            VALUES
            (:slug, :title, :date, :severity, :status, :symptom, :repro_steps, :expected,
             :actual, :root_cause, :components, :fix_applied, :fix_commit, :test_scenarios)
        """, row)
        conn.commit()
    path = _write_markdown(row)
    return {
        "slug": slug,
        "status": status,
        "severity": severity,
        "vault_path": str(path.relative_to(PROJECT_ROOT)),
        "test_scenarios_count": len(test_scenarios),
    }


@mcp.tool()
def bug_list(
    status: Optional[str] = None,
    severity: Optional[str] = None,
) -> list[dict]:
    """
    List bug reports. Filter by status (open|fixed) or severity (low|medium|high|critical).
    Returns list of {slug, title, date, severity, status, test_scenarios_count}.
    """
    with _get_conn() as conn:
        query = "SELECT slug, title, date, severity, status, test_scenarios FROM bugs WHERE 1=1"
        params: list = []
        if status:
            query += " AND status = ?"
            params.append(status)
        if severity:
            query += " AND severity = ?"
            params.append(severity)
        query += " ORDER BY date DESC"
        rows = conn.execute(query, params).fetchall()
        return [
            {
                "slug": r["slug"],
                "title": r["title"],
                "date": r["date"],
                "severity": r["severity"],
                "status": r["status"],
                "test_scenarios_count": len(json.loads(r["test_scenarios"] or "[]")),
            }
            for r in rows
        ]


@mcp.tool()
def bug_get(slug: str) -> dict:
    """Get full details of a bug report by slug."""
    with _get_conn() as conn:
        row = conn.execute("SELECT * FROM bugs WHERE slug = ?", (slug,)).fetchone()
        if row is None:
            return {"error": f"bug not found: {slug}"}
        d = dict(row)
        for field in ("repro_steps", "components", "test_scenarios"):
            d[field] = json.loads(d[field] or "[]")
        return d


@mcp.tool()
def bug_update(
    slug: str,
    status: Optional[str] = None,
    fix_commit: Optional[str] = None,
) -> dict:
    """
    Update a bug's status or fix_commit. Also regenerates the markdown vault note.
    status: open | fixed
    """
    with _get_conn() as conn:
        row = conn.execute("SELECT * FROM bugs WHERE slug = ?", (slug,)).fetchone()
        if row is None:
            return {"error": f"bug not found: {slug}"}
        updates = {}
        if status:
            updates["status"] = status
        if fix_commit:
            updates["fix_commit"] = fix_commit
        if not updates:
            return {"error": "nothing to update — provide status or fix_commit"}
        set_clause = ", ".join(f"{k} = ?" for k in updates)
        conn.execute(
            f"UPDATE bugs SET {set_clause} WHERE slug = ?",
            [*updates.values(), slug],
        )
        conn.commit()
        updated = dict(conn.execute("SELECT * FROM bugs WHERE slug = ?", (slug,)).fetchone())
    _write_markdown(updated)
    return {"slug": slug, "updated": list(updates.keys()), "status": updated["status"]}


if __name__ == "__main__":
    mcp.run()
