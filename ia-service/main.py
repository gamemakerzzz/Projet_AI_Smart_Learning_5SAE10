"""
Service IA pour SmartLearn - Analyse de l'apprenant, recommandations et planification
FastAPI + algorithme de recommandation TF-IDF
"""
from datetime import date, datetime, timedelta
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from typing import List, Dict, Optional
import math
from collections import Counter

app = FastAPI(title="SmartLearn IA Service", version="1.1.0")


class Competence(BaseModel):
    nom: str
    niveau: int


class Profil(BaseModel):
    niveau: Optional[str] = ""
    objectif: Optional[str] = ""
    domaineInteret: Optional[str] = ""
    competences: List[Competence] = []


class Formation(BaseModel):
    id: int
    titre: str
    description: Optional[str] = ""
    niveau: str
    motsCles: Optional[str] = ""
    categorie: Optional[str] = ""


class AnalyseRequest(BaseModel):
    profil: Profil


class RecommandationRequest(BaseModel):
    profil: Profil
    formations: List[Formation]


def tokenize(text: str) -> List[str]:
    """Tokenisation simple du texte."""
    return [w.lower().strip() for w in (text or "").split() if len(w) > 2]


def compute_tf_idf(documents: List[str]) -> Dict[str, Dict[str, float]]:
    """Calcule les scores TF-IDF pour chaque document."""
    n = len(documents)
    df: Dict[str, int] = {}
    for doc in documents:
        for term in set(tokenize(doc)):
            df[term] = df.get(term, 0) + 1

    tf_idf = {}
    for i, doc in enumerate(documents):
        tf = Counter(tokenize(doc))
        max_tf = max(tf.values()) if tf else 1
        scores = {}
        for term, count in tf.items():
            # CORRECTION : IDF lissé. L'ancienne formule log(n / (1 + df)) devenait
            # négative ou nulle pour les mots présents dans (presque) tous les documents.
            idf = math.log((1 + n) / (1 + df.get(term, 0))) + 1
            scores[term] = (count / max_tf) * idf
        tf_idf[f"doc_{i}"] = scores
    return tf_idf


def cosine_similarity(vec1: Dict[str, float], vec2: Dict[str, float]) -> float:
    """Calcule la similarité cosinus entre deux vecteurs."""
    keys = set(vec1.keys()) | set(vec2.keys())
    dot = sum(vec1.get(k, 0) * vec2.get(k, 0) for k in keys)
    norm1 = math.sqrt(sum(v ** 2 for v in vec1.values()))
    norm2 = math.sqrt(sum(v ** 2 for v in vec2.values()))
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return dot / (norm1 * norm2)


def determine_style(profil: Profil) -> str:
    """Détermine le style d'apprentissage basé sur le profil."""
    if not profil.competences:
        return "Débutant - Exploration des bases"
    avg = sum(c.niveau for c in profil.competences) / len(profil.competences)
    if avg < 50:
        return "Débutant - Renforcement des bases"
    elif avg < 75:
        return "Intermédiaire - Approfondissement"
    else:
        return "Avancé - Perfectionnement"


def analyze_profil(profil: Profil) -> Dict:
    """Analyse détaillée du profil de l'apprenant."""
    forces = []
    faiblesses = []
    comp_evol = {}

    for comp in profil.competences:
        comp_evol[comp.nom] = float(comp.niveau)
        if comp.niveau >= 75:
            forces.append(f"{comp.nom} ({comp.niveau}%)")
        elif comp.niveau < 50:
            faiblesses.append(f"{comp.nom} ({comp.niveau}%)")

    avg = sum(c.niveau for c in profil.competences) / len(profil.competences) if profil.competences else 0

    if avg < 50:
        niveau_global = "Débutant"
    elif avg < 75:
        niveau_global = "Intermédiaire"
    else:
        niveau_global = "Avancé"

    recommandations = []
    if faiblesses:
        recommandations.append(f"Consacrez des séances de révision aux compétences faibles: {', '.join(faiblesses)}")
    if profil.objectif:
        recommandations.append(f"Alignez vos formations sur votre objectif: {profil.objectif}")
    if len(profil.competences) < 3:
        recommandations.append("Développez au moins 3 compétences pour une base solide")
    if avg >= 75:
        recommandations.append("Maintenez votre niveau avec des défis avancés")

    return {
        "styleApprentissage": determine_style(profil),
        "forces": forces,
        "faiblesses": faiblesses,
        "recommandationsPersonnalisees": recommandations,
        "scoreProgression": int(avg),
        "competencesEvolution": comp_evol,
        "niveauGlobal": niveau_global
    }


@app.post("/analyze")
async def analyze(request: AnalyseRequest):
    """Analyse le profil de l'apprenant."""
    return analyze_profil(request.profil)


@app.post("/recommend")
async def recommend(request: RecommandationRequest):
    """Recommande des formations basées sur le profil."""
    # CORRECTION : objectif / domaineInteret / niveau sont Optional, donc peuvent être None.
    # " ".join([None, ...]) levait un TypeError (erreur 500).
    user_text = " ".join([
        request.profil.objectif or "",
        request.profil.domaineInteret or "",
        " ".join(c.nom for c in request.profil.competences),
        request.profil.niveau or ""
    ])

    formation_texts = [
        " ".join([f.titre, f.description or "", f.motsCles or "", f.categorie or ""])
        for f in request.formations
    ]

    all_texts = [user_text] + formation_texts
    tf_idf = compute_tf_idf(all_texts)

    user_vec = tf_idf.get("doc_0", {})
    results = []

    for i, formation in enumerate(request.formations):
        form_vec = tf_idf.get(f"doc_{i + 1}", {})
        score = cosine_similarity(user_vec, form_vec)

        if request.profil.niveau and formation.niveau:
            if request.profil.niveau.lower() == formation.niveau.lower():
                score += 0.1

        for comp in request.profil.competences:
            if comp.nom.lower() in formation.titre.lower() or comp.nom.lower() in (formation.motsCles or "").lower():
                score += 0.05 * (comp.niveau / 100)

        results.append({
            "formationId": formation.id,
            "score": min(score, 1.0),
            "methode": "TF_IDF"
        })

    results.sort(key=lambda x: x["score"], reverse=True)
    return results


# ---------------------------------------------------------------
# PLANIFICATION : route manquante (cause du 404 « POST /planification »)
# ---------------------------------------------------------------
JOURS_REVISION = 5  # nombre de jours de révision avant une évaluation
CRENEAUX = [("09:00", "11:00"), ("14:00", "16:00"), ("16:30", "18:30")]


def _pick(d, *keys, default=None):
    """Premier champ non nul parmi plusieurs noms possibles (camelCase / snake_case)."""
    if isinstance(d, dict):
        for k in keys:
            if d.get(k) is not None:
                return d[k]
    return default


def _to_date(v):
    """Accepte '2026-10-30', '2026-10-30T10:00:00', '30/10/2026' ou [2026, 10, 30]."""
    if v is None:
        return None
    if isinstance(v, (list, tuple)):
        try:
            return date(int(v[0]), int(v[1]), int(v[2]))
        except (ValueError, IndexError, TypeError):
            return None
    s = str(v).strip()
    try:
        return date.fromisoformat(s[:10])
    except ValueError:
        pass
    try:
        return datetime.strptime(s[:10], "%d/%m/%Y").date()
    except ValueError:
        return None


@app.post("/planification")
async def planification(request: Request):
    """Génère les séances de révision avant chaque évaluation à venir."""
    body = await request.json()
    print("PLANIFICATION BODY:", body)  # pour voir ce que Spring envoie réellement

    debut = _to_date(_pick(body, "dateDebut", "date_debut", "startDate", "debut"))
    fin = _to_date(_pick(body, "dateFin", "date_fin", "endDate", "fin"))
    if not debut or not fin:
        raise HTTPException(status_code=422, detail="dateDebut et dateFin sont obligatoires")
    if fin < debut:
        raise HTTPException(status_code=422, detail="dateFin doit être >= dateDebut")

    evaluations = _pick(body, "evaluations", "examens", "exams", "devoirs", default=[]) or []
    faiblesses = _pick(body, "faiblesses", "weaknesses", default=[]) or []
    competences = _pick(body, "competences", default=[]) or []
    formations_body = _pick(body, "formations", default=[]) or []

    # Spring envoie TOUS les examens de l'apprenant + la liste des formations retenues
    # (filtrée si l'utilisateur en a sélectionné). On filtre donc par cette liste.
    formation_ids = set(_pick(body, "formationIds", "formation_ids", default=[]) or [])
    formation_ids |= {f.get("id") for f in formations_body
                      if isinstance(f, dict) and f.get("id") is not None}

    # Compétences faibles (< 50 %) : un point faible concerne une formation dont le titre
    # contient le nom de la compétence (ex. compétence "Java" -> "Java pour débutants")
    mots_faibles = [
        str(c.get("nom")).lower() for c in competences
        if isinstance(c, dict) and c.get("nom")
        and (c.get("niveau") if c.get("niveau") is not None else 100) < 50
    ]

    faibles = set()
    for w in faiblesses:
        fid = _pick(w, "formationId", "formation_id")
        titre = _pick(w, "formationTitre", "formation_titre", "titre")
        if fid is not None:
            faibles.add(fid)
        if titre:
            faibles.add(str(titre).lower())

    candidates = {}
    for e in evaluations:
        d_eval = _to_date(_pick(e, "date", "dateEvaluation", "date_evaluation",
                                "dateExamen", "dateLimite"))
        if not d_eval or d_eval <= debut:
            continue  # évaluation passée ou le jour même : rien à réviser

        fid = _pick(e, "formationId", "formation_id")
        ftitre = _pick(e, "formationTitre", "formation_titre", "formation") or "Formation"
        if isinstance(ftitre, dict):  # formation imbriquée {id, titre}
            fid = fid if fid is not None else ftitre.get("id")
            ftitre = ftitre.get("titre", "Formation")
        if formation_ids and fid not in formation_ids:
            continue

        coef = float(_pick(e, "coefficient", "coef", default=1) or 1)
        type_eval = str(_pick(e, "type", default="Examen"))
        est_faible = (fid in faibles or str(ftitre).lower() in faibles
                      or any(m in str(ftitre).lower() for m in mots_faibles))

        # La révision s'arrête la veille de l'évaluation, même si elle est après dateFin
        premier = max(debut, d_eval - timedelta(days=JOURS_REVISION))
        dernier = min(fin, d_eval - timedelta(days=1))
        jour = premier
        while jour <= dernier:
            proximite = max(0, 3 - (d_eval - jour).days)
            priorite = int(round(coef * 2)) + (5 if est_faible else 0) + proximite
            motif = f"{type_eval} le {d_eval:%d/%m/%Y} (coef. {coef:g})"
            if est_faible:
                motif += " + point faible détecté"
            cle = (jour, fid if fid is not None else ftitre)
            if cle not in candidates or candidates[cle]["priorite"] < priorite:
                candidates[cle] = {
                    "date": jour.isoformat(),
                    "titre": f"Révision - {ftitre}",
                    "description": f"Séance de révision planifiée pour la formation : {ftitre}",
                    "formationId": fid,
                    "formationTitre": ftitre,
                    "motif": motif,
                    "priorite": priorite,
                }
            jour += timedelta(days=1)

    seances, par_jour = [], {}
    for s in sorted(candidates.values(), key=lambda x: (x["date"], -x["priorite"])):
        i = par_jour.get(s["date"], 0)
        par_jour[s["date"]] = i + 1
        h1, h2 = CRENEAUX[i % len(CRENEAUX)]
        seances.append({**s, "heureDebut": h1, "heureFin": h2})

    return {
        "apprenantId": _pick(body, "apprenantId", "apprenant_id"),
        "periode": f"{debut:%d/%m/%Y} - {fin:%d/%m/%Y}",
        "totalSeances": len(seances),
        "seances": seances,
    }


# ---------------------------------------------------------------
# PROBABILITÉ DE RÉALISATION D'UNE SÉANCE (route appelée par SeanceProbabiliteService.java)
# ---------------------------------------------------------------
class CreneauIn(BaseModel):
    heure: Optional[int] = 12
    jourSemaine: Optional[int] = 0          # lundi = 0 ... dimanche = 6
    dureeMin: Optional[int] = 60
    joursAvantExamen: Optional[int] = None
    seancesMemeJour: Optional[int] = 0


def _probabilite(c: CreneauIn) -> float:
    """Score logistique à poids fixés à la main (heuristique, pas un modèle entraîné)."""
    h = 12 if c.heure is None else c.heure
    d = 60 if c.dureeMin is None else c.dureeMin
    jour = 0 if c.jourSemaine is None else c.jourSemaine
    m = c.seancesMemeJour or 0

    z = 1.1                                  # base : environ 75 %
    if h >= 21 or h <= 6:
        z -= 1.4                             # très tôt ou très tard
    elif h >= 19 or h <= 7:
        z -= 0.6
    elif 9 <= h <= 11 or 14 <= h <= 16:
        z += 0.3                             # créneaux les plus productifs
    if d > 120:
        z -= 1.0
    elif d > 90:
        z -= 0.6
    elif d < 30:
        z -= 0.3
    if jour >= 5:
        z -= 0.4                             # week-end
    z -= 0.5 * m                             # séances déjà prévues le même jour
    if c.joursAvantExamen is not None:
        if c.joursAvantExamen <= 2:
            z += 0.5                         # échéance proche : plus de motivation
        elif c.joursAvantExamen > 14:
            z -= 0.3

    p = 1 / (1 + math.exp(-z))
    return round(min(0.95, max(0.05, p)), 3)


@app.post("/seance/probabilite")
async def seance_probabilite(creneaux: List[CreneauIn]):
    """Reçoit un tableau JSON de créneaux, renvoie un tableau de même taille et même ordre."""
    return [
        {
            "heure": c.heure if c.heure is not None else 12,
            "jourSemaine": c.jourSemaine if c.jourSemaine is not None else 0,
            "dureeMin": c.dureeMin if c.dureeMin is not None else 60,
            "probabilite": _probabilite(c),
            "source": "SCORE",
        }
        for c in creneaux
    ]


@app.get("/health")
async def health():
    return {"status": "ok", "service": "SmartLearn IA"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)