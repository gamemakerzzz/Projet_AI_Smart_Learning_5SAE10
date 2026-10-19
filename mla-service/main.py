from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import os, json
import joblib
import numpy as np

app = FastAPI(
    title="SmartLearn MLA Recommendation Service",
    description="Microservice de recommandation intelligente basé sur TF-IDF & Cosine Similarity",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

SEANCE_MODEL = None
try:
    SEANCE_MODEL = joblib.load(os.path.join(os.path.dirname(__file__), "seance_model.joblib"))
except Exception as e:
    print("Modèle séance non chargé, règles utilisées :", e)

class Creneau(BaseModel):
    heure: int
    jourSemaine: int          # 0 = lundi
    dureeMin: int
    joursAvantExamen: int
    seancesMemeJour: int = 0

def regle_simple(c: Creneau) -> float:
    """Fallback : règles de bon sens."""
    p = 0.7
    if c.heure >= 21 or c.heure <= 7: p -= 0.3
    if c.dureeMin > 90: p -= 0.2
    p -= 0.1 * c.seancesMemeJour
    return max(0.05, min(0.95, p))

class ProfilData(BaseModel):
    niveau: Optional[str] = ""
    objectif: Optional[str] = ""
    domaineInteret: Optional[str] = ""
    competences: Optional[List[str]] = []

class CompetenceData(BaseModel):
    nom: str
    niveau: int

class ProfilData(BaseModel):
    niveau: Optional[str] = ""
    objectif: Optional[str] = ""
    domaineInteret: Optional[str] = ""
    competences: Optional[List[CompetenceData]] = []

class AnalyseRequest(BaseModel):
    profil: Optional[ProfilData] = None

class AnalyseResponse(BaseModel):
    styleApprentissage: str
    forces: List[str]
    faiblesses: List[str]
    recommandationsPersonnalisees: List[str]
    scoreProgression: int
    competencesEvolution: dict
    niveauGlobal: str

class FormationData(BaseModel):
    id: int
    titre: str
    description: Optional[str] = ""
    niveau: Optional[str] = ""
    motsCles: Optional[str] = ""
    categorie: Optional[str] = ""

class RecommendationRequest(BaseModel):
    profil: ProfilData
    formations: List[FormationData]

class RecommendationItem(BaseModel):
    formationId: int
    titre: Optional[str] = ""
    score: float

@app.get("/")
def health_check():
    return {"status": "ok", "service": "SmartLearn MLA Recommendation Engine"}

@app.post("/recommend", response_model=List[RecommendationItem])
def recommend_formations(payload: RecommendationRequest):
    profil = payload.profil
    formations = payload.formations

    if not formations:
        return []

    # 1. Build profile text representation
    comps_str = " ".join(profil.competences) if profil.competences else ""
    user_text = f"{profil.objectif or ''} {profil.domaineInteret or ''} {comps_str} {profil.niveau or ''}".strip().lower()

    # 2. Build formations text representation list
    corpus = [user_text]
    for f in formations:
        f_text = f"{f.titre} {f.description or ''} {f.motsCles or ''} {f.categorie or ''} {f.niveau or ''}".strip().lower()
        corpus.append(f_text)

    # 3. Vectorize using TF-IDF
    vectorizer = TfidfVectorizer(ngram_range=(1, 2))
    if not any(vectorizer.build_analyzer()(text) for text in corpus):
        return [RecommendationItem(formationId=f.id, titre=f.titre, score=0.0) for f in formations]
    tfidf_matrix = vectorizer.fit_transform(corpus)

    # 4. Calculate Cosine Similarity between user profile (index 0) and each formation (index 1..N)
    user_vector = tfidf_matrix[0:1]
    formations_vectors = tfidf_matrix[1:]

    similarities = cosine_similarity(user_vector, formations_vectors)[0]

    # 5. Build results list and sort descending by score
    results = []
    for idx, f in enumerate(formations):
        score = float(similarities[idx])
        
        results.append(RecommendationItem(
            formationId=f.id,
            titre=f.titre,
            score=round(score, 2)
        ))

    results.sort(key=lambda x: x.score, reverse=True)
    return results

@app.post("/analyze", response_model=AnalyseResponse)
def analyze_profil(payload: AnalyseRequest):
    profil = payload.profil or ProfilData()
    competences = profil.competences or []
    niveau = (profil.niveau or "").strip()
    objectif = (profil.objectif or "").strip()
    domaine = (profil.domaineInteret or "").strip()

    # Moyenne des niveaux de compétences
    if competences:
        avg_level = sum(c.niveau for c in competences) / len(competences)
    else:
        avg_level = 0.0

    # Niveau global
    if avg_level < 50:
        niveau_global = "Débutant"
    elif avg_level < 75:
        niveau_global = "Intermédiaire"
    else:
        niveau_global = "Avancé"

    # Style d'apprentissage
    if not competences:
        style = "Débutant - Explorer les bases"
    elif avg_level < 50:
        style = "Débutant - Renforcement des bases"
    elif avg_level < 75:
        style = "Intermédiaire - Approfondissement"
    else:
        style = "Avancé - Perfectionnement"

    # Forces et faiblesses
    forces = [f"{c.nom} ({c.niveau}%)" for c in competences if c.niveau >= 75]
    faiblesses = [f"{c.nom} ({c.niveau}%)" for c in competences if c.niveau < 50]

    # Évolution des compétences
    competences_evolution = {c.nom: float(c.niveau) for c in competences}

    # Recommandations personnalisées
    recommandations = []
    if faiblesses:
        recommandations.append("Consacrez des séances de révision aux compétences faibles : " + ", ".join(faiblesses))
    if objectif:
        recommandations.append("Alignez vos formations sur votre objectif : " + objectif)
    if len(competences) < 3:
        recommandations.append("Développez au moins 3 compétences pour une base solide")
    if not recommandations:
        recommandations.append("Continuez sur cette dynamique, votre profil est équilibré.")

    return AnalyseResponse(
        styleApprentissage=style,
        forces=forces,
        faiblesses=faiblesses,
        recommandationsPersonnalisees=recommandations,
        scoreProgression=int(round(avg_level)),
        competencesEvolution=competences_evolution,
        niveauGlobal=niveau_global
    )

class ExamenPlanifData(BaseModel):
    id: int
    titre: str
    dateExamen: str
    coefficient: int = 1
    formationId: Optional[int] = None
    formationTitre: Optional[str] = ""

class CompetencePlanifData(BaseModel):
    nom: str
    niveau: int

class FormationPlanifData(BaseModel):
    id: int
    titre: str

class PlanificationIARequest(BaseModel):
    apprenantId: int
    dateDebut: str
    dateFin: str
    examens: List[ExamenPlanifData] = []
    competences: List[CompetencePlanifData] = []
    formations: List[FormationPlanifData] = []

class SeancePlanifData(BaseModel):
    date: str
    heureDebut: str
    heureFin: str
    titre: str
    description: str
    formationId: Optional[int] = None
    formationTitre: Optional[str] = ""
    priorite: int
    motif: str

class PlanificationIAResponse(BaseModel):
    totalSeances: int
    periode: str
    seances: List[SeancePlanifData]

@app.post("/planification", response_model=PlanificationIAResponse)
def planification_ia(payload: PlanificationIARequest):
    from datetime import datetime, timedelta, date

    date_debut = datetime.strptime(payload.dateDebut, "%Y-%m-%d").date()
    date_fin = datetime.strptime(payload.dateFin, "%Y-%m-%d").date()
    jours = (date_fin - date_debut).days + 1

    # Index des compétences par nom
    comp_levels = {c.nom.lower(): c.niveau for c in payload.competences}

    # Index des formations par id
    formations_map = {f.id: f.titre for f in payload.formations}

    # Calcul du score de priorité par examen
    examen_scores = []
    today = date.today()
    for ex in payload.examens:
        date_exam = datetime.strptime(ex.dateExamen, "%Y-%m-%d").date() if ex.dateExamen else None
        jours_restants = (date_exam - today).days if date_exam else 999
        urgence = max(0, 30 - jours_restants)  # plus l'examen est proche, plus l'urgence est haute
        coeff = ex.coefficient or 1

        # Faiblesses liées à la formation de l'examen
        faible_count = 0
        faible_noms = []
        for nom, niveau in comp_levels.items():
            if niveau < 50:
                # On considère que la compétence est liée si elle apparaît dans le titre de la formation
                if nom in (ex.formationTitre or "").lower() or nom in (ex.titre or "").lower():
                    faible_count += 1
                    faible_noms.append(nom)

        score = urgence * 2 + coeff * 3 + faible_count * 5
        examen_scores.append({
            "examen": ex,
            "score": score,
            "jours_restants": max(0, jours_restants),
            "faibles": faible_noms
        })

    # Trier par score décroissant
    examen_scores.sort(key=lambda x: x["score"], reverse=True)

    # Générer les séances sur la période
    seances = []
    seance_index = 0
    heur_debut = 9
    heur_fin = 17  # 8 heures de disponible par jour

    # Répartition pondérée : plus le score est élevé, plus on alloue de séances
    total_score = sum(es["score"] for es in examen_scores) or 1
    total_seances_cibles = max(jours * 2, len(examen_scores))  # au moins 2 séances par jour

    allocation = {}
    for es in examen_scores:
        ratio = es["score"] / total_score
        nb = max(1, int(total_seances_cibles * ratio))
        allocation[id(es["examen"])] = nb

    # Générer les séances jour par jour
    current = date_debut
    while current <= date_fin and seance_index < total_seances_cibles:
        for es in examen_scores:
            if seance_index >= total_seances_cibles:
                break
            ex = es["examen"]
            nb_alloues = allocation.get(id(ex), 1)
            # On répartit les séances de cet examen sur les jours restants
            if seance_index % nb_alloues == 0 or len(seances) == 0:
                pass
            heure_debut = heur_debut + (seance_index % 4) * 2  # 9h, 11h, 13h, 15h
            heure_fin = heure_debut + 2
            if heure_fin > heur_fin:
                continue

            motif_parts = []
            if es["jours_restants"] <= 7:
                motif_parts.append(f"Examen dans {es['jours_restants']} jour(s)")
            if es["faibles"]:
                motif_parts.append("Renforcer : " + ", ".join(es["faibles"]))
            if not motif_parts:
                motif_parts.append("Révision régulière")

            seances.append(SeancePlanifData(
                date=current.isoformat(),
                heureDebut=f"{heure_debut:02d}:00",
                heureFin=f"{heure_fin:02d}:00",
                titre=f"Révision - {ex.titre}",
                description=f"Préparation de l'examen '{ex.titre}' (coefficient {ex.coefficient})",
                formationId=ex.formationId,
                formationTitre=ex.formationTitre or formations_map.get(ex.formationId, ""),
                priorite=es["score"],
                motif=" | ".join(motif_parts)
            ))
            seance_index += 1
        current += timedelta(days=1)

    return PlanificationIAResponse(
        totalSeances=len(seances),
        periode=f"{payload.dateDebut} au {payload.dateFin}",
        seances=seances
    )

@app.post("/seance/probabilite")
def seance_probabilite(creneaux: list[Creneau]):
    out = []
    for c in creneaux:
        if SEANCE_MODEL is not None:
            x = np.array([[c.heure, c.jourSemaine, c.dureeMin,
                           c.joursAvantExamen, c.seancesMemeJour]])
            p, source = float(SEANCE_MODEL.predict_proba(x)[0][1]), "ML"
        else:
            p, source = regle_simple(c), "REGLES"
        out.append({"heure": c.heure, "jourSemaine": c.jourSemaine,
                    "dureeMin": c.dureeMin, "probabilite": round(p, 3), "source": source})
    return out

@app.get("/seance/model/info")
def seance_model_info():
    try:
        return json.load(open(os.path.join(os.path.dirname(__file__), "metrics_seance.json")))
    except Exception:
        return {"status": "aucun modèle entraîné"}
