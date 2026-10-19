from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

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

class ProfilData(BaseModel):
    niveau: Optional[str] = ""
    objectif: Optional[str] = ""
    domaineInteret: Optional[str] = ""
    competences: Optional[List[str]] = []

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


import os, json
import joblib
import numpy as np

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
