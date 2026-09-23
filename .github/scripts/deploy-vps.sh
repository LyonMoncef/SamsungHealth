#!/usr/bin/env bash
# VPS deploy script — runs on the VPS, not on the runner.
# Argument: $1 = IMAGE_TAG (ex: dev-abc123...).
#
# Pourquoi un script séparé plutôt qu'un heredoc dans le workflow :
# `ssh ... bash << EOF ... EOF` envoie le heredoc sur stdin du bash distant.
# `docker compose pull` peut consommer ce stdin (ou la connexion SSH se ferme
# pendant le pull), bash voit EOF et sort proprement (exit 0), laissant
# l'ancien container tourner. Symptôme : déploiement "success" mais image
# inchangée. Un script-fichier exécuté via `bash /tmp/deploy.sh` n'a pas ce
# problème — bash lit le fichier, pas stdin.
set -euo pipefail

IMAGE_TAG="${1:-}"
if [ -z "$IMAGE_TAG" ]; then
  echo "::error::IMAGE_TAG arg required"
  exit 1
fi

cd /srv/samsunghealth-dev

if ! grep -qE "^IMAGE_TAG=" .env.prod; then
  echo "::error::IMAGE_TAG not found in .env.prod"
  exit 1
fi
sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$IMAGE_TAG/" .env.prod
echo "IMAGE_TAG updated: $(grep '^IMAGE_TAG=' .env.prod)"

docker compose -f docker-compose.prod.yml --env-file .env.prod pull web

CURRENT=$(docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm web alembic current 2>/dev/null | awk 'NR==1{print $1}')
EXPECTED=$(docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm web alembic heads 2>/dev/null | awk 'NR==1{print $1}')
if [ -z "$EXPECTED" ]; then
  echo "::error::Failed to read alembic heads"
  exit 1
fi
CURRENT=${CURRENT:-(none)}
echo "alembic current=$CURRENT expected=$EXPECTED"
if [ "$CURRENT" != "$EXPECTED" ] && [ "$CURRENT" != "(none)" ]; then
  echo "::error::Alembic drift — current=$CURRENT expected=$EXPECTED"
  exit 1
fi

docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm web alembic upgrade head
docker compose -f docker-compose.prod.yml --env-file .env.prod rm --stop --force web 2>/dev/null || true
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d web

echo "=== Running container ==="
docker ps --filter "publish=8001" --format "{{.Names}} {{.Image}}"
