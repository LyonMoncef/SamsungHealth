"""Plomberie du harnais : lecture de l'export Nightfall (contrat v2).

Les calculs (dédup, NPCRA, périodogramme...) restent dans les cellules des notebooks.
Ici on ne fait que charger les fichiers, typer les colonnes et vérifier le manifeste.
"""

import json
import zipfile
from datetime import timedelta, timezone

import pandas as pd

VERSION_CONTRAT_ATTENDUE = "2"


def charger_export(chemin_zip):
    # Prend en entrée le chemin de l'archive ZIP exportée depuis l'app (Profil → Exporter mes données)
    # et retourne un dictionnaire de DataFrames : 'sessions', 'stades', 'pas', plus le 'manifeste' (dict).
    #
    # Toutes les dates sont converties en datetime UTC (tz-aware).
    # Les décalages horaires ('+02:00') sont gardés à part : c'est eux qui donnent l'heure locale.

    with zipfile.ZipFile(chemin_zip) as archive:
        manifeste = json.loads(archive.read("manifest.json"))
        sessions = pd.read_csv(archive.open("sleep_sessions.csv"))
        stades = pd.read_csv(archive.open("sleep_stages.csv"))
        pas = pd.read_csv(archive.open("steps.csv"))

    # ============= Etape 1 : le fichier est-il lisible par ce harnais ?  ===============
    # Si le contrat change côté app, on préfère planter ici plutôt que calculer sur des colonnes mal comprises.
    assert manifeste["contract_version"] == VERSION_CONTRAT_ATTENDUE, (
        f"Contrat {manifeste['contract_version']} reçu, le harnais attend le contrat {VERSION_CONTRAT_ATTENDUE}"
    )

    # ============= Etape 2 : typage des dates  ===============
    # ISO8601 : l'app écrit les millisecondes seulement quand elles ne sont pas nulles (…33Z vs …33.757Z)
    for colonne in ["start_utc", "end_utc", "last_modified_utc"]:
        sessions[colonne] = pd.to_datetime(sessions[colonne], utc=True, format="ISO8601")
    for colonne in ["start_utc", "end_utc"]:
        stades[colonne] = pd.to_datetime(stades[colonne], utc=True, format="ISO8601")
        pas[colonne] = pd.to_datetime(pas[colonne], utc=True, format="ISO8601")

    # ============= Etape 3 : les comptes doivent correspondre au manifeste  ===============
    # C'est le contrôle TA-14 : rien n'a été perdu entre l'app et le notebook.
    assert len(sessions) == manifeste["sleep_sessions_count"], "sessions : compte différent du manifeste"
    assert len(stades) == manifeste["sleep_stages_count"], "stades : compte différent du manifeste"
    assert len(pas) == manifeste["hourly_steps_count"], "pas : compte différent du manifeste"
    assert sessions["id"].is_unique, "deux sessions portent le même identifiant"

    sessions = sessions.sort_values("start_utc").reset_index(drop=True)
    return {"sessions": sessions, "stades": stades, "pas": pas, "manifeste": manifeste}


def vers_heure_locale(instant_utc, decalage):
    # Prend un instant UTC et un décalage au format '+02:00' (ou vide),
    # et retourne l'heure locale telle que la montre l'a vécue.
    # Décalage manquant → on reste en UTC, sans deviner.
    if pd.isna(decalage):
        return instant_utc
    signe = -1 if decalage.startswith("-") else 1
    heures, minutes = decalage.lstrip("+-").split(":")
    fuseau = timezone(signe * timedelta(hours=int(heures), minutes=int(minutes)))
    return instant_utc.tz_convert(fuseau)


JOURS_FR = ["lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."]


def date_fr(instant):
    # Libellé court en français, sans dépendre de la locale du système : 'ven. 26/09/2025 10:57'
    return f"{JOURS_FR[instant.weekday()]} {instant:%d/%m/%Y %H:%M}"
