# Harnais de validation (notebooks)

Chaque calcul de Nightfall est prototypé et validé ici, en Python, avant d'être porté dans le module `core/` Kotlin.
Les calculs sont écrits dans les cellules ; `helpers.py` ne contient que la plomberie (lecture de l'export, heures locales).

| Notebook | Calcul | Validation |
|---|---|---|
| `01_dedup_chevauchements.ipynb` | Fusion des sessions de sommeil qui se chevauchent (analyse / affichage) | cas synthétiques + parité darkhour (1146) |

## Installation (une fois)

```bash
uv venv notebooks/.venv --python 3.12
VIRTUAL_ENV=notebooks/.venv uv pip install -r notebooks/requirements.txt
notebooks/.venv/bin/nbstripout --install --attributes .gitattributes
```

`nbstripout` retire les sorties des notebooks à chaque commit : **aucune donnée de santé ne doit entrer dans git** (contrainte C1).

## Utilisation

```bash
cd notebooks && .venv/bin/jupyter lab
```

Les données viennent de l'export de l'app (Profil → Exporter mes données), déposé hors du repo ; son chemin est la première cellule de chaque notebook.

Un notebook est validé quand **Restart & Run All** passe sans erreur : les `assert` sont les critères de validation.
