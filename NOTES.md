# Notes

> Ce fichier contient uniquement des ADR (Architecture Decision Records).
> Les bugs et features vont dans GitHub Issues + BACKLOG.md.

---

## ADR-3 : VPS perso vs PaaS + checklist hardening

**Contexte :** Phase 6 déploie sur infrastruture, avec décision architecture : VPS loué vs PaaS cloud (Render, Fly.io, Railway).

**Décision :** MVP Phase 6 sur **VPS perso** (colocalisé avec DataSaillance), pas PaaS.

**Justification** :
- **Coût** : VPS déjà payé (multi-project sharing) = quasi-nul pour SamsungHealth
- **Maîtrise** : contrôle complet SSH/firewall/Postgres, pas de vendor lock-in (Render proprietary runtime, Fly.io edge compute)
- **Acceptable MVP** : usage perso 1 utilisateur, pas de SLA critique (si VPS crash, manuelle recovery tolérée)
- **Trade-off assumé** : ops à charge du dev (sécu, backups, fail2ban, rotation clés SSH/DB, OS updates)

**Non-choix PaaS** : Render/Fly.io séduisants (auto-scaling, managed DB, TLS auto, health-check integré) mais :
- Vendor lock-in (data export + migration coûteux si pivotage futur)
- Secrets management opaque (Render vault, Fly wireguard — pas inspection directe)
- Pas d'apprentissage VPS/DevOps (learning goal Phase 6 = CI/CD complet, déploiement multi-env)
- Coût caché si data transfer/extra dynos + PG tiers premium dépasse le VPS brut

**Conséquences** :
- Déploiement `deploy-dev.yml` + `deploy-prod.yml` scripté (cf. spec Phase 6 §4/5)
- Responsabilité : pre-deploy `.github/known_hosts`, env-scoped secrets SSH, `.env.prod` VPS manuel chmod 600
- Maintenance opérationnelle : backups PG, fail2ban monitoring, root SSH key rotation

---

## Checklist hardening VPS (à compléter avant 1er déploiement prod réel)

Basé sur spec Phase 6 décisions R-H1, R-M3, D-3.

**Avant 1er déploiement dev** :
- [ ] VPS créé avec OS supporté (Ubuntu 22.04+ ou Debian 12+)
- [ ] SSH root login fonctionnel (temporary, avant CI setup)
- [ ] Clé publique SSH CI dev générée (Ed25519, `ssh-keygen -t ed25519 -f vps_dev_id` + commit pubkey dans repo)
- [ ] Clé publique SSH CI prod générée (séparé, `ssh-keygen -t ed25519 -f vps_prod_id`)
- [ ] `.github/known_hosts` pré-rempli via `ssh-keyscan`, merged main avant Phase 6 PR

**Configuration OS hardening** :
- [ ] `PasswordAuthentication no` dans `/etc/ssh/sshd_config` (clés only)
- [ ] `PermitRootLogin no` (user `deploy` ou `app`, sudo pour infra commands)
- [ ] `fail2ban` installé et actif (`systemctl enable fail2ban && systemctl start fail2ban`)
- [ ] UFW firewall activé :
  ```bash
  ufw enable
  ufw default deny incoming
  ufw default allow outgoing
  ufw allow 22/tcp    # SSH (restrict to CI IPs optionnel)
  ufw allow 80/tcp    # Caddy HTTP
  ufw allow 443/tcp   # Caddy HTTPS
  # Vérifier aucune autre port (postgres 5432 notamment)
  ufw status
  ```
- [ ] Docker + docker-compose installés (`apt install docker.io docker-compose`)
- [ ] User deployment (ex: `deploy`) créé, sudoers pour docker commands

**Application deployment** :
- [ ] Répertoire `/srv/samsunghealth-dev/` + `/srv/samsunghealth-prod/` créés, clone git
- [ ] `.env.prod` créé (template `.env.prod.example`), rempli avec secrets, `chmod 600`
  ```bash
  chmod 600 /srv/samsunghealth-{dev,prod}/.env.prod
  chown app:app /srv/samsunghealth-{dev,prod}/.env.prod
  ls -la /srv/samsunghealth-{dev,prod}/.env.prod
  # Doit montrer : -rw------- 1 app app
  ```
- [ ] `docker-compose.prod.yml` testé localement (compose up, /healthz répond)
- [ ] Postgres volume `/srv/samsunghealth-{dev,prod}/pgdata/` créé, mountpoint permissions correctes
- [ ] Postgres **jamais exposé en `ports:`** (réseau docker interne only) — vérifier `docker-compose.prod.yml`
- [ ] Healthcheck dans compose (`HEALTHCHECK --interval=30s`) + `docker logs` inspect

**Reverse-proxy TLS (Caddy/nginx)** :
- [ ] Caddy ou nginx installé
- [ ] Snippet D-3 configuré : `/readyz` bloqué external IPs (reverse-proxy filter)
  ```caddy
  @readyz_external {
    path /readyz
    not remote_ip 127.0.0.1 ::1 10.0.0.0/8 192.168.0.0/16
  }
  respond @readyz_external 404
  ```
- [ ] TLS auto via Let's Encrypt (Caddy `handle` directive avec `file` cert storage)
- [ ] Certs stockés hors repo (`/etc/caddy/certs/` ou équivalent, permission 0600)
- [ ] Test : `curl https://samsunghealth.example/healthz` → 200 (certificate valide)

**Backups & recovery** :
- [ ] Backups Postgres cronnés (optionnel MVP, **MANDATORY si données réelles**)
  ```bash
  # Exemple cron : daily 02:00 UTC
  0 2 * * * cd /srv/samsunghealth-prod && pg_dump -U $USER $DB | gzip > backups/daily-$(date +\%Y\%m\%d).sql.gz
  ```
- [ ] Offsite copy basique (optionnel MVP : rsync vers second VPS ou S3)
- [ ] Recovery test (restore depuis backup, vérifier app démarre)

**Rotation clés & rotation secrets** :
- [ ] **SSH key rotation policy** (différé Phase 6+ — procédure à formaliser issue GitHub)
  - Tous les 90 jours : rotate `VPS_SSH_KEY_DEV` + `VPS_SSH_KEY_PROD` (générer nouvelles clés, update `.authorized_keys` VPS + GitHub Secrets)
  - Document : "SSH key rotation SOP" dans `.github/` ou wiki repo
- [ ] **`SAMSUNGHEALTH_ENCRYPTION_KEY` rotation** (différé Phase 6+ — SOPS/age future)
  - MVP : si compromis détecté, opération manuelle (re-encrypt colonnes sensibles Art.9 avec nouvelle clé)
  - Procédure pas encore formalisée — ouvrir issue `chore/encryption-key-rotation-sops` après Phase 6

**Monitoring & alerting** :
- [ ] Logs structlog du container (uvicorn + alembic) → syslog ou journald VPS
  ```bash
  # Via docker-compose : volume vers /var/log/samsunghealth/
  # Puis logrotate + journalctl capture
  docker logs <container> | grep '"level":"error"' # manual check
  ```
- [ ] Alerting basique : `/healthz` 5xx → alert (optionnel : CloudFlare Workers, cron local curl)
- [ ] Status page : endpoint `/status` (optionnel — pour MVP monitoring basique suffisant)

**Documentation post-deployment** :
- [ ] VPS IP, hostnames, network config documentés (wiki repo private, pas commit)
- [ ] Secrets access policy : qui a accès root/ssh ? (GitHub repo members only)
- [ ] Incident runbook : "If deployment fails", "If DB down", "If OOM" (NOTES.md ou `.github/INCIDENT.md`)
- [ ] Disaster recovery plan (RTO/RPO définies pour Phase 6)

---

## Notes additionnelles ADR-3

**Phase 6+ évolutions** :
- Upgrade Fly.io/Render si SLA devient critique (multi-user, uptime 99.9%)
- Terraform/Pulumi IaC pour VPS provisioning (actuellement manual scripts)
- Secrets management via Vault/SOPS (rotation clés au-delà key rotation manual)

**Lien spec Phase 6** : Voir `docs/vault/specs/2026-04-30-phase6-cicd-mvp.md` pour détail complet décisions (HIGH/D/R).

---

## ADR-2 : Workflow pentester-review pour les phases sensibles

**Contexte :** Le subagent `pentester` review les specs avant TDD et émet un verdict `PASS|WARN|FAIL` avec HIGH bloquants + décisions design + risques additionnels. Ces verdicts doivent vivre quelque part de traçable et indexable, pas seulement dans la spec ou la mémoire de la session.

**Décision :** À chaque pentester-review d'une spec :
1. Ouvrir une **issue GitHub** avec label `pentester-review` (template `.github/ISSUE_TEMPLATE/pentester-review.yml` pré-rempli)
2. Body = mini-rapport audit synthétisé (HIGH cochables, design choices, risks, tests, différé hors-scope)
3. Le **PR de la phase ferme l'issue** via `Closes #N` dans la description

**Conséquences :**
- Trace native GitHub : indexable par label, fermée par PR, cherchable via `is:closed label:pentester-review`
- Onboarding futur : `gh issue list --label pentester-review --state all` montre tous les audits passés
- Le pattern à reproduire pour Phase 4 / Phase 6 / etc.
- Précédent : Issue #22 — Phase 3 RGPD (commit 2923097)

---

## ADR-1 : Port 8001 pour le serveur FastAPI

**Contexte :** DataSaillance tourne sur le port 8000 en dev local. Les deux projets coexistent sur la même machine.

**Décision :** SamsungHealth utilise le port **8001** (`make dev`, Android `DEFAULT_URL`). Aucun port hardcodé dans le code serveur — uniquement dans le Makefile et le `DEFAULT_URL` Android (configurable via l'UI Settings).

**Conséquences :** Pour tester depuis un téléphone physique, l'URL à configurer est `http://<IP_LAN>:8001`.

---

## Tech debt

- **PII scrubber automatique pour logs (V2.3+)** — V2.0.5 livre la fondation structlog mais
  ne filtre pas automatiquement les champs sensibles (emails, tokens, valeurs santé brutes).
  Convention humaine pour l'instant : ne pas mettre de PII dans `event`/extras. À reconsidérer
  quand V2.3 ajoute l'auth (events `login_*`, `password_reset`) et qu'on a un risque réel de
  fuite via stack traces ou kwargs accidentels.

---

## V2.3.1 checklist (différé post-V2.3)

V2.3 ship l'auth foundation atomique (users + JWT + multi-user FK + redaction + audit).
Reste hors-scope V2.3, prévu pour V2.3.1+ :

- **V2.3.0.1 (clean-up immédiat post-merge V2.3, ~30min)** : migration alembic 0005
  passant `user_id NOT NULL` sur les 22 tables santé, après backfill validé.
- **V2.3.1** : reset password flow + email verification (table `verification_tokens`,
  email = log structlog uniquement en attendant SMTP).
- **V2.3.2** : Google OAuth (provider abstraction `AuthProvider`).
- **V2.3.3** : rate limiting login (slowapi + redis OR in-memory bucket) + lockout
  enforcement automatique sur `failed_login_count > N` + frontend Nightfall login form.
- **CAPTCHA register** (différé V2.3.3+).
- **2FA / TOTP** (non scopé pour l'instant).
