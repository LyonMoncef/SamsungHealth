---
type: code-source
language: python
file_path: tests/agents/test_changelog_generator.py
git_blob: a1bd65d327729d2908e196f6460e029d27a4ce60
last_synced: '2026-04-23T10:49:30Z'
loc: 163
annotations: []
imports:
- pathlib
- pytest
exports:
- TestParseGitLog
- TestRenderChangelogNote
- TestGenerateChangelog
tags:
- code
- python
---

# tests/agents/test_changelog_generator.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_changelog_generator.py`](../../../tests/agents/test_changelog_generator.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.changelog_generator.

Generates one note per commit in `docs/vault/changelog/<YYYY-MM-DD>-<short-sha>.md`,
each with frontmatter (sha, date, author, scope, files) + full commit message body.
"""

from pathlib import Path

import pytest


class TestParseGitLog:
    def test_parses_oneline_commit_records(self):
        from agents.cartographer.changelog_generator import parse_git_log_records

        # Format: %H%x00%an%x00%ad%x00%s%x00%b%x00%x1e (NUL-delimited fields, RS terminator)
        raw = (
            "abc1234\x00Alice\x002026-04-23\x00feat(x): add foo\x00body line 1\nbody line 2\x00\x1e"
            "def5678\x00Bob\x002026-04-22\x00fix(y): patch\x00\x00\x1e"
        )
        records = parse_git_log_records(raw)
        assert len(records) == 2
        assert records[0].short_sha == "abc1234"
        assert records[0].author == "Alice"
        assert records[0].date == "2026-04-23"
        assert records[0].subject == "feat(x): add foo"
        assert records[0].body == "body line 1\nbody line 2"
        assert records[0].commit_type == "feat"
        assert records[0].scope == "x"

        assert records[1].body == ""
        assert records[1].commit_type == "fix"
        assert records[1].scope == "y"

    def test_handles_subject_without_scope(self):
        from agents.cartographer.changelog_generator import parse_git_log_records

        raw = "abc1234\x00Alice\x002026-04-23\x00chore: bump version\x00\x00\x1e"
        records = parse_git_log_records(raw)
        assert records[0].commit_type == "chore"
        assert records[0].scope == ""

    def test_subject_without_conventional_format(self):
        from agents.cartographer.changelog_generator import parse_git_log_records

        raw = "abc1234\x00Alice\x002026-04-23\x00random subject\x00\x00\x1e"
        records = parse_git_log_records(raw)
        assert records[0].commit_type == ""
        assert records[0].scope == ""
        assert records[0].subject == "random subject"


class TestRenderChangelogNote:
    def test_frontmatter_and_body(self):
        from agents.cartographer.changelog_generator import (
            CommitRecord,
            render_changelog_note,
        )

        rec = CommitRecord(
            short_sha="abc1234",
            full_sha="abc1234fullshahash",
            author="Alice",
            date="2026-04-23",
            subject="feat(sleep): add hypnogram",
            body="Detailed explanation\n\nWith paragraphs.",
            commit_type="feat",
            scope="sleep",
            files=["server/routers/sleep.py", "static/dashboard.js"],
        )
        out = render_changelog_note(rec)
        assert out.startswith("---\n")
        assert "type: changelog" in out
        assert "sha: abc1234" in out
        assert "date: '2026-04-23'" in out or "date: 2026-04-23" in out
        assert "author: Alice" in out
        assert "commit_type: feat" in out
        assert "scope: sleep" in out
        assert "feat(sleep): add hypnogram" in out
        assert "Detailed explanation" in out
        assert "server/routers/sleep.py" in out


class TestGenerateChangelog:
    def test_creates_one_file_per_commit(self, tmp_path: Path, monkeypatch):
        from agents.cartographer import changelog_generator as cg
        from agents.cartographer.changelog_generator import CommitRecord

        records = [
            CommitRecord(
                short_sha="abc1234", full_sha="abc1234full",
                author="Alice", date="2026-04-23",
                subject="feat: x", body="", commit_type="feat",
                scope="", files=["a.py"],
            ),
            CommitRecord(
                short_sha="def5678", full_sha="def5678full",
                author="Bob", date="2026-04-22",
                subject="fix: y", body="", commit_type="fix",
                scope="", files=["b.py"],
            ),
        ]
        # Patch the loader to return our fake records
        monkeypatch.setattr(cg, "load_recent_commits", lambda repo_root, limit: records)

        cg.generate_changelog(
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            limit=2,
        )
        assert (tmp_path / "docs/vault/changelog/2026-04-23-abc1234.md").exists()
        assert (tmp_path / "docs/vault/changelog/2026-04-22-def5678.md").exists()

    def test_idempotent_skips_existing(self, tmp_path: Path, monkeypatch):
        from agents.cartographer import changelog_generator as cg
        from agents.cartographer.changelog_generator import CommitRecord

        records = [
            CommitRecord(
                short_sha="abc1234", full_sha="abc1234full",
                author="A", date="2026-04-23", subject="feat: x",
                body="", commit_type="feat", scope="", files=[],
            ),
        ]
        monkeypatch.setattr(cg, "load_recent_commits", lambda repo_root, limit: records)

        out_file = tmp_path / "docs/vault/changelog/2026-04-23-abc1234.md"
        out_file.parent.mkdir(parents=True)
        out_file.write_text("PRESERVED\n")

        cg.generate_changelog(
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            limit=1,
        )
        # Existing file untouched (idempotent)
        assert out_file.read_text() == "PRESERVED\n"

    def test_regenerate_overwrites(self, tmp_path: Path, monkeypatch):
        from agents.cartographer import changelog_generator as cg
        from agents.cartographer.changelog_generator import CommitRecord

        records = [
            CommitRecord(
                short_sha="abc1234", full_sha="abc1234full",
                author="A", date="2026-04-23", subject="feat: x",
                body="", commit_type="feat", scope="", files=[],
            ),
        ]
        monkeypatch.setattr(cg, "load_recent_commits", lambda repo_root, limit: records)

        out_file = tmp_path / "docs/vault/changelog/2026-04-23-abc1234.md"
        out_file.parent.mkdir(parents=True)
        out_file.write_text("OLD\n")

        cg.generate_changelog(
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            limit=1,
            regenerate=True,
        )
        assert "OLD" not in out_file.read_text()
        assert "feat: x" in out_file.read_text()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestParseGitLog` (class) — lines 12-50
- `TestRenderChangelogNote` (class) — lines 53-81
- `TestGenerateChangelog` (class) — lines 84-163

### Imports
- `pathlib`
- `pytest`

### Exports
- `TestParseGitLog`
- `TestRenderChangelogNote`
- `TestGenerateChangelog`


## Exercises *(auto — this test file touches)*

### `test_changelog_generator.TestGenerateChangelog.test_creates_one_file_per_commit`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `render_changelog_note`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `generate_changelog`

### `test_changelog_generator.TestGenerateChangelog.test_idempotent_skips_existing`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `generate_changelog`

### `test_changelog_generator.TestGenerateChangelog.test_regenerate_overwrites`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `render_changelog_note`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `generate_changelog`

### `test_changelog_generator.TestParseGitLog.test_handles_subject_without_scope`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `parse_git_log_records`

### `test_changelog_generator.TestParseGitLog.test_parses_oneline_commit_records`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `parse_git_log_records`

### `test_changelog_generator.TestParseGitLog.test_subject_without_conventional_format`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `parse_git_log_records`

### `test_changelog_generator.TestRenderChangelogNote.test_frontmatter_and_body`
- [[../../code/agents/cartographer/changelog_generator|agents/cartographer/changelog_generator.py]] · `render_changelog_note`
