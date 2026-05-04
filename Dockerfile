# syntax=docker/dockerfile:1.7
# Phase 6 — multi-stage backend image (~150MB cible).
# Spec: docs/vault/specs/2026-04-30-phase6-cicd-mvp.md §1
#
# Stage 1: builder — installe les deps Python via uv avec --require-hashes
# Stage 2: runner  — runtime slim non-root, healthcheck via urllib (R-L5: pas de curl)

# ---------- Stage 1: builder ----------
FROM python:3.12-slim AS builder

WORKDIR /app

# uv = installer Rust 10–100× plus rapide que pip, mêmes wheels PEP 503
RUN pip install --no-cache-dir uv

# Copier UNIQUEMENT les fichiers nécessaires à l'install (cache layer)
COPY requirements.txt requirements.lock ./

# --require-hashes : pip refuse d'installer si le hash ne correspond pas (HIGH 5 supply chain)
# --system : pas de venv (image éphémère, on copie site-packages en stage 2)
# --no-cache : pas de cache uv (réduit la taille du layer)
RUN uv pip install --system --no-cache --require-hashes -r requirements.lock

# ---------- Stage 2: runner ----------
FROM python:3.12-slim AS runner

WORKDIR /app

# Utilisateur non-root dédié (UID/GID stables pour bind-mounts éventuels)
RUN useradd -m -u 1000 app

# Copier les site-packages + binaires depuis le builder (pas de pip/uv en runtime)
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copier le code applicatif minimal (cf .dockerignore pour les exclusions)
COPY server/ ./server/
COPY alembic/ ./alembic/
COPY alembic.ini ./
COPY static/ ./static/

# Permissions : tout owned par app (uvicorn, alembic, écritures éventuelles)
RUN chown -R app:app /app

USER app

EXPOSE 8001

# HEALTHCHECK via urllib stdlib — curl absent de python:3.12-slim (R-L5)
# Conforme au mode utilisé par le compose (docker-compose.prod.yml §2).
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD python -c "import urllib.request, sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8001/healthz', timeout=3).status == 200 else 1)"

# Port 8001 ADR-1 (cf server/main.py + Makefile dev)
CMD ["uvicorn", "server.main:app", "--host", "0.0.0.0", "--port", "8001"]
