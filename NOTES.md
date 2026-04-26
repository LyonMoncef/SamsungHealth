# Notes

> Ce fichier contient uniquement des ADR (Architecture Decision Records).
> Les bugs et features vont dans GitHub Issues + BACKLOG.md.

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
