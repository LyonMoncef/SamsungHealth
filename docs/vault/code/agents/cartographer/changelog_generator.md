---
type: code-source
language: python
file_path: agents/cartographer/changelog_generator.py
git_blob: a1a0e1e6c226c8a774b0fd108602ccc7e3f3d1b6
last_synced: '2026-04-23T08:44:33Z'
loc: 170
annotations: []
imports:
- '

  i'
- '

  i'
- 'bprocess

  f'
- taclasses i
- 'ml


  '
exports:
- rse_git_log_records(r
- ad_recent_commits(r
- er_changelog_note(rec
- "e_changelog(\n    r"
- 'gv: '
tags:
- code
- python
---

# agents/cartographer/changelog_generator.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/changelog_generator.py`](../../../agents/cartographer/changelog_generator.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Generate one vault note per recent git commit.

Output: `docs/vault/changelog/<YYYY-MM-DD>-<short-sha>.md` per commit, with
frontmatter (sha, date, author, commit_type, scope, files) + full message body.

Idempotent by default — existing files are skipped. Pass `regenerate=True`
to overwrite (used when message format/template changes).
"""

from __future__ import annotations

import os
import re
import subprocess
from dataclasses import dataclass, field

import yaml


_CONVENTIONAL_RE = re.compile(r"^([a-z]+)(?:\(([^)]+)\))?:\s*")


@dataclass
class CommitRecord:
    short_sha: str
    full_sha: str
    author: str
    date: str
    subject: str
    body: str
    commit_type: str
    scope: str
    files: list[str] = field(default_factory=list)


def parse_git_log_records(raw: str) -> list[CommitRecord]:
    """Parse the NUL-delimited / RS-terminated output of `git log --format=...`.

    Each record: `%H\\x00%an\\x00%ad\\x00%s\\x00%b\\x00\\x1e`
    """
    records: list[CommitRecord] = []
    for chunk in raw.split("\x1e"):
        chunk = chunk.strip("\n")
        if not chunk:
            continue
        parts = chunk.split("\x00")
        if len(parts) < 5:
            continue
        full_sha = parts[0]
        author = parts[1]
        date = parts[2]
        subject = parts[3]
        body = parts[4].rstrip()

        m = _CONVENTIONAL_RE.match(subject)
        commit_type = m.group(1) if m else ""
        scope = (m.group(2) or "") if m else ""

        records.append(CommitRecord(
            short_sha=full_sha[:7],
            full_sha=full_sha,
            author=author,
            date=date,
            subject=subject,
            body=body,
            commit_type=commit_type,
            scope=scope,
        ))
    return records


def load_recent_commits(repo_root: str, limit: int) -> list[CommitRecord]:
    """Run `git log` and parse + enrich with file lists."""
    fmt = "%H%x00%an%x00%ad%x00%s%x00%b%x00%x1e"
    out = subprocess.check_output(
        [
            "git", "log",
            f"-{limit}",
            "--date=short",
            f"--format={fmt}",
        ],
        cwd=repo_root,
    ).decode("utf-8", errors="replace")

    records = parse_git_log_records(out)

    # Enrich with file lists (one extra git call per commit — cheap on 30 commits)
    for rec in records:
        files_out = subprocess.check_output(
            ["git", "show", "--name-only", "--format=", rec.full_sha],
            cwd=repo_root,
        ).decode("utf-8", errors="replace")
        rec.files = [f for f in files_out.strip().splitlines() if f]
    return records


def render_changelog_note(rec: CommitRecord) -> str:
    meta = {
        "type": "changelog",
        "sha": rec.short_sha,
        "full_sha": rec.full_sha,
        "date": rec.date,
        "author": rec.author,
        "commit_type": rec.commit_type,
        "scope": rec.scope,
        "files": rec.files,
        "tags": ["changelog", rec.commit_type] if rec.commit_type else ["changelog"],
    }
    fm = yaml.safe_dump(meta, sort_keys=False, allow_unicode=True).rstrip()
    body_section = f"\n\n## Body\n\n{rec.body}\n" if rec.body else ""
    return (
        f"---\n{fm}\n---\n\n"
        f"# `{rec.short_sha}` — {rec.subject}\n\n"
        f"**Author** : {rec.author} · **Date** : {rec.date}\n"
        f"{body_section}"
        f"\n## Files touched ({len(rec.files)})\n\n"
        + ("\n".join(f"- `{f}`" for f in rec.files) if rec.files else "_(no files)_")
        + "\n"
    )


def generate_changelog(
    repo_root: str,
    vault_root: str,
    limit: int = 30,
    regenerate: bool = False,
) -> int:
    """Write one note per recent commit. Return number of notes written."""
    out_dir = os.path.join(vault_root, "changelog")
    os.makedirs(out_dir, exist_ok=True)

    written = 0
    for rec in load_recent_commits(repo_root, limit):
        path = os.path.join(out_dir, f"{rec.date}-{rec.short_sha}.md")
        if os.path.exists(path) and not regenerate:
            continue
        with open(path, "w", encoding="utf-8") as fp:
            fp.write(render_changelog_note(rec))
        written += 1
    return written


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    import argparse
    import sys

    p = argparse.ArgumentParser(prog="changelog-generator")
    p.add_argument("--limit", type=int, default=30)
    p.add_argument("--regenerate", action="store_true")
    p.add_argument("--repo-root", default=os.getcwd())
    p.add_argument("--vault-root", default=os.path.join(os.getcwd(), "docs", "vault"))
    ns = p.parse_args(argv if argv is not None else sys.argv[1:])

    written = generate_changelog(
        repo_root=ns.repo_root,
        vault_root=ns.vault_root,
        limit=ns.limit,
        regenerate=ns.regenerate,
    )
    print(f"[changelog-generator] {written} note(s) written (limit={ns.limit}, regenerate={ns.regenerate})")
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `rse_git_log_records(r` (function) — lines 36-69
- `ad_recent_commits(r` (function) — lines 72-94
- `er_changelog_note(rec` (function) — lines 97-119
- `e_changelog(
    r` (function) — lines 122-140
- `gv: ` (function) — lines 147-165

### Imports
- `
i`
- `
i`
- `bprocess
f`
- `taclasses i`
- `ml

`

### Exports
- `rse_git_log_records(r`
- `ad_recent_commits(r`
- `er_changelog_note(rec`
- `e_changelog(
    r`
- `gv: `
