# Multi-agent system — SamsungHealth V2

Système d'agents à responsabilité unique pour exécuter le master plan V2 (refonte security/RGPD/Compose) sans saturer le contexte de la session principale.

## Source of truth

- **Master plan** : `vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-v2-refactor-master.md`
- **Plan multi-agents** : `vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-v2-multi-agents-architecture.md`
- **Branche long-lived** : `chore/v2-refactor`
- **Sous-branche méta-projet** : `chore/phase-a-foundation-agents`

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
| **A** — Foundation agents | en cours | contrats Pydantic + 5 subagents + hooks + settings + skills + budget enforcer |
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
- Tout cela enforced par `.claude/hooks/enforce-path-scope.sh`

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
