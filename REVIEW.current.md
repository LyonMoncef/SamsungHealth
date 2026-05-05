# Review — feat/nightfall-dashboard — 2026-04-21
<!-- branch: feat/nightfall-dashboard | generated: 2026-04-21 -->

## Tests

### Chargement & rendu de base
- [x] `http://localhost:8001/` charge sans erreur 404 ni console error
- [x] La page affiche le header "Nightfall / Samsung Sleep" avec la lune et l'heure de sync
- [x] Les 9 sections s'affichent (heatmap, timeline, hypnogram, radial, small multiples, ridgeline, top nights, week agenda, metrics)
- [x] Les fonts Instrument Serif et Geist sont bien chargées (titres en serif, labels en mono)

### Données réelles
- [x] Le hero affiche la dernière nuit avec une date, un score, une durée et un % REM cohérents
- [x] L'arc SVG de la dernière nuit est tracé avec des segments colorés (deep/light/rem/awake)
- [x] Le heatmap 30 nuits × 24h est rempli avec des cellules colorées
- [x] La section Metrics affiche des valeurs non nulles (score régularité, dette, stage mix, HR)

### Navigation
- [x] Les boutons ‹ › du hypnogram changent de nuit et rechargent le rendu
- [x] Cliquer une cellule dans les Small Multiples met à jour le hypnogram et le radial clock
- [x] Les flèches clavier ← → naviguent entre les nuits
- [x] La touche T ouvre le panneau Tweaks (accent, grain)

### État vide
- [x] Avec une DB vide (ou `api.js` qui retourne `[]`), l'écran "Aucune donnée" s'affiche sans crash JS

### Résilience données manquantes
- [x] Si steps et HR ne sont pas en DB pour les mêmes jours que le sleep, le dashboard charge quand même (pas de crash, valeurs à 0)
