---
title: Plan V2 — Multi-agents Architecture
type: plan
project: SamsungHealth
phase: meta
status: approved
created: 2026-04-23
updated: 2026-04-23
branch: v2-refactor
parent_plan: 2026-04-23-plan-v2-refactor-master.md
vault_links:
  claude_plan_file: /home/tats/.claude/plans/glittery-napping-rossum.md
  research_note: /home/tats/.claude/plans/glittery-napping-rossum-agent-a5dc04e66912dc995.md
  master_plan: 2026-04-23-plan-v2-refactor-master.md
tags: [samsunghealth, plan, v2, agents, orchestration, observability, harness, monitoring]
stack_choice: arize-phoenix
trace_format: opentelemetry-genai
viz_primary: mermaid-gitgraph
---

# Plan — Architecture multi-agents pour SamsungHealth V2

> **Master plan de la refonte** : `docs/vault/specs/2026-04-23-plan-v2-refactor-master.md` (approuvé 2026-04-23)
> Ce plan définit **comment** exécuter le master plan avec des agents à responsabilité unique.
> Les specs vivent **dans le repo** dans `docs/vault/` (source canonique, versionnée en git). Le dossier Windows est un miroir read-only optionnel pour navigation Obsidian (voir `.env.local.example`).

## Context

Le master plan V2 liste 6 phases, ~6-8 semaines, avec pour chaque module une spec Obsidian, des tests TDD RED-first, une implémentation backend/Android, des mises à jour HISTORY/BACKLOG/Codex, et des reviews de parité UI. Tout exécuter dans la session principale sature le contexte : 1 passe = lecture de 10+ fichiers, diff de centaines de lignes, génération de specs + code + tests + docs, pour un seul livrable.

**Objectif** : segmenter le travail en agents à rôle unique, avec contrats API formalisés, pour :
1. **Libérer du contexte** à la session principale (orchestrateur)
2. **Isoler les responsabilités** — un agent = une mission = un type de sortie
3. **Scoper les permissions** — coder ne lit pas les secrets, doc ne touche pas au code, tester ne modifie rien
4. **Paralléliser** quand les tâches sont indépendantes (plusieurs phases, plusieurs modules)
5. **Intégrer dans CI/CD** — certains agents tournent en local (Claude Code), d'autres en GitHub Actions via Anthropic SDK

**Problème concret à résoudre** : actuellement 1 session Claude Code fait tout (spec Obsidian + code + tests + doc + PR + review). Un cycle P2 (sleep + steps + hr + exercise) saturerait le contexte avant la fin du 2ᵉ module. Avec des subagents, l'orchestrateur délègue par rôle, chaque subagent démarre cold avec un brief ciblé, retourne un résumé structuré, et libère son contexte.

---

## Décisions techniques

| Sujet | Choix | Raison |
|-------|-------|--------|
| **Runtime local** | Claude Code subagents natifs (Task tool + `.claude/agents/*.md`) | Context isolation automatique, tool scoping built-in, file handoff natif, zéro infra |
| **Runtime CI** | Claude Agent SDK Python (`anthropic` package) + GitHub Actions | Pattern DataSaillance (`.github/scripts/job_runner.py`) réutilisable |
| **Contrats agent** | Pydantic dans `agents/contracts/` — versionnés (`version: "1.0"`) | Même contrat réutilisé local + CI ; validation entrée/sortie obligatoire |
| **Handoff** | Fichiers sur disque (`work/<task-id>/`), messages = contrôle uniquement | Gros artifacts (diffs, logs, outputs) ne polluent pas le contexte |
| **Modèle par rôle** | Haiku (linter, documenter simple) / Sonnet (coder, tester, reviewer) / Opus (orchestrateur, architect) | Cost/latency optimal ; 80% des tâches en Sonnet, 15% Haiku, 5% Opus |
| **Guardrails** | `tools:` allowlist par agent + hooks `PreToolUse` pour ops risquées + git tag checkpoint avant multi-fichiers | Principle of least privilege, rollback garanti |
| **Fail handling** | Retry max 2 fois avec contexte d'échec ajouté → escalade orchestrateur → human-in-the-loop | Pas de boucle infinie, humain toujours dans la boucle décisionnelle |
| **Observabilité** | Logging per agent per run → `work/<task-id>/trace.jsonl` + tokens comptabilisés | Mesurer coût réel, identifier agents qui dérivent |
| **État** | Pas de state machine (LangGraph overkill à cette échelle) — orchestrateur = session principale qui pilote séquentiellement | Simplicité |

---

## Inventaire des agents

### Groupe 1 — Agents locaux (Claude Code subagents, `.claude/agents/`)

| Agent | Rôle | Modèle | Tools autorisés | Tools interdits | Input type | Output type |
|-------|------|--------|-----------------|-----------------|------------|-------------|
| `spec-writer` | Rédige specs Obsidian (module / UI / RGPD) à partir d'un brief | Sonnet | Read, Write (Obsidian only), Grep, Glob | Edit, Bash, WebFetch | `SpecBrief` | `SpecArtifact` |
| `test-writer` | Écrit les tests RED first avant toute impl, depuis la spec | Sonnet | Read, Write (tests only), Grep, Glob | Edit (src), Bash (run tests OK) | `TestBrief` | `TestArtifact` |
| `coder-backend` | Implémente code Python (routers, services, models) pour faire passer les tests | Sonnet | Read, Edit, Write (server/), Grep, Glob, Bash (run tests) | Write hors server/, Delete | `CodeBrief` | `CodeArtifact` |
| `coder-android` | Implémente Kotlin/Compose (screens, Canvas, repositories) | Sonnet | Read, Edit, Write (android-app/), Grep, Glob, Bash (gradle) | Write hors android-app/ | `CodeBrief` | `CodeArtifact` |
| `coder-frontend` | Implémente JS dashboard (static/) | Sonnet | Read, Edit, Write (static/), Grep, Glob, Bash (vitest) | Write hors static/ | `CodeBrief` | `CodeArtifact` |
| ~~`reviewer`~~ | ~~Vérifie parité spec ↔ code~~. **Retiré 2026-05-06** — délégué au global `reviewer` (~/.claude/agents/reviewer.md). CLAUDE.md fournit le contexte projet. | — | — | — | — | — |
| `documenter` | Met à jour HISTORY.md + feature table + codex entries Obsidian post-merge | Haiku | Read, Edit (docs/HISTORY/vault), Grep | Edit code, Bash | `DocBrief` | `DocArtifact` |
| `git-steward` | **Toutes** les opérations git/gh + maintenance HISTORY.md/.gitignore — invoqué auto post-save + avant chaque commit/PR/checkpoint | Sonnet | Read, Bash (git/gh only), Edit (HISTORY.md/NOTES.md/.gitignore only) | Write, WebFetch, WebSearch | `GitOperationBrief` | `GitOperationReport` |
| ~~`pentester`~~ | ~~Adversarial — SAST/SCA/secrets/RGPD~~. **Retiré 2026-05-06** — délégué au global `pentester` (~/.claude/agents/pentester.md) qui inclut scan RGPD + frontend React. Contrat `agents/contracts/pentester.py` conservé. | — | — | — | — | — |
| `plan-keeper` | Détecte déviations plans ↔ livraisons (agents/skills/branches/structure) — read-only, propose patches plans, n'applique pas. Invoqué auto pré-commit + post-livraison + manuel `/align` | Sonnet | Read, Grep, Glob, Bash (read-only git) | Edit, Write, WebFetch | `PlanAuditBrief` | `PlanAuditReport` |
| `vision-keeper` | Évalue l'alignement d'un artefact (spec/plan/diff) avec VISION.md — verdict `aligned`/`drift_alert`/`vision_update_needed` + score 0-100. Invoqué après `/spec` ou via `/vision`. **Ajouté 2026-05-06** | Sonnet | Read, Bash (read-only) | Write, Edit, WebFetch | `VisionAuditBrief` | `VisionAuditResult` |
| ~~`code-cartographer`~~ | ~~Phase A.5 — régénère notes vault~~. **Retiré 2026-05-06** — délégué au global `code-cartographer` (~/.claude/agents/code-cartographer.md, plus complet : Write tool + gestion orphans). Contrat `agents/contracts/cartographer.py` conservé. Skills `/sync-vault`, `/annotate`, `/anchor-review` inchangés. | — | — | — | — | — |
| `logq-analyst` | Interroge logs via `logq` CLI pour diagnostic/debug, remonte un résumé | Haiku | Read, Bash (logq only) | Write, Edit | `LogQuery` | `LogReport` |
| `migration-writer` | Rédige une révision Alembic depuis un brief de changement de schéma | Sonnet | Read, Write (alembic/versions/), Bash (alembic autogenerate) | Write hors alembic/ | `MigrationBrief` | `MigrationArtifact` |
| `ui-parity-checker` | Compare screenshots browser (Playwright) vs Compose (Paparazzi), remonte diff % | Sonnet | Read, Bash (playwright, paparazzi) | Write, Edit | `ParityBrief` | `ParityReport` |

### Groupe 2 — Agents CI (Anthropic SDK, `.github/scripts/`)

Pattern DataSaillance adapté — déclenché par GitHub Actions :

| Agent | Rôle | Trigger | Script | Template prompt |
|-------|------|---------|--------|-----------------|
| `pr-review-agent` | Review auto à l'ouverture de PR — commente inline | `pull_request.opened`, `synchronize` | `.github/scripts/pr_review_agent.py` | `.github/templates/agents/pr-review.md` |
| `codex-feature-summary` | Génère entry Codex "feature" post-merge | PR mergée (label `feat`) | réutilise `.github/scripts/job_runner.py` + `codex-jobs.yml` | `.github/templates/codex/feature-summary.md` |
| `codex-bug-fix` | Idem pour bugs (Symptôme/Cause/Fix/Prévention) | PR mergée (label `fix`) | idem | `.github/templates/codex/bug-fix.md` |
| `codex-migration` | Entry Codex pour migrations Alembic | PR touche `alembic/versions/**` | idem | `.github/templates/codex/migration.md` |
| `codex-adr` | Entry ADR sur issue fermée `decision|adr` | Issue closed | idem | `.github/templates/codex/adr.md` |
| `audit-security-agent` | Analyse output `pip-audit` + `gradle dependencyUpdates`, crée issues triées par CVSS | Cron 1er du mois | `.github/scripts/audit_security_agent.py` | `.github/templates/agents/audit-security.md` |
| `ui-parity-ci-agent` | En CI, compare screenshots prod vs Compose, poste résultat en check run | Post-build PR | `.github/scripts/parity_ci_agent.py` | `.github/templates/agents/ui-parity.md` |

### Groupe 3 — Skills Claude Code (orchestration manuelle utilisateur)

Réutiliser/adapter les skills DataSaillance + créer ceux spécifiques SamsungHealth :

| Skill | Rôle | Subagents invoqués | Next default (linked-list) |
|-------|------|---------------------|-----------------------------|
| `/daily` | Kickoff session : contexte branche, issues courantes, spec de la phase active | logq-analyst (optionnel) | `/spec` ou `/triage` |
| `/wrap` | Cloture session : update BACKLOG, update HISTORY, rédiger résumé | documenter + git-steward | (fin de session) |
| `/spec <type> <slug>` | **nouveau** — crée un spec Obsidian depuis template | spec-writer | `/tdd` |
| `/tdd <spec-path>` | **nouveau** — lit la spec, génère les tests RED first, prépare l'environnement pour l'impl | test-writer | `/impl` |
| `/impl <spec-path> <target>` | **nouveau** — impl après tests RED : dispatch au bon coder (backend/android/frontend), fail si tests GREEN avant impl | coder-backend\|coder-android\|coder-frontend | `/review` |
| `/review <branch>` | Review complet : spec ↔ code, tests, sécurité, parité | reviewer + **pentester** (scope diff, quick) + ui-parity-checker | `/commit` ou `/impl` (si changes requested) |
| `/pentest [--quick\|--lastdiffs\|--files\|--sandbox]` | **nouveau** — chasse vulnérabilités full-spectrum (SAST/SCA/secrets/semantic/RGPD ; DAST si --sandbox Phase B+) | pentester | `/impl` (block_merge), `/review` (warn), `/commit` (pass) |
| `/align [--full\|--scope <path>]` | **nouveau** — audit cohérence plans ↔ livraisons (agents, skills, branches, structure) | plan-keeper | `/commit` (appliquer patches), `/impl` (refonte structurelle si block) |
| `/vision <artifact-path> [--slug <slug>]` | **Ajouté 2026-05-06** — audit alignement spec/plan vs VISION.md (score 0-100, block si violation C1/LLM) | vision-keeper | `/tdd` (si aligned) |
| `/sync-vault [--full\|--diff\|--check]` | **Phase A.5** — régénère notes `docs/vault/code/` depuis code source + annotations | code-cartographer | `/commit` |
| `/annotate <slug> [--at <file:line>\|--range <file:start-end>\|edit\|delete]` | **Phase A.5** — CRUD annotations + injection/retrait marqueurs `@vault:<slug>` | code-cartographer | `/sync-vault --diff` |
| `/anchor-review [<slug>]` | **Phase A.5** — résout annotations orphelines (marqueur perdu suite refactor) | code-cartographer | `/commit` |
| `/sync-coverage` | **Phase A.7** — régénère `docs/vault/_index/coverage-map.json` (gitignored) via pytest --cov + dynamic context. Sert au note_renderer pour afficher liens "Tested by:" | (CLI direct, pas de subagent) | `/sync-vault --full` |
| `/git-status` | **nouveau** — audit complet état git (branche, sync, conflicts, stash, fichiers sensibles) | git-steward | `/commit` ou `/git-fix` |
| `/commit` | **nouveau** — compose message single-line + update HISTORY + commit, après save automatique | git-steward | `/git-status` ou `/pr` |
| `/checkpoint <scope>` | **nouveau** — tag annoté + push + entry HISTORY `## Checkpoint` avant op destructive | git-steward | `/git-status` |
| `/git-fix` | **nouveau** — résout conflit/divergence/branche stale avec approval explicite | git-steward | `/commit` |
| `/pr` | Crée PR avec template rempli (lien spec, tests, migrations, logging impact) | git-steward + documenter | (attente merge) |
| `/release` | Release PR `refactor/v2` (ou autre) → `main` + tag release | git-steward | `/smoke` |
| `/triage` | Trie issues en tiers (pattern DataSaillance) | — | `/spec` ou `/issue` |
| `/debug <symptom>` | **nouveau** — analyse logs + code pour identifier la cause d'un bug | logq-analyst + reviewer | `/tdd` (régression) ou `/impl` (fix direct) |

Chaque skill termine son delivery par : `✅ Delivery: <résumé 1 ligne>` puis `👉 Next: /<nextSkill> <args> ou commentaire pour ajuster.` Le `next_default` est déclaré dans le frontmatter de chaque `SKILL.md` (linked-list déterministe).

---

## Directory layout

```
SamsungHealth/
├── .claude/
│   ├── agents/                          ← subagents natifs Claude Code
│   │   ├── spec-writer.md
│   │   ├── test-writer.md
│   │   ├── coder-backend.md
│   │   ├── coder-android.md
│   │   ├── coder-frontend.md
│   │   ├── documenter.md
│   │   ├── git-steward.md               ← Phase A — gestion git/gh + HISTORY
│   │   ├── plan-keeper.md               ← Phase A — détection déviations plans
│   │   ├── vision-keeper.md             ← Ajouté 2026-05-06 — alignement VISION.md
│   │   │   # Retirés 2026-05-06 (délégués aux globaux ~/.claude/agents/) :
│   │   │   # reviewer.md, pentester.md, code-cartographer.md, annotation-suggester.md
│   │   ├── logq-analyst.md
│   │   ├── migration-writer.md
│   │   └── ui-parity-checker.md
│   ├── skills/                          ← slash commands
│   │   ├── daily/SKILL.md
│   │   ├── wrap/SKILL.md
│   │   ├── spec/SKILL.md                ← nouveau
│   │   ├── tdd/SKILL.md                 ← nouveau
│   │   ├── impl/SKILL.md                ← nouveau
│   │   ├── review/SKILL.md
│   │   ├── git-status/SKILL.md          ← nouveau (git-steward)
│   │   ├── commit/SKILL.md              ← nouveau (git-steward, auto post-save)
│   │   ├── checkpoint/SKILL.md          ← nouveau (git-steward)
│   │   ├── git-fix/SKILL.md             ← nouveau (git-steward)
│   │   ├── pentest/SKILL.md             ← nouveau (pentester)
│   │   ├── align/SKILL.md               ← nouveau (plan-keeper)
│   │   ├── sync-vault/SKILL.md          ← Phase A.5 (code-cartographer)
│   │   ├── annotate/SKILL.md            ← Phase A.5 (code-cartographer)
│   │   ├── anchor-review/SKILL.md       ← Phase A.5 (code-cartographer)
│   │   ├── sync-coverage/SKILL.md       ← Phase A.7 — pytest --cov + manifest
│   │   ├── pr/SKILL.md
│   │   ├── release/SKILL.md
│   │   ├── triage/SKILL.md
│   │   └── debug/SKILL.md               ← nouveau
│   ├── hooks/
│   │   ├── pre-bash-destructive.sh      ← bloque rm -rf, force-push, etc.
│   │   ├── post-edit-run-linter.sh      ← auto-invoke ruff/eslint après Edit
│   │   ├── post-edit-git-steward.sh     ← invoque git-steward audit après Edit/Write
│   │   └── pre-commit-checkpoint.sh     ← git tag avant commit multi-fichiers
│   └── settings.json                    ← permissions + hooks + env vars CI
├── agents/                              ← contrats + orchestration code
│   ├── contracts/
│   │   ├── __init__.py
│   │   ├── base.py                      ← AgentInput/Output bases Pydantic
│   │   ├── spec_writer.py
│   │   ├── test_writer.py
│   │   ├── coder.py
│   │   ├── reviewer.py
│   │   ├── documenter.py
│   │   ├── git_steward.py               ← GitOperationBrief / GitOperationReport
│   │   ├── pentester.py                 ← PentestBrief / PentestFinding / PentestReport
│   │   ├── plan_keeper.py               ← PlanAuditBrief / PlanDeviation / PlanAuditReport
│   │   ├── vision_keeper.py             ← VisionAuditBrief / VisionAuditResult (Ajouté 2026-05-06)
│   │   ├── cartographer.py              ← Phase A.5 — CartographyBrief/Report + AnnotationOpBrief/Report + Annotation/AnchorLocation
│   │   ├── annotation_suggester.py      ← Phase A.6 — AnnotationSuggestionBrief/Report + SuggestedAnnotation
│   │   ├── migration_writer.py
│   │   ├── logq_analyst.py
│   │   └── ui_parity.py
│   ├── orchestrator/
│   │   ├── __init__.py
│   │   ├── dispatch.py                  ← helper : choisit le bon coder
│   │   ├── retry.py                     ← stratégie retry avec contexte échec
│   │   └── checkpoint.py                ← git tag + HISTORY entry
│   └── README.md                        ← doc publique du système multi-agent
├── work/                                ← artifacts par run — gitignored
│   └── <task-id>/                       ← un dossier par invocation
│       ├── brief.json                   ← input de l'agent
│       ├── result.json                  ← output de l'agent
│       ├── trace.jsonl                  ← log structuré de l'exécution
│       ├── diff.patch                   ← si coder : le diff produit
│       └── review.md                    ← si reviewer : le rapport
├── .github/
│   ├── scripts/
│   │   ├── extractor.py                 ← copié DataSaillance, adapté
│   │   ├── job_runner.py                ← copié DataSaillance, adapté
│   │   ├── pr_review_agent.py           ← nouveau
│   │   ├── audit_security_agent.py      ← nouveau
│   │   └── parity_ci_agent.py           ← nouveau
│   ├── templates/
│   │   ├── codex/                       ← copié DataSaillance
│   │   │   ├── feature-summary.md
│   │   │   ├── bug-fix.md
│   │   │   ├── migration.md
│   │   │   └── adr.md
│   │   └── agents/                      ← nouveau
│   │       ├── pr-review.md
│   │       ├── audit-security.md
│   │       └── ui-parity.md
│   └── workflows/
│       ├── codex-pipeline.yml           ← copié DataSaillance
│       ├── pr-review-agent.yml          ← nouveau
│       ├── audit-security.yml           ← nouveau
│       └── ui-parity.yml                ← nouveau
└── codex-jobs.yml                       ← racine, copié DataSaillance
```

---

## Contrats agent (Pydantic v2)

Chaque agent a 2 types : `Input` et `Output`, tous dans `agents/contracts/`.

### Base commune (`agents/contracts/base.py`)

```python
from pydantic import BaseModel, Field
from typing import Literal

class AgentInputBase(BaseModel):
    version: Literal["1.0"] = "1.0"
    task_id: str = Field(..., description="uuid ou timestamp-slug, sert de clé dir work/")
    invoked_by: str = Field(..., description="skill/agent/human qui déclenche")
    work_dir: str = Field(..., description="work/<task-id>/ path relatif repo")

class AgentOutputBase(BaseModel):
    version: Literal["1.0"] = "1.0"
    task_id: str
    agent: str = Field(..., description="nom de l'agent qui a produit")
    status: Literal["success", "partial", "failed", "needs_clarification"]
    summary: str = Field(..., max_length=500, description="<=500 chars, humain-lisible")
    artifacts: list[str] = Field(default_factory=list, description="file paths produits")
    tokens_used: int = 0
    duration_ms: int = 0
    next_recommended: Literal["review", "test", "document", "merge", "human", "none"] | None = None
    blockers: list[str] = Field(default_factory=list, description="ce qui a bloqué si status != success")
```

### Exemples concrets

**`agents/contracts/spec_writer.py`** :
```python
class SpecBrief(AgentInputBase):
    spec_type: Literal["module", "ui", "rgpd"]
    slug: str                              # "p2-sleep", "p5-dashboard-hypnogram"
    phase: Literal["P0","P1","P2","P3","P4","P5","P6"]
    context_files: list[str]               # fichiers source à consulter
    parent_specs: list[str] = []           # specs dont dépend celle-ci
    key_points: list[str]                  # bullets clés à couvrir

class SpecArtifact(AgentOutputBase):
    spec_path: str                         # vault/...spec-<slug>.md
    sections_completed: list[str]          # ["responsabilites", "types", "rgpd", ...]
    issues_opened: list[int] = []          # GitHub issues créées depuis la spec
```

**`agents/contracts/coder.py`** :
```python
class CodeBrief(AgentInputBase):
    spec_path: str
    target: Literal["backend", "android", "frontend"]
    target_files: list[str]
    constraints: list[str] = []
    tests_red_path: str                    # tests existants que l'impl doit faire passer

class CodeArtifact(AgentOutputBase):
    files_modified: list[str]
    diff_path: str                         # work/<task-id>/diff.patch
    tests_green: bool
    test_output_path: str                  # work/<task-id>/test-output.txt
    lint_clean: bool
```

**`agents/contracts/reviewer.py`** :
```python
class ReviewBrief(AgentInputBase):
    branch: str
    spec_path: str
    diff_path: str
    checklist: list[str] = [
        "spec ↔ code alignment",
        "tests cover edge cases",
        "no secrets in code",
        "logging added for new events",
        "audit log written for RGPD-critical paths",
        "HISTORY.md updated",
    ]

class ReviewReport(AgentOutputBase):
    findings: list[dict]                   # {severity, location, description, suggestion}
    overall: Literal["approve", "request_changes", "comment"]
    critical_count: int
    warning_count: int
    suggestion_count: int
```

**`agents/contracts/git_steward.py`** :
```python
class GitOperationBrief(AgentInputBase):
    op_type: Literal["status", "commit", "tag", "checkpoint", "pr", "release", "fix", "audit_post_save"]
    scope: str = ""                        # "phase-a-bootstrap" | "fix-conflict" | "release-v1" | ...
    dry_run: bool = False                  # si True, ne fait que proposer sans exécuter
    auto_approve_safe: list[str] = [       # ops sans risque autorisées sans approval explicite
        "git status", "git fetch", "git diff", "git log",
        "git ls-remote --tags", "gh pr view", "gh pr list",
    ]
    files_changed: list[str] = []          # contexte pour audit_post_save (output Edit/Write)

class GitOperationReport(AgentOutputBase):
    actions_taken: list[dict]              # {cmd, exit_code, stdout_preview}
    actions_proposed: list[dict]           # {cmd, rationale, risk_level}
    warnings: list[str]                    # branche non-conforme, divergence, secrets staged, ...
    requires_human_approval: bool
    history_md_updated: bool               # true si HISTORY.md patché par l'agent
    next_recommended: Literal["commit", "pr", "checkpoint", "fix", "none"] | None = None
```

### Validation

Chaque agent (local ou CI) :
1. Lit `work/<task-id>/brief.json` → parse via Pydantic → rejette si invalide
2. Exécute son travail
3. Écrit `work/<task-id>/result.json` validé par Pydantic
4. L'orchestrateur valide le résultat avant de continuer

---

## Exemple de subagent definition (`.claude/agents/coder-backend.md`)

```markdown
---
name: coder-backend
description: Implémente code Python (FastAPI routers, SQLAlchemy models, services) pour faire passer des tests RED existants. À invoquer après que test-writer a produit les tests. Ne touche ni à Android, ni à static/, ni aux migrations Alembic.
tools: Read, Edit, Write, Grep, Glob, Bash
disallowedTools: WebFetch, WebSearch
model: sonnet
permissionMode: default
maxTurns: 20
color: blue
hooks:
  PreToolUse:
    - matcher: "Write|Edit"
      hooks:
        - type: command
          command: .claude/hooks/enforce-path-scope.sh server
---

Tu es un ingénieur backend Python senior. Ton rôle unique : implémenter du code pour faire passer des tests RED.

**Inputs** — lis `$CLAUDE_TASK_WORK_DIR/brief.json`, il respecte le contrat `CodeBrief` :
- `spec_path` — la spec Obsidian à suivre
- `target_files` — où écrire
- `tests_red_path` — les tests qui doivent passer GREEN à la fin
- `constraints` — contraintes techniques (ex: chiffrement obligatoire sur tel champ)

**Workflow** :
1. Lire la spec Obsidian à `spec_path`
2. Lire les tests à `tests_red_path` — les tests sont le contrat
3. Lire les fichiers existants dans `target_files`
4. Implémenter le minimum de code pour faire passer les tests
5. Lancer `pytest` — itérer jusqu'à GREEN
6. Lancer `ruff check server/` — corriger lint
7. Lancer `mypy server/` — corriger types
8. Produire `work/<task-id>/diff.patch` via `git diff > ...`
9. Écrire `work/<task-id>/result.json` au format `CodeArtifact`

**Règles strictes** :
- Tu ne modifies QUE des fichiers dans `server/` ou `tests/` — pas `android-app/`, pas `static/`, pas `alembic/versions/`
- Si tu as besoin d'une migration DB, échoue avec `status: "needs_clarification"` et demande une invocation de `migration-writer`
- Logging : chaque fonction métier doit logger au moins `start` et `success`/`fail` via `structlog` avec le scope approprié
- Pas de comments explicatifs inutiles — laisse le code parler
- Pas de gestion d'erreur défensive au-delà des boundaries (input user, API externe)

**Contraintes RGPD** (toujours) :
- Jamais de valeur santé brute dans un log
- Chiffrer les champs listés dans la spec via `server.security.crypto.encrypt_field()`
- Toute route santé doit `Depends(get_current_user)` et filtrer par `user_id`

**Si bloqué** : échoue proprement avec `status: "failed"` ou `"needs_clarification"` + `blockers` explicites. Ne devine pas.
```

Les autres agents suivent le même pattern : frontmatter strict, rôle unique, workflow numéroté, règles dures, contrat I/O en JSON.

---

## Pattern d'orchestration (exemple concret)

**Scénario** : livrer le module "sleep" en Phase 2.

### Exécution par session principale (orchestrateur humain + Claude)

```
0. [human] /daily
   → résumé de la phase P2 en cours, branche feat/p2-sleep active

1. [human] /spec module p2-sleep
   → invoke spec-writer en subagent
   → brief.json { spec_type: "module", slug: "p2-sleep", phase: "P2", context_files: [...] }
   → spec-writer écrit vault/.../specs/2026-04-24-spec-p2-sleep.md
   → retourne SpecArtifact { status: "success", spec_path: "...", sections_completed: [...] }
   → [human] review de la spec, édits manuels si nécessaire, passe status=ready

2. [human] /tdd vault/.../specs/2026-04-24-spec-p2-sleep.md
   → invoke test-writer en subagent
   → lit la spec, génère tests/sleep/test_*.py (RED)
   → pytest → tous fail (normal)
   → retourne TestArtifact { tests_red_count: 12, test_files: [...] }

3. [human] /impl vault/.../specs/2026-04-24-spec-p2-sleep.md backend
   → invoke coder-backend en subagent
   → lit spec + tests + target_files
   → itère jusqu'à GREEN
   → retourne CodeArtifact { tests_green: true, files_modified: [...], diff_path: "..." }

   (si échec → retourne needs_clarification avec blockers → human décide)

4. Parallèle (orchestrateur spawn 2 subagents en même temps) :
   a. /review feat/p2-sleep → reviewer sur spec ↔ code ↔ tests
   b. migration-writer (si la spec implique schema change) → alembic revision

5. [orchestrator séquentiel] Si review passe :
   → documenter subagent : update HISTORY.md + feature table + frontmatter spec (status: implemented)

6. [human] /pr
   → PR ouverte avec template rempli, lien spec, tests GREEN, migrations listées

7. [CI auto] pr-review-agent.yml tourne → pr_review_agent.py commente inline sur la PR

8. [human] merge après approbation

9. [CI auto] codex-pipeline.yml → job_runner.py → feature-summary entry dans vault/codex/
```

### Libération de contexte

À chaque `→ invoke <agent>`, la session principale :
- écrit `brief.json` (petit, ~1-2 KB)
- spawn le subagent avec un prompt minimal pointant vers le brief
- reçoit `result.json` (petit, ~1-2 KB) + summary (<500 chars)
- **ne voit pas** : les 50 Read/Write/Bash calls internes, les 500+ lignes de diff, les logs de test

Exemple de trace typique :
- Subagent `coder-backend` consomme ~30-80k tokens dans son propre contexte
- Orchestrateur consomme ~2k tokens pour cette étape (brief + summary)

---

## Guardrails

### 1. Tool scoping par agent

Frontmatter `tools:` + `disallowedTools:`. Exemples :
- `spec-writer` : pas de Bash, pas d'Edit de code → ne peut pas casser le build
- `reviewer` : Read-only → ne peut rien modifier
- `coder-backend` : Write restreint à `server/` et `tests/` via hook `pre-edit-path-scope.sh`

### 2. Hooks PreToolUse

**`.claude/hooks/enforce-path-scope.sh`** — exécuté avant Write/Edit :
```bash
#!/bin/bash
INPUT=$(cat)
TARGET=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
ALLOWED_PREFIX=$1   # passé par l'agent (server/, android-app/, static/, etc.)
if [[ ! "$TARGET" =~ ^$(pwd)/$ALLOWED_PREFIX ]]; then
    echo "Path $TARGET hors scope autorisé ($ALLOWED_PREFIX)" >&2
    exit 2   # blocking
fi
exit 0
```

**`.claude/hooks/pre-bash-destructive.sh`** — bloque les commandes dangereuses :
```bash
# bloque: rm -rf /, git push --force main, git reset --hard HEAD~<n>, docker volume rm, etc.
```

**`.claude/hooks/pre-commit-checkpoint.sh`** — avant un commit multi-fichiers :
```bash
# si > 5 fichiers modifiés, pose un git tag checkpoint-<date>-<scope> automatiquement
```

### 3. Permission mode + settings.json

Dans `.claude/settings.json` :
```json
{
  "hooks": {
    "PreToolUse": [
      { "matcher": "Bash(rm *)", "hooks": [{ "type": "command", "command": ".claude/hooks/pre-bash-destructive.sh" }] },
      { "matcher": "Bash(git push *)", "hooks": [{ "type": "command", "command": ".claude/hooks/block-force-push-main.sh" }] }
    ],
    "PostToolUse": [
      { "matcher": "Edit|Write", "hooks": [{ "type": "command", "command": ".claude/hooks/post-edit-run-linter.sh" }] }
    ],
    "SubagentStop": [
      { "matcher": ".*", "hooks": [{ "type": "command", "command": ".claude/hooks/post-subagent-validate-contract.sh" }] }
    ]
  }
}
```

Le `post-subagent-validate-contract.sh` vérifie que `result.json` est valide Pydantic avant de rendre la main à l'orchestrateur.

### 4. Git tag checkpoints

Avant toute invocation de coder qui touche > 3 fichiers, hook `pre-subagent-spawn.sh` pose automatiquement `checkpoint-before-<agent>-<task-id>-<date>`. Rollback = `git reset --hard <tag>`.

---

## Fail handling

### Échelle d'escalade

1. **Agent échoue (status=failed ou needs_clarification)**
   - Orchestrateur lit `blockers`
   - Si clarifiable automatiquement : re-spawn l'agent avec contexte ajouté (max 2 retries)
   - Sinon : escalade human

2. **Contrat non validé (result.json invalide)**
   - Hook `post-subagent-validate-contract.sh` sort avec exit 2
   - Orchestrateur re-spawn avec instruction "ton output précédent ne respectait pas le schema `<X>`, voici les erreurs : ..."

3. **Boucle détectée (même outil/args appelé > 3 fois)**
   - `maxTurns` stoppe l'agent (frontmatter)
   - Orchestrateur escalade human

4. **Rollback nécessaire**
   - `git reset --hard checkpoint-before-<agent>-<task-id>` + log dans HISTORY entry `[ROLLBACK]`

### Retry strategy (code, `agents/orchestrator/retry.py`)

```python
def retry_with_context(agent_name: str, brief: AgentInputBase, last_failure: AgentOutputBase, max_retries: int = 2) -> AgentOutputBase:
    for attempt in range(max_retries):
        brief_enriched = brief.model_copy(update={
            "prior_attempts": [*getattr(brief, "prior_attempts", []), {
                "status": last_failure.status,
                "blockers": last_failure.blockers,
                "summary": last_failure.summary,
            }]
        })
        result = spawn_subagent(agent_name, brief_enriched)
        if result.status == "success":
            return result
        last_failure = result
    return last_failure  # escalade
```

---

## Observabilité

### Per-agent trace

`work/<task-id>/trace.jsonl` — append-only :
```json
{"ts":"2026-04-24T14:02:01Z","event":"agent_start","agent":"coder-backend","task_id":"...","model":"sonnet"}
{"ts":"...","event":"tool_call","tool":"Read","args":{"file_path":"..."}}
{"ts":"...","event":"tool_result","tool":"Read","bytes":12400,"truncated":false}
{"ts":"...","event":"tool_call","tool":"Bash","args":{"command":"pytest tests/sleep/"}}
{"ts":"...","event":"tool_result","tool":"Bash","exit_code":1,"stderr_bytes":880}
{"ts":"...","event":"agent_end","agent":"coder-backend","status":"success","tokens_in":12400,"tokens_out":4200,"duration_ms":82000}
```

### Dashboard agrégé (nice-to-have P6+)

`agents/tools/agent_stats.py` :
```bash
python -m agents.tools.agent_stats --since "2026-04-23" --agent coder-backend
# → tableau : runs, success_rate, avg_tokens, avg_duration_ms, p95_duration_ms
```

Détecte les agents qui dérivent (success rate ↓, tokens ↑) et alerte.

### Token budget

Dans `settings.json` :
```json
{
  "env": {
    "CLAUDE_MAX_TOKENS_PER_TASK": "100000",
    "CLAUDE_AUTOCOMPACT_PCT_OVERRIDE": "50"
  }
}
```

Agent qui dépasse son budget → échec automatique, escalade.

---

## Intégration CI : agents externes

### PR Review Agent

**Trigger** : `.github/workflows/pr-review-agent.yml` sur `pull_request.opened` et `synchronize`

**Script** `.github/scripts/pr_review_agent.py` :
```python
# Pseudo-code
from anthropic import Anthropic

def run_pr_review(pr_number: int, diff: str, files_changed: list[str]):
    spec_path = find_spec_for_pr(pr_number)  # via PR body "Spec: vault/..."
    spec_content = fetch_spec(spec_path)
    template = load("/.github/templates/agents/pr-review.md")
    prompt = template.format(
        diff=diff[:50000],              # truncate
        files=files_changed,
        spec=spec_content,
        checklist=CHECKLIST,
    )
    client = Anthropic()
    response = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=4096,
        messages=[{"role":"user","content":prompt}],
        system="You are a strict code reviewer. Output JSON matching the schema.",
    )
    report = parse_json(response)       # schema: ReviewReport
    post_github_review(pr_number, report)
```

Output : review sur GitHub avec findings inline, verdict `approve|request_changes|comment`, label `tested` ajouté si verdict=approve.

### Recurring agents (cron)

Workflows `.github/workflows/recurring-*.yml` tournent le 1er du mois :
- `audit-security-agent.yml` → `audit_security_agent.py` analyse CVEs, crée issues triées P0/P1/P2
- `audit-deps-agent.yml` → `audit_deps_agent.py` analyse outdated, propose MAJ prioritaires
- `audit-perf-agent.yml` → analyse Lighthouse + bundle size

### Codex pipeline (réutilisé tel quel depuis DataSaillance)

`codex-pipeline.yml` → `extractor.py` → `job_runner.py` → entrées Codex auto-générées dans `docs/vault/codex/`.

**Adaptation** : `codex-jobs.yml` à la racine, jobs :
- `feature-summary` (PR `feat` mergée → dev → `vault/.../codex/feature/{N}-{slug}.md`)
- `bug-fix`, `migration`, `adr` — même pattern
- **Nouveau** : `spec-implemented` — quand une PR ferme une issue avec label `has-spec`, update frontmatter de la spec (status: implemented, commits: [...])

---

---

## Observabilité & monitoring du harness

> Sans cette couche, les agents sont des boîtes noires. Avec elle, chaque appel, retry, feedback humain est navigable comme on navigue un `git log --graph`.

### Vision : agent-run = git-graph

L'analogie que tu as formulée est une **contribution originale** (aucun outil mainstream ne ship ça). Mapping :

| Git concept | Agent harness concept |
|-------------|----------------------|
| Repository | Un "run" d'orchestrateur (`conversation_id`) |
| Branche `main` | Orchestrateur (session principale) |
| Branches latérales | Subagents spawned (coder, tester, reviewer, ...) |
| Commits | Tool calls / LLM turns dans un agent |
| Merge commit | Agent termine et rend à l'orchestrateur (result.json) |
| Tags | Checkpoints (rollback points, git tag réels sur le repo) |
| PR | Livrable final d'un agent → revue humaine |
| Force-push / reset | Retry qui abandonne un sous-arbre |
| `HEAD` | Agent actif courant |

Rendu : **Mermaid `gitGraph`** pour les vues summary (docs, Obsidian), **Phoenix/Langfuse tree view** pour le debug détaillé.

### Stack choisie : Arize Phoenix self-hosted

Comparatif contre Langfuse et DIY (voir `glittery-napping-rossum-agent-a5dc04e66912dc995.md` pour détails) :

| Critère | Phoenix ✅ | Langfuse | DIY (ClickStack+Grafana) |
|---------|-----------|----------|--------------------------|
| Ops simplicity | 1 container Docker | 3 containers (Postgres + ClickHouse + Redis) | 5+ containers |
| License | Apache 2.0, full OSS | MIT core (some features paid) | Apache/MIT mixed |
| Claude Agent SDK instrumentor built-in | ✅ | ❌ (OTel générique) | ❌ |
| Agent tree UI | ✅ natif, replay + session evals | ✅ | ❌ (il faut builder) |
| OTel GenAI compliant | ✅ + OpenInference extensions | ✅ | via OTel Collector |
| Alerts runtime natifs | ⚠️ faible (API polling) | ⚠️ roadmap | ✅ Grafana alerting |

**Décision** : Phoenix pour la phase 1. Alerts comblées par un tiny sidecar Python qui poll l'API Phoenix et pousse vers GitHub/mail. Si un jour on veut des alerts natifs + cost dashboards riches, on migre vers Langfuse (même format OTel, migration = relancer un container).

**Pourquoi pas les deux en parallèle** : un seul UI à maintenir, un seul endroit à consulter. Simplicité > features pour un projet perso.

### Schéma wire : OTel GenAI semantic conventions

Chaque agent émet des spans OTel au format standard 2026 :

```
invoke_agent orchestrator                              (ROOT, gen_ai.conversation.id=<run_id>)
├── invoke_agent spec-writer                          (attempt=1)
│   ├── execute_tool Read                             (file_path=vault/...)
│   ├── execute_tool Write                            (file_path=vault/.../spec-xxx.md)
│   └── [agent_result status=success]
├── invoke_agent test-writer                          (attempt=1)
│   ├── execute_tool Read
│   ├── execute_tool Write (tests/sleep/test_*.py)
│   ├── execute_tool Bash (pytest → exit 1 = RED expected)
│   └── [agent_result status=success]
├── invoke_agent coder-backend                        (attempt=1)
│   ├── execute_tool Bash (pytest → exit 1)
│   ├── execute_tool Edit
│   ├── execute_tool Bash (pytest → exit 0 GREEN)
│   └── [agent_result status=success]
├── invoke_agent reviewer                             (attempt=1) ∥ parallel
│   └── [agent_result status=request_changes, 1 blocker]
├── invoke_agent human_review                         (kind=INTERNAL, status=pending)
│   └── [human.decision=approve après 45min]
├── invoke_agent coder-backend                        (attempt=2, contexte enrichi)
│   └── [agent_result status=success]
└── invoke_agent documenter                           (attempt=1)
    └── [agent_result status=success]
```

**Attributs clés** :
- `gen_ai.conversation.id` — le glue qui relie tout
- `gen_ai.agent.name`, `gen_ai.agent.version`
- `gen_ai.usage.input_tokens`, `gen_ai.usage.output_tokens`, `gen_ai.usage.cost_usd`
- Custom : `harness.task_id`, `harness.spec_path`, `harness.attempt`, `human.decision`

### Monitoring stack (mini "dev-tracking" à la DataSaillance)

Port direct du pattern DataSaillance `apps/data-engine/src/data_engine/dev_tracking/` — adapté pour ingérer **à la fois** :
- Les JSONL natifs Claude Code (`~/.claude/projects/<slug>/*.jsonl`) → sessions utilisateur
- Les traces OTel des subagents (via Phoenix OTLP endpoint) → runs d'agents

**Schéma Postgres dédié** (tables `harness_*` séparées des health tables RGPD — pas de données santé là-dedans, purement télémétrie dev) :

```sql
-- Un run = une invocation d'orchestrateur (une session Claude Code, ou un job CI)
CREATE TABLE harness_runs (
    id UUID PRIMARY KEY,
    conversation_id TEXT NOT NULL UNIQUE,        -- gen_ai.conversation.id
    session_type TEXT NOT NULL,                  -- 'interactive' | 'ci' | 'recurring'
    trigger TEXT,                                -- 'user' | 'pr_opened' | 'cron_security_audit'
    root_agent TEXT NOT NULL,                    -- 'orchestrator' | 'pr-review-agent' | ...
    started_at TIMESTAMPTZ NOT NULL,
    stopped_at TIMESTAMPTZ,
    status TEXT NOT NULL,                        -- 'running' | 'success' | 'failed' | 'stuck' | 'killed'
    git_branch TEXT,
    git_commit TEXT,
    task_id TEXT,                                -- harness.task_id (lien vers work/<id>/)
    spec_path TEXT,
    tokens_total INT DEFAULT 0,
    cost_usd_total NUMERIC(10,4) DEFAULT 0,
    attempts_total INT DEFAULT 0,
    pr_number INT,
    issue_number INT,
    tags TEXT[],
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Un span = un invoke_agent ou un execute_tool
CREATE TABLE harness_spans (
    id UUID PRIMARY KEY,
    run_id UUID REFERENCES harness_runs ON DELETE CASCADE,
    span_id TEXT NOT NULL,                       -- OTel span_id
    parent_span_id TEXT,                         -- OTel parent
    span_kind TEXT NOT NULL,                     -- 'invoke_agent' | 'execute_tool' | 'human_review'
    name TEXT NOT NULL,                          -- agent name ou tool name
    started_at TIMESTAMPTZ NOT NULL,
    duration_ms INT,
    status TEXT,                                 -- 'ok' | 'error' | 'pending' | 'killed'
    attempt INT DEFAULT 1,                       -- retry count for this span
    model TEXT,                                  -- claude-sonnet-4-6, etc.
    tokens_in INT,
    tokens_out INT,
    cost_usd NUMERIC(10,6),
    tool_input JSONB,                            -- pour les tools : les args
    tool_output_preview TEXT,                    -- truncated output preview
    tool_output_path TEXT,                       -- full output saved in work/<task_id>/
    human_decision TEXT,                         -- approve | request_changes | reject
    blockers TEXT[],
    attributes JSONB                             -- tous les OTel attributes
);
CREATE INDEX idx_spans_run ON harness_spans(run_id);
CREATE INDEX idx_spans_parent ON harness_spans(parent_span_id);
CREATE INDEX idx_spans_name ON harness_spans(name);

-- Messages extraits des JSONL Claude Code (pour session replay comme DataSaillance)
CREATE TABLE harness_messages (
    id UUID PRIMARY KEY,
    run_id UUID REFERENCES harness_runs ON DELETE CASCADE,
    session_id TEXT NOT NULL,
    message_index INT NOT NULL,
    role TEXT NOT NULL,                          -- 'user' | 'assistant' | 'system'
    content TEXT,                                -- FTS-searchable
    blocks JSONB,                                -- tool_use, tool_result, thinking, text
    timestamp TIMESTAMPTZ NOT NULL,
    search_vector TSVECTOR,
    UNIQUE(session_id, message_index)
);
CREATE INDEX idx_messages_run ON harness_messages(run_id);
CREATE INDEX idx_messages_fts ON harness_messages USING GIN(search_vector);

-- Entities extraites des messages (issues, PRs, commits, files, specs Obsidian, URLs)
CREATE TABLE harness_entities (
    id UUID PRIMARY KEY,
    entity_type TEXT NOT NULL,                   -- issue | pr | commit | file | spec | url | agent
    entity_id TEXT NOT NULL,                     -- "142" | "abc1234" | "server/routers/sleep.py"
    source_type TEXT NOT NULL,                   -- span | message | run
    source_id UUID NOT NULL,
    run_id UUID REFERENCES harness_runs,
    timestamp TIMESTAMPTZ NOT NULL,
    metadata JSONB,
    UNIQUE(entity_type, entity_id, source_type, source_id)
);
CREATE INDEX idx_entities_run ON harness_entities(run_id);
CREATE INDEX idx_entities_type_id ON harness_entities(entity_type, entity_id);

-- Alerts déclenchées (pour ne pas spammer — dedup par rule+key+window)
CREATE TABLE harness_alerts (
    id UUID PRIMARY KEY,
    run_id UUID REFERENCES harness_runs,
    rule TEXT NOT NULL,                          -- 'retry_threshold' | 'budget_exceeded' | 'loop' | 'stuck'
    severity TEXT NOT NULL,                      -- 'info' | 'warning' | 'critical'
    dedup_key TEXT NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    notified_channels TEXT[],                    -- ['github', 'email', 'slack']
    context JSONB,
    UNIQUE(rule, dedup_key)
);
```

### Endpoints backend (FastAPI, préfixe `/api/harness`)

Copié/adapté du pattern DataSaillance `/dev-tracking/*` :

| Endpoint | Méthode | Rôle |
|----------|---------|------|
| `/api/harness/otlp` | POST | Receiver OTLP (proxy vers Phoenix + ingestion dans harness_spans) |
| `/api/harness/ingest-jsonl` | POST | Ingère les JSONL Claude Code d'une session (SSE stream de progress) |
| `/api/harness/runs` | GET | Liste runs paginée/filtrable (status, agent, date range, conversation_id) |
| `/api/harness/runs/{run_id}` | GET | Détail d'un run avec spans |
| `/api/harness/runs/{run_id}/tree` | GET | Arbre d'appels en format JSON (pour rendu Mermaid gitGraph ou React tree) |
| `/api/harness/runs/{run_id}/mermaid` | GET | Mermaid `gitGraph` string prêt à render |
| `/api/harness/runs/{run_id}/messages` | GET | Messages paginés (session replay) |
| `/api/harness/runs/{run_id}/messages/search` | GET | FTS dans messages du run |
| `/api/harness/spans/{span_id}` | GET | Détail span : tool_input, tool_output (full depuis work/) |
| `/api/harness/spans/{span_id}/edit` | PATCH | Annotation/edition humaine d'un span (notes, reclassification) |
| `/api/harness/alerts` | GET | Liste alerts ouvertes + historique |
| `/api/harness/alerts/{id}/resolve` | POST | Marquer résolu |
| `/api/harness/stats/daily` | GET | Agrégats journaliers (runs, tokens, cost, success rate par agent) |

### Visualisation (Compose shell + WebView dashboard admin)

Nouveau chapitre du dashboard : `/admin/harness/`

1. **Timeline view** (à la DataSaillance dev-tracking-timeline)
   - Groupement par jour
   - Un bloc par run : status color-coded, root agent name, duration, tokens, cost
   - Filtres : branche git, type (interactive/ci/recurring), status, date range
   - Click run → détail

2. **Run detail view** — le cœur de la navigation
   - Header : conversation_id, root agent, spec_path, git_branch, PR link, status
   - **Mermaid gitGraph** pour la vue git-tree (summary) :
     ```mermaid
     gitGraph
       commit id:"start"
       branch spec-writer
       commit id:"Read spec"
       commit id:"Write spec.md"
       checkout main
       merge spec-writer tag:"spec ready"
       branch test-writer
       commit id:"Write tests"
       checkout main
       merge test-writer
       branch coder-backend
       commit id:"pytest RED"
       commit id:"Edit router"
       commit id:"pytest GREEN"
       checkout main
       merge coder-backend tag:"impl ok"
     ```
   - **Span tree** React/D3 pour la vue détaillée (expand/collapse, timing)
   - **Timeline horizontale** (gantt) — chaque span sur une ligne temporelle, parallélisme visible
   - **Chat replay** (copie DataSaillance `dev-tracking-chat-reader.tsx`) avec accordion pour tool_use+tool_result
   - **Cost breakdown** : tokens/cost par agent, sparkline
   - **Edit annotations** : humain peut ajouter note sur un span ("cet échec = cause X", "retry justifié"), persisted en metadata

3. **Alerts tray**
   - Liste alerts ouvertes, couleur par severity
   - Actions : acknowledge, resolve, link to run
   - Notification count dans le topbar

4. **Stats dashboard**
   - Success rate par agent (7j, 30j, all-time)
   - Tokens/cost par agent
   - Avg duration, p95 duration
   - Runs par type (interactive vs CI vs recurring)

**Frontend** : réutiliser `static/` pour le dashboard admin web (itération rapide). Pour la version Compose native prod, rendu équivalent en Compose Canvas (respecte le dual-track UI du master plan).

### Règles d'alerting

Implémenté dans `agents/orchestrator/alerts.py`, polling API Phoenix + checks directs sur DB :

| Rule | Condition | Dedup key | Severity | Channels |
|------|-----------|-----------|----------|----------|
| `retry_threshold` | Un agent a `attempt >= 2` | `{run_id}:{agent}` | warning | GitHub issue comment (si run lié à PR) + email digest |
| `retry_escalate` | Un agent a `attempt >= 3` | `{run_id}:{agent}` | critical | GitHub issue dédiée + email immédiat |
| `budget_exceeded` | Un run dépasse `CLAUDE_MAX_TOKENS_PER_TASK` | `{run_id}` | critical | Kill run + email + GitHub comment |
| `loop_detected` | Même tool+args appelé > 3 fois | `{run_id}:{agent}:{tool_hash}` | critical | Kill agent + email |
| `stuck_run` | Run en status `running` depuis > 30min sans nouveau span | `{run_id}` | warning | Email digest |
| `review_blocker_age` | PR en `request_changes` depuis > 24h liée à un run | `{pr_number}` | info | GitHub comment rappel + email digest |
| `cost_daily` | Sum cost > $10/jour | `day:{date}` | warning | Email digest |
| `human_review_pending` | Span `human_review` en status pending depuis > 2h | `{span_id}` | warning | GitHub issue comment |

**Canaux** :
- GitHub (issue comment, issue create) — natif au workflow
- Email (SMTP Mailgun/SES en prod, Mailpit en dev) — digests quotidiens
- Slack/Discord webhook — optionnel, si dispo
- Dashboard badge — count visible en permanence

### Budget enforcement (pas juste alerting — leçon $47k)

**Hard ceiling au niveau du LLM client middleware**, pas de l'observabilité :

```python
# agents/orchestrator/budget.py
class BudgetEnforcer:
    def __init__(self, max_tokens_per_run: int, max_cost_usd_per_day: float):
        self.max_tokens_per_run = max_tokens_per_run
        self.max_cost_usd_per_day = max_cost_usd_per_day

    async def pre_call(self, run_id: str, estimated_tokens: int) -> None:
        current = await self.get_run_tokens(run_id)
        if current + estimated_tokens > self.max_tokens_per_run:
            raise BudgetExceeded(f"Run {run_id} would exceed per-run ceiling")
        daily_cost = await self.get_daily_cost()
        if daily_cost >= self.max_cost_usd_per_day:
            raise BudgetExceeded(f"Daily cost ceiling hit: ${daily_cost}")
```

Tous les agents (locaux + CI) wrappent leurs appels API derrière `BudgetEnforcer.pre_call()`. Dépassement = exception, pas warning.

---

## Phases d'implémentation (méta-projet révisé)

### Phase A — Foundation agents (avant P0 master)
**Durée** : 2-3 jours

- `agents/contracts/base.py` + tous les sous-contrats Pydantic (incl. `git_steward.py`, `pentester.py`, `plan_keeper.py`)
- `.claude/agents/` — spec-writer, test-writer, coder-backend, reviewer, documenter, **git-steward**, **pentester**, **plan-keeper**
- `.claude/hooks/` — enforce-path-scope, pre-bash-destructive, post-subagent-validate-contract, **post-edit-git-steward** (auto-invoke audit après save), **pre-commit-plan-keeper** (block si déviation `severity >= medium`)
- `.claude/settings.json` étendu (hooks + env vars + `CLAUDE_MAX_TOKENS_PER_TASK`)
- `.claude/skills/` — spec, tdd, impl, debug (nouveaux) + **git-status, commit, checkpoint, git-fix, pentest, align** (nouveaux) + linked-list `next_default` dans frontmatter
- `work/` gitignored + `**/poc/` redondance gitignored
- `agents/orchestrator/budget.py` — BudgetEnforcer
- `agents/README.md`
- `.githooks/pre-push` — autoriser `feat/|fix/|chore/|hotfix/|release/|refactor/|spike/` (élargi vs version initiale)
- `Makefile` — target `security-install` (bandit, pip-audit, safety, semgrep) + `pentest` (escape hatch)

**Test** : exécuter scénario "spec → tdd → impl → review (reviewer + pentester en parallèle) → align → commit → pr" sur micro-module factice, vérifier contrats + scoping + budget + linked-list skill suggestion + git-steward auto-audit post-save + pentester refuse exécution POC en Phase A + plan-keeper bloque commit si déviation medium+.

### Phase A.5 — Code-as-vault (peut démarrer en parallèle de Phase A étapes 3+)
**Durée** : 4-5 jours
**Plan dédié** : [[2026-04-23-plan-code-as-vault]]

Subagent `code-cartographer` + module Python `agents/cartographer/` (markers, walker tree-sitter, annotation_io, marker_injector, anchor_resolver, note_renderer, orphan_detector, index_generator, cli, **coverage_map** ← Phase A.7) + skills `/sync-vault`, `/annotate`, `/anchor-review`, **`/sync-coverage`** ← Phase A.7) + hook `.githooks/pre-commit` + bootstrap initial des ~34 notes vault Python+JS+Kotlin + générateur changelog 30 derniers commits + plan-keeper extension (3 deviation_types vault).

**Test** : créer annotation `--at server/x.py:42` → marqueur `# @vault:slug` injecté → annotation file créée → note vault rendue avec callout interleaved au bon endroit. Modifier code source → pre-commit régénère note. Supprimer marqueur → orphan détecté → `/anchor-review` propose recovery.

### Phase A.6 — Annotation suggester (2-3 jours, après A.5) — ✅ livré 2026-04-23
Subagent `annotation-suggester` + hook post-commit + heuristics (commit refs issue, complexité diff, mots-clés workaround/perf/rgpd/fix). Voir commits `a3f9c30`+.

### Phase A.8 — Specs in vault (5 blocs, ~3h, après A.7) — ✅ livré 2026-04-23
**Objectif** : specs first-class dans le vault, links bidirectionnels spec ↔ tests + spec ↔ code, discipline spec-first.

**Livrables** :
- Migration 7 specs PKM → `docs/vault/specs/` (single source of truth dans le repo) + stubs PKM avec wikilinks
- `agents/contracts/spec.py` (`SpecMeta`/`SpecImplements`/`SpecTestedBy` Pydantic) + 5 re-exports
- `agents/cartographer/spec_indexer.py` — `load_spec`, `build_index`, `discover_spec_paths`, `detect_implements_drift`, `untested_specs`
- `note_renderer` étendu : section "Implements specs" (code → spec), section "Validates specs" (test → spec), section "Targets" (spec → code+tests)
- Spec-summary notes auto dans `code/specs/` (mirror des sources)
- `index_generator.generate_specs_index()` → `_index/specs.md` (table + Untested specs)
- `plan-keeper` +2 deviation types (`spec_implements_drift`, `untested_spec`)
- Skill `/spec` génère squelette frontmatter + body Vision/Décisions/Livrables/Tests
- **Discipline spec-first** : 1 spec ≈ 1 US/feature livrable < 1 semaine ; 1 PR = 1 spec ; tests déclarés `tested_by:` côté spec (top-down). Vocabulaire : `plan` = méta-architecture multi-semaines, `spec` = unitaire, `us`/`feature` agile

Commits : `ac24832` → `14a129a` → `728ea5d` → `1b82456` → `52c5fc9`.

### Phase A.7 — Test ↔ code linking (4 blocs, ~2h, après A.6) — ✅ livré 2026-04-23
**Objectif** : naviguer dans une note vault de code et voir directement quels tests touchent chaque symbole/range.

**Livrables** :
- `tests/**/*.py`, `android-app/**/test/**/*.kt` ajoutés à `DEFAULT_SOURCE_GLOBS` du cartographer (+15 notes vault tests)
- `agents/cartographer/coverage_map.py` — wrapper `pytest --cov` avec `dynamic_context = test_function` + parser JSON → manifest 3 vues (`by_symbol`, `by_test`, `by_file`)
- `.claude/skills/sync-coverage/SKILL.md` — invoque le générateur, gitignore le manifest
- `note_renderer` étendu : frontmatter `coverage_pct`, appendix Symbols liste `Tested by (N)`, callout annotation reçoit sub-callout `> [!test]+ Tested by` (intersection range), test files reçoivent section `## Exercises` (inverse map)
- `index_generator.generate_coverage_map_index()` → `_index/coverage-map.md` (table par fichier + Untested symbols)
- `.github/workflows/coverage.yml` — CI build manifest + threshold gate opt-in (`vars.COVERAGE_MIN_PCT`) + upload artifact 14j
- Fix bonus : walker UTF-8 byte slicing (symboles tronqués réparés)

**Coverage manifest gitignored** (`docs/vault/_index/coverage-map.json`, `coverage.json`) — regénéré par dev local via `/sync-coverage` ou par CI dans artifact.

Commits : `5108be3` → `0bcd477` → `049b24f` → `6bbbdb2` (+ HISTORY `affbaaa`).

### Phase B — Observabilité & monitoring (après P0 master — dépend de Postgres + FastAPI)
**Durée** : 4-5 jours

1. **Phoenix self-hosted** — ajouté à `docker-compose.yml` (1 container, port 6006)
2. **OTel instrumentation** — setup Claude Code avec `OTEL_EXPORTER_OTLP_ENDPOINT=http://phoenix:6006` + OpenInference Claude Agent SDK instrumentor
3. **Backend `/api/harness/*`** — tables Postgres `harness_*`, endpoints, FTS, SSE ingestion
4. **JSONL ingestion worker** — port DataSaillance `dev_tracking/jsonl_parser.py` + `reconstruction.py` (adapter schéma)
5. **Entity extractor** — port DataSaillance `dev_tracking/entity_extractor.py` + ajouter types SamsungHealth (spec, agent, run)
6. **Trace-tree → Mermaid gitGraph transformer** — `agents/tools/trace_to_mermaid.py` (~200 LOC)
7. **Dashboard admin `/admin/harness/*`** — timeline, run detail, chat replay, stats
8. **Alerts module** — `agents/orchestrator/alerts.py` + cron poll Phoenix API + GitHub/email dispatch
9. **BudgetEnforcer** branché sur tous les subagents

**Test** : lancer 10 runs factices, vérifier ingestion complète, navigation timeline → run → span → tool detail, alert déclenche sur retry>=2, Mermaid rendu cohérent avec la réalité.

### Phase C — CI agents (après P1 master)
**Durée** : 2-3 jours

- `.github/scripts/` — extractor.py, job_runner.py (copie DataSaillance)
- `.github/templates/` — codex + agents
- `.github/workflows/` — codex-pipeline.yml, pr-review-agent.yml, audit-security.yml
- `codex-jobs.yml` racine
- Secrets : `ANTHROPIC_API_KEY`, `PHOENIX_API_KEY`, `VAULT_SYNC_TOKEN`
- **Les agents CI émettent aussi OTel vers Phoenix** → même vue monitoring pour local + CI

### Phase D — Android agents (avant P4 master)
**Durée** : 1-2 jours

- `.claude/agents/coder-android.md`, `ui-parity-checker.md`
- `.github/scripts/parity_ci_agent.py`
- `.github/workflows/ui-parity.yml`

### Phase E — Observabilité avancée (après P2 master)
**Durée** : 2-3 jours

- Session replay UI complet (timeline scrubber, step-through)
- Run-over-run diff (comparer 2 runs du même type)
- Export d'un run en markdown pour post-mortem
- Webhook configurable vers Slack/Discord
- Dashboard mobile (Compose native, miroir du web)

**Total méta-projet** : ~11-16 jours étalés sur les 6-8 semaines du master plan. Phase A bloque P0, Phase B bloque C/D/E, le reste est parallélisable.

---

## Intégration avec le master plan

| Master plan phase | Méta-projet dépendance |
|-------------------|------------------------|
| P0 Infra | **Phase A obligatoire avant** (agents nécessaires pour TDD) |
| P0 Infra | Phase B démarre dès que Postgres + FastAPI up |
| P1 Auth | Phase B complétée pendant P1 |
| P2 Data layer | Phase C démarre (review agent sur chaque PR P2) |
| P4 Android | Phase D obligatoire avant (coder-android) |
| P5 Compose | Phase E démarre (observabilité des tests de parité) |

**Principe** : dès P0 complet, **tous** les runs Claude Code + CI sont tracés, visibles dans `/admin/harness/`, alertables. La dette technique d'observabilité est payée une fois au début, pas à la fin.

---

## Fichiers critiques

| Fichier | Pourquoi |
|---------|----------|
| `agents/contracts/base.py` | Types I/O partagés ; un bug ici casse tous les agents |
| `agents/orchestrator/budget.py` | Hard ceiling pré-exécution ; c'est lui qui empêche le $47k runaway, pas les alerts |
| `agents/orchestrator/retry.py` | Stratégie retry avec contexte d'échec ; mal calibré = coût token explose |
| `agents/orchestrator/alerts.py` | Règles d'alerting + dispatch GitHub/email ; la couche feedback humain passe par là |
| `agents/tools/trace_to_mermaid.py` | Trace-tree → Mermaid gitGraph ; c'est la vue navigation git-analogy unique au projet |
| `.claude/agents/coder-backend.md` | Agent le plus invoqué ; dérive du system prompt = dérive du backend |
| `.claude/hooks/enforce-path-scope.sh` | Garde-fou unique contre les agents qui écrivent hors scope |
| `.claude/hooks/post-subagent-validate-contract.sh` | Validation contrat Pydantic post-run ; si elle loupe, orchestrateur consomme outputs invalides |
| `.claude/settings.json` | Env vars OTel + hooks + budget (`CLAUDE_MAX_TOKENS_PER_TASK`) |
| `server/routers/harness.py` | Endpoints `/api/harness/*` — ingestion OTel + JSONL, queries runs/spans/messages |
| `server/workers/jsonl_ingestor.py` | Parse les JSONL Claude Code et upsert en DB ; port DataSaillance |
| `docker-compose.yml` | Phoenix + Postgres + FastAPI + Caddy + Mailpit ; Phoenix ajouté ici |
| `.github/scripts/job_runner.py` | Pattern CI pour agents externes ; émet aussi OTel vers Phoenix |
| `codex-jobs.yml` | Source de vérité des triggers CI Codex |

---

## Vérification end-to-end post-implémentation

### Agents (Phase A)
1. **Subagent scoping** : `/spec module p0-infra-foundation` → spec-writer spawn → vérifie `work/<task-id>/trace.jsonl` : aucun Bash, aucun Write hors vault
2. **Tests RED gatekeeping** : `/tdd <spec>` → tests écrits → `pytest` → tous FAIL (RED), puis `/impl` → GREEN. `/impl` avant `/tdd` : skill refuse
3. **Path scope enforcement** : coder-backend tente écrire `android-app/Foo.kt` → hook exit 2, agent `needs_clarification`
4. **Contract validation** : result.json avec champ manquant → hook post-subagent-validate-contract bloque
5. **Retry avec contexte** : forcer un échec → retry auto avec contexte enrichi → succès 2ème tentative
6. **Budget enforcement (pré-exécution)** : `CLAUDE_MAX_TOKENS_PER_TASK=10000` → coder-backend bloqué AVANT API call qui dépasserait (pas après)

### Observabilité (Phase B)
7. **Phoenix running** : `docker-compose up` → http://localhost:6006 → UI Phoenix accessible
8. **OTel flow** : lancer `/spec` → Phoenix affiche arbre `invoke_agent orchestrator` → `invoke_agent spec-writer` → tool calls
9. **Ingestion JSONL** : `POST /api/harness/ingest-jsonl` avec session_id → SSE stream progress → table `harness_runs` + `harness_messages` + `harness_spans` + `harness_entities` peuplées
10. **Dashboard admin timeline** : `http://localhost:8001/admin/harness/` → runs groupés par jour, filtres fonctionnels
11. **Run detail + Mermaid** : clic sur un run → Mermaid gitGraph rendu correctement → branches = agents, commits = tool calls, merges = results
12. **Chat replay** : navigation message par message, accordion tool_use+tool_result, FTS search
13. **Span edit** : ajouter annotation "retry justifié" sur un span → persisté → visible à la recharge
14. **Alerts** :
    - Forcer 2 retries → alert `retry_threshold` → GitHub issue comment posté (si run lié PR) + email
    - `CLAUDE_MAX_TOKENS_PER_TASK=5000` → run kill automatique → alert `budget_exceeded` → email
    - Simuler loop (même tool×3) → alert `loop_detected` → agent killed
    - PR laissée en `request_changes` > 24h (simuler avec time travel DB) → alert `review_blocker_age`
15. **Stats** : après 20 runs, `GET /api/harness/stats/daily` → tokens/cost/success rate par agent cohérents

### CI (Phase C)
16. **CI review agent** : PR ouverte → review agent commente <60s → verdict cohérent avec spec
17. **Codex pipeline** : PR `feat` mergée → entry Codex dans vault <2 min → frontmatter spec mise à jour
18. **CI → Phoenix** : run de pr-review-agent.py visible dans Phoenix avec même format que runs locaux

### Git-graph analogy
19. **Mermaid gitGraph** d'un run complexe (3 retries + 1 review human + 1 merge) → rendu correct, human approval visible comme merge commit taggé
20. **Rollback via tag** : coder-backend modifie 5 fichiers → hook pose `checkpoint-before-coder-<task>-<date>` → `git reset --hard <tag>` → repo propre + entry `[ROLLBACK]` dans HISTORY.md

---

## Ce que l'architecture multi-agents N'EST PAS

- **Pas un state machine LangGraph** — overkill à notre échelle, on reste sur orchestrateur = session principale + subagents stateless
- **Pas un remplaçant humain** — human-in-the-loop obligatoire pour les merges, les ADR, les specs en status `ready`, les rollbacks
- **Pas une solution miracle cost-wise** — les subagents consomment des tokens. L'optimisation vient de 2 leviers : (1) chaque agent a un contexte isolé donc démarre cold, (2) modèle choisi par rôle (Haiku / Sonnet / Opus). Mesurer avant d'optimiser
- **Pas une architecture figée** — on commence avec 10 agents locaux + 4 CI. Si on voit qu'un agent est jamais utilisé ou systématiquement mal calibré, on le retire ou fusionne

---

## Estimation méta-projet

| Phase | Durée | Dépendance master plan |
|-------|-------|------------------------|
| Phase A — Foundation agents (8 subagents) | 3-4j | **À faire avant P0 master** |
| Phase A.5 — Code-as-vault (cartographer + miroir docs/vault/) | 4-5j | Parallèle Phase A étapes 3+, plan dédié [[2026-04-23-plan-code-as-vault]] |
| Phase A.6 — Annotation suggester | 2-3j | Après A.5 |
| Phase B — CI agents | 2-3j | Après P1 master (repo a l'auth qui va alimenter les scripts) |
| Phase C — Android agents | 1-2j | Avant P4 master |
| Phase D — Observabilité | 1-2j | Après P2 master (quand on a assez de runs) |
| **Total** | **~6-10j** étalés sur les 6-8 semaines du master plan |
