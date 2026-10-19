"""
Service IA pour SmartLearn - Analyse de l'apprenant et recommandations
FastAPI + algorithme de recommandation TF-IDF
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Optional
import math
from collections import Counter

app = FastAPI(title="SmartLearn IA Service", version="1.0.0")


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
    return [w.lower().strip() for w in text.split() if len(w) > 2]


def compute_tf_idf(documents: List[str]) -> Dict[str, List[float]]:
    """Calcule les scores TF-IDF pour chaque document."""
    n = len(documents)
    df = {}
    for doc in documents:
        terms = set(tokenize(doc))
        for term in terms:
            df[term] = df.get(term, 0) + 1

    tf_idf = {}
    for i, doc in enumerate(documents):
        terms = tokenize(doc)
        tf = Counter(terms)
        max_tf = max(tf.values()) if tf else 1
        scores = {}
        for term, count in tf.items():
            idf = math.log(n / (1 + df.get(term, 1)))
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
    
    # Recommandations
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
    # Construction du texte de l'apprenant
    user_text = " ".join([
        request.profil.objectif,
        request.profil.domaineInteret,
        " ".join(c.nom for c in request.profil.competences),
        request.profil.niveau
    ])
    
    # Textes des formations
    formation_texts = [
        " ".join([f.titre, f.description or "", f.motsCles or "", f.categorie or ""])
        for f in request.formations
    ]
    
    # TF-IDF
    all_texts = [user_text] + formation_texts
    tf_idf = compute_tf_idf(all_texts)
    
    user_vec = tf_idf.get("doc_0", {})
    results = []
    
    for i, formation in enumerate(request.formations):
        form_vec = tf_idf.get(f"doc_{i + 1}", {})
        score = cosine_similarity(user_vec, form_vec)
        
        # Bonus basé sur le niveau
        if request.profil.niveau and formation.niveau:
            if request.profil.niveau.lower() == formation.niveau.lower():
                score += 0.1
        
        # Bonus basé sur les compétences
        for comp in request.profil.competences:
            if comp.nom.lower() in formation.titre.lower() or comp.nom.lower() in (formation.motsCles or "").lower():
                score += 0.05 * (comp.niveau / 100)
        
        results.append({
            "formationId": formation.id,
            "score": min(score, 1.0),
            "methode": "TF_IDF"
        })
    
    # Trier par score décroissant
    results.sort(key=lambda x: x["score"], reverse=True)
    return results


@app.get("/health")
async def health():
    return {"status": "ok", "service": "SmartLearn IA"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)