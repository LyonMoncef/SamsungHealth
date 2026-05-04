# Multi-agent system — SamsungHealth V2

Système d'agents à responsabilité unique pour exécuter le master plan V2 (refonte security/RGPD/Compose) sans saturer le contexte de la session principale.

## Source of truth

- **Master plan** : `docs/vault/specs/2026-04-23-plan-v2-refactor-master.md`
- **Plan multi-agents** : `docs/vault/specs/2026-04-23-plan-v2-multi-agents-architecture.md`
- **Branche long-lived** : `refactor/v2`
- **Sous-branche méta-projet** : `refactor/phase-a-foundation-agents`

## Layout

```
agents/
├── contracts/                  ← types Pydantic I/O par agent (Phase A)
│   ├── base.py                 ← AgentInputBase / AgentOutputBase
│   ├── spec_writer.py
│   ├── test_writer.py
│   ├── coder.py
│   ├── reviewer.py
│   ├── documenter.py
│   ├── git_steward.py          ← Phase A — gestion git/gh + HISTORY
│   ├── pentester.py            ← Phase A — vulnérabilités SAST/SCA/secrets/semantic/RGPD
│   ├── plan_keeper.py          ← Phase A — détection déviations plans ↔ livraisons
│   ├── migration_writer.py     ← Phase B
│   ├── logq_analyst.py         ← Phase B
│   └── ui_parity.py            ← Phase D
├── orchestrator/               ← orchestration code (Phase A)
│   ├── budget.py               ← BudgetEnforcer (pre-call hard ceiling)
│   ├── retry.py                ← retry avec contexte d'échec enrichi
│   └── checkpoint.py           ← git tag + HISTORY entry helper
└── README.md                   ← ce fichier
```

Voir aussi :
- `.claude/agents/` — définitions Markdown des subagents Claude Code (frontmatter strict)
- `.claude/skills/` — skills slash commands (intent humain → invoke agent)
- `.claude/hooks/` — hooks PreToolUse / PostToolUse / SubagentStop
- `work/` (gitignored) — artifacts par run (`work/<task-id>/{brief,result}.json + trace.jsonl`)

## Pattern d'invocation

Chaque skill produit un brief.json sur disque, spawn le subagent correspondant via Task tool, reçoit un summary court + result.json validé Pydantic. Pas de magie : tout passe par fichier sur disque, jamais par message inline.

```
[human] /spec module p2-sleep
  → write work/<task-id>/brief.json
  → spawn spec-writer subagent
  → spec-writer write work/<task-id>/result.json
  → orchestrator validate result.json (Pydantic)
  → display summary + next step suggestion (linked-list pattern)
```

## Phases d'implémentation

| Phase | Statut | Scope |
|-------|--------|-------|
| **A** — Foundation agents | en cours | contrats Pydantic + 8 subagents (spec-writer, test-writer, coder-backend, reviewer, documenter, **git-steward**, **pentester**, **plan-keeper**) + hooks + settings + skills + budget enforcer + outils SAST/SCA/secrets |
| B — Observabilité | bloquée par P0 master | Phoenix self-hosted, OTel, dashboard `/admin/harness/*`, alerts |
| C — Agents CI | bloquée par P1 master | pr-review, codex-*, audit-security via Anthropic SDK |
| D — Agents Android | bloquée par P3 master | coder-android, ui-parity-checker |
| E — Observabilité avancée | bloquée par P5 master | session replay, run diff, webhooks |

## Règles de scoping

- `coder-backend` n'écrit que dans `server/` et `tests/`
- `coder-android` n'écrit que dans `android-app/`
- `coder-frontend` n'écrit que dans `static/`
- `spec-writer` n'écrit que dans le vault Obsidian
- `reviewer` est read-only
- `documenter` n'écrit que dans `HISTORY.md` + vault `codex/`
- `git-steward` n'utilise que `git`/`gh` via Bash + Edit limité à `HISTORY.md`, `NOTES.md`, `.gitignore`
- `pentester` est read-only sur le code source, n'écrit QUE dans `work/<task-id>/poc/` + `work/<task-id>/pentest-report.md`, n'exécute jamais de POC en Phase A
- `plan-keeper` est read-only absolu, n'écrit QUE `work/<task-id>/plan-audit.md` + `result.json` ; propose des patches plans, n'applique jamais
- Tout cela enforced par `.claude/hooks/enforce-path-scope.sh`

## `git-steward` — gardien des opérations git/gh

Agent dédié à toutes les opérations git/gh + maintenance HISTORY.md, invoqué automatiquement après chaque save (via hook `PostToolUse Edit|Write`) et avant tout commit/PR/checkpoint.

**Rôle** : éviter de saturer le contexte de la session principale avec du raisonnement git (résolution conflits, divergence local/origin, renommage branche, multi-tag, audit tags pushés vs locaux, edits HISTORY split entre branches, …).

**Workflow** :
1. **Pré-flight check** — branche conforme `feat/|fix/|chore/|hotfix/|release/|refactor/|spike/`, sync origin, conflicts pending, stash oublié, fichiers staged sensibles
2. **Commit** — génère message single-line (CLAUDE.md global rule), pas de Co-Authored-By trailer, update HISTORY.md (Features + Changelog), commit
3. **Tag/Checkpoint** — avant op destructive crée `checkpoint-<scope>-<date>` annoté + push + entry HISTORY `## Checkpoint`
4. **PR** — vérifie base correcte (`feat/* → dev`, `release/* → main`), génère titre + body conforme template
5. **Conflict resolution** — diagnostique sémantique vs textuel, propose résolution avec rationale, n'auto-résout JAMAIS sans approval
6. **Branch hygiene** — détecte stale (mergées non-supprimées) + divergentes (local ahead/behind), propose cleanup explicite

**Skills associés (linked-list)** :

| Skill | Invoque | Next default |
|-------|---------|--------------|
| `/git-status` | git-steward (audit complet) | `/commit` ou `/git-fix` |
| `/commit` | git-steward (compose msg + HISTORY + commit) | `/git-status` ou `/pr` |
| `/checkpoint <scope>` | git-steward (tag + push + HISTORY entry) | `/git-status` |
| `/pr` (existant, à enrichir) | git-steward + `documenter` | merge wait |
| `/release` (existant, à enrichir) | git-steward (release PR + tag) | `/smoke` |
| `/git-fix` | git-steward (résout conflit/divergence avec approval) | `/commit` |

**Mode automatique post-save** : hook `.claude/hooks/post-edit-git-steward.sh` invoque git-steward en audit silencieux après chaque `Edit|Write`. Si l'audit produit un commit candidat propre (1 fichier ou groupe sémantique cohérent), il propose `/commit` dans le delivery du skill courant. Pas d'auto-commit silencieux — l'humain valide toujours via `/commit`.

## `pentester` — chasseur de vulnérabilités

Agent adversarial qui ignore la spec et cherche **comment abuser du code**. Complémentaire de `reviewer` (qui valide la cohérence spec ↔ code) et des `test-writer` (qui valide le contrat fonctionnel).

**Modes** :

| Mode | Phase A | Outils | Risque |
|------|---------|--------|--------|
| `sast` (code statique) | ✅ | `bandit`, `semgrep`, custom grep heuristics | 🟢 Zéro |
| `sca` (deps CVE) | ✅ | `pip-audit`, `safety` | 🟢 Zéro |
| `secrets` | ✅ | `gitleaks` + custom regex | 🟢 Zéro |
| `semantic` (auth/IDOR/logic) | ✅ | manual + grep heuristics | 🟢 Zéro |
| `rgpd` (data protection) | ✅ | custom heuristics projet | 🟢 Zéro |
| `dast` (active probing) | ❌ Phase B+ uniquement | sandbox `docker-compose.pentest.yml` requise | 🟠 Sandbox-only |

**Génération de POCs** :
- POCs générés pour `severity >= high` uniquement (`exploit_scenario` textuel pour le reste)
- Écrits dans `work/<task-id>/poc/` (gitignored via `work/`)
- En-tête `# DO NOT RUN — illustrative only`, hardcoded localhost, jamais de lecture `.env`
- **Phase A** : `allow_poc_execution=true` dans le brief = ignoré côté agent (pas de sandbox)
- **Phase B+** : sandbox isolée (`docker-compose.pentest.yml` — Postgres :5433, app :8002, données synthétiques) ; pentester accepte `target_url=http://localhost:8002` UNIQUEMENT

**Skill associé** : `/pentest`

| Invocation | Comportement |
|------------|--------------|
| `/pentest` | `scope_mode=full_repo`, tous modes statiques, severity_threshold=medium |
| `/pentest --quick` ou `--lastdiffs` | `scope_mode=diff_only`, skip semgrep, scope = diff vs `refactor/v2` |
| `/pentest --files <a,b,c>` | `scope_mode=files_only` |
| `/pentest --sandbox` (Phase B+) | spin up sandbox + run avec DAST + teardown |

**Auto-invoké par `/review`** avec `scope_mode=diff_only`, `quick=true`, `severity_threshold=medium`. Verdict global `/review` :
- reviewer `approve` ET pentester `pass|warn` → **approve**
- pentester `block_merge` → **request_changes** (override reviewer)
- sinon → reviewer verdict standard

**Outils à installer** (Phase A — voir Makefile target `security-install`) :
```bash
make security-install   # bandit, pip-audit, safety, semgrep
# gitleaks: cf https://github.com/gitleaks/gitleaks#installing (brew/apt/binaire)
```

## `plan-keeper` — gardien de la cohérence plans ↔ livraisons

Agent qui détecte les déviations entre les plans approuvés (master + multi-agents + specs) et l'état réel du repo (agents, skills, branches, structure). Symétrique à `reviewer` : reviewer = local (spec ↔ code d'une livraison), plan-keeper = global (plans ↔ structure du projet).

**Quand il tourne** :

| Trigger | Comportement |
|---------|--------------|
| Hook `PreToolUse` sur `git-steward` (op_type=commit/checkpoint/pr) | Audit silencieux ; si déviation `severity >= medium` → bloque le commit et propose patches |
| Auto post-livraison de `/spec`, `/tdd`, `/impl`, `/pentest` | Vérifie que la livraison est référencée par un plan ; sinon flag `file_orphan` |
| Skill manuel `/align` | Audit complet on-demand |

**Catégories de déviations** : `agent_added_not_in_plan`, `branch_naming_mismatch`, `phase_scope_drift`, `directory_structure_drift`, `skill_added_not_in_plan`, `file_orphan`, `duration_estimate_drift`, `other`.

**Sortie** : `PlanAuditReport` avec liste de `PlanDeviation` (severity + location + plan_affected + proposed_patch). Read-only — propose seulement, n'applique jamais (les patches sont appliqués par `documenter` via `/commit`).

**Skill associé** : `/align` — invocation manuelle, scope full ou diff selon args.

**Linked-list** : `/align` → `/commit` (appliquer patches) ou `/impl`/`/spec` (refonte structurelle si block).

**Pourquoi cet agent existe** : sans lui, chaque ajout d'agent/skill/branche hors plan crée une dette structurelle invisible jusqu'à la prochaine release. Le pattern industrie standard est "doc-as-code" — ce mécanisme l'automatise.

## Budget

`BudgetEnforcer` (`agents/orchestrator/budget.py`) bloque pré-appel API si :
- Run dépasserait `CLAUDE_MAX_TOKENS_PER_TASK`
- Coût journalier dépasserait le ceiling

Pas d'alerte qui fait office d'enforcement (leçon $47k cf. plan §Budget enforcement).

## Linked-list de skills

Chaque skill termine son delivery par une suggestion du suivant logique :

```
✅ Delivery: <résumé 1 ligne>
👉 Next: /<nextSkill> <args> ou commentaire pour ajuster.
```

Le `next_default` est déclaré dans le frontmatter de chaque `SKILL.md`.
