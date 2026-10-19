"""Build and execute the self-contained teaching notebook using the local runtime."""
import contextlib
import io
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / 'notebooks'
OUT.mkdir(exist_ok=True)

rows = [
    (1, 'Java pour débutants', 'Introduction à Java et à la programmation orientée objet.', 'Débutant', 'java programmation classes objets', 'Développement Web'),
    (2, 'Spring Boot Avancé', 'Développement API REST avec Java Spring Boot et SQL pour le backend.', 'Avancé', 'java spring boot backend api rest sql', 'Développement Web'),
    (3, 'Angular Fundamentals', 'Créer des interfaces web avec Angular et TypeScript.', 'Intermédiaire', 'angular typescript frontend composants', 'Développement Web'),
    (4, 'Python pour débutants', 'Apprendre les bases de Python et écrire des scripts.', 'Débutant', 'python variables boucles fonctions', 'Programmation'),
    (5, 'Analyse de données avec Python', 'Analyser des données avec pandas et réaliser des visualisations.', 'Intermédiaire', 'python pandas analyse données statistiques', 'Data Science'),
    (6, 'Machine Learning avec scikit-learn', 'Construire et évaluer des modèles de classification et régression.', 'Intermédiaire', 'python machine learning scikit learn modèles données', 'Data Science'),
    (7, 'SQL et bases de données', 'Écrire des requêtes SQL et concevoir des bases de données relationnelles.', 'Débutant', 'sql mysql requêtes jointures bases données', 'Bases de données'),
    (8, 'Docker et CI/CD', 'Déployer des applications avec Docker et automatiser les pipelines CI/CD.', 'Intermédiaire', 'docker devops déploiement pipeline ci cd', 'Cloud / DevOps'),
    (9, 'Kubernetes en pratique', 'Orchestrer des conteneurs et gérer des déploiements dans le cloud.', 'Avancé', 'kubernetes docker cloud devops orchestration', 'Cloud / DevOps'),
    (10, 'Sécurité des applications web', 'Sécuriser les API et comprendre les vulnérabilités web.', 'Intermédiaire', 'sécurité web api authentification vulnérabilités', 'Cybersécurité'),
    (11, 'Management équipe', 'Développer le leadership et la communication pour gérer une équipe.', 'Débutant', 'management leadership communication équipe', 'Management'),
    (12, 'Gestion de projet agile', 'Organiser les projets avec Scrum et les méthodes agiles.', 'Intermédiaire', 'gestion projet agile scrum équipe', 'Management'),
    (13, 'Design UX/UI', 'Concevoir des interfaces et améliorer expérience utilisateur.', 'Débutant', 'design ux ui interfaces utilisateur figma', 'Design'),
    (14, 'React et JavaScript', 'Développer des interfaces frontend avec React et JavaScript.', 'Intermédiaire', 'react javascript frontend web composants', 'Développement Web'),
    (15, 'Tests Java avec JUnit', 'Tester les applications Java et Spring Boot avec JUnit et Mockito.', 'Intermédiaire', 'java spring boot junit mockito tests backend', 'Développement Web'),
]
formations = [dict(zip(['id', 'titre', 'description', 'niveau', 'motsCles', 'categorie'], r)) for r in rows]
profils = [
    dict(nom='Profil backend fictif', niveau='Intermédiaire', objectif='Développeur Backend', domaineInteret='Développement Web', competences=['Java', 'Spring', 'SQL']),
    dict(nom='Profil data fictif', niveau='Intermédiaire', objectif='Analyse de données avec Python', domaineInteret='Data Science', competences=['Python', 'Pandas', 'Statistiques']),
    dict(nom='Profil DevOps fictif', niveau='Intermédiaire', objectif='Déploiement cloud et DevOps', domaineInteret='Cloud / DevOps', competences=['Docker', 'Kubernetes', 'CI/CD']),
    dict(nom='Profil management fictif', niveau='Débutant', objectif='Management équipe et leadership', domaineInteret='Management', competences=['Communication', 'Gestion projet']),
]
dataset = {'description': 'Dataset pédagogique synthétique, sans données personnelles ni labels de pertinence réels.', 'formations': formations, 'profils': profils}
(OUT / 'dataset_smartlearn.json').write_text(json.dumps(dataset, ensure_ascii=False, indent=2), encoding='utf-8')

cells = []
def md(s):
    cells.append(dict(cell_type='markdown', metadata={}, source=s.splitlines(keepends=True)))
def code(s):
    cells.append(dict(cell_type='code', metadata={}, source=s.splitlines(keepends=True), execution_count=None, outputs=[]))

md('''# SmartLearn — recommandations TF-IDF et similarité cosinus

Notebook pédagogique reproduisant le calcul de `mla-service/main.py`.

**Données :** 15 formations et 4 profils entièrement synthétiques, inclus dans ce notebook et fournis dans `dataset_smartlearn.json`. Ils ne sont pas extraits de MySQL.

**Exécution :** ouvrir dans Jupyter ou Google Colab, puis exécuter les cellules dans l’ordre. Si nécessaire, décommenter la commande d’installation. Aucun serveur ni connexion à MySQL n’est nécessaire.

Il s’agit de recommandation basée sur le contenu. Il n’y a pas d’entraînement supervisé ni de prédiction de réussite.''')
code('# Si scikit-learn est absent, décommenter puis exécuter :\n# %pip install scikit-learn\nimport json\nfrom sklearn.feature_extraction.text import TfidfVectorizer\nfrom sklearn.metrics.pairwise import cosine_similarity')
md('## 1. Charger le dataset\nLe dataset est intégré pour que le notebook fonctionne seul. Le champ `nom` sert uniquement à identifier les profils dans les exemples ; il ne participe pas au calcul.')
code('dataset = json.loads(' + repr(json.dumps(dataset, ensure_ascii=False)) + ')\nformations = dataset["formations"]\nprofils = dataset["profils"]\nprint(f"{len(formations)} formations, {len(profils)} profils")\nfor f in formations:\n    print(f"{f[\'id\']:2} | {f[\'titre\']} | {f[\'categorie\']}")')
md('## 2. Construire les textes\nMême ordre de champs que le service Python. Les textes passent en minuscules. Les noms des compétences sont utilisés, mais pas leurs pourcentages de maîtrise. Le niveau est traité comme un mot, sans contrainte pédagogique.')
code('''def texte_profil(profil):
    competences = " ".join(profil.get("competences") or [])
    return f"{profil.get('objectif') or ''} {profil.get('domaineInteret') or ''} {competences} {profil.get('niveau') or ''}".strip().lower()

def texte_formation(f):
    return f"{f['titre']} {f.get('description') or ''} {f.get('motsCles') or ''} {f.get('categorie') or ''} {f.get('niveau') or ''}".strip().lower()

profil = profils[0]
corpus = [texte_profil(profil)] + [texte_formation(f) for f in formations]
print("PROFIL :", corpus[0])
print("FORMATION :", corpus[2])''')
md('''## 3. Transformer les textes avec TF-IDF

`ngram_range=(1, 2)` conserve les mots et les paires de mots, par exemple `spring` et `spring boot`.

Avec les paramètres par défaut utilisés ici, le poids est basé sur la fréquence du terme multipliée par `IDF = log((1 + N) / (1 + df)) + 1`, puis le vecteur est normalisé en norme L2. `N` est le nombre de documents (profil inclus) et `df` le nombre de documents contenant le terme.

Comme dans le projet, `fit_transform` est effectué sur le profil et le catalogue à chaque demande. Changer le catalogue peut donc changer les scores. Aucun traitement spécifique des synonymes ni liste française de mots vides n’est ajouté.''')
code('''vectorizer = TfidfVectorizer(ngram_range=(1, 2))
matrice = vectorizer.fit_transform(corpus)
print("Dimensions (documents, termes) :", matrice.shape)
termes = vectorizer.get_feature_names_out()
poids = matrice[0].toarray()[0]
for i in poids.argsort()[::-1][:12]:
    if poids[i] > 0:
        print(f"{termes[i]:25} {poids[i]:.4f}")''')
md('''## 4. Calculer la similarité cosinus

`cosinus(u, v) = (u · v) / (||u|| × ||v||)`

On compare le premier vecteur (profil) à chacun des suivants (formations). Avec ces vecteurs TF-IDF non négatifs, les scores sont compris entre 0 et 1. Un affichage de 19 % signifie une similarité de 0,19, pas une probabilité de réussite ni une précision du système.''')
code('''similarites = cosine_similarity(matrice[0:1], matrice[1:])[0]
for f, score in zip(formations, similarites):
    print(f"{f['titre']:38} {score:.4f}")''')
md('## 5. Fonction complète identique au calcul du projet\nLe score est arrondi à deux décimales **avant le tri**, comme dans `main.py`. Les égalités conservent l’ordre du catalogue. Le service retourne toutes les formations ; le top 5 ci-dessous est seulement un choix d’affichage.')
code('''def recommander(profil, formations):
    if not formations:
        return []
    corpus = [texte_profil(profil)] + [texte_formation(f) for f in formations]
    vectorizer = TfidfVectorizer(ngram_range=(1, 2))
    if not any(vectorizer.build_analyzer()(texte) for texte in corpus):
        return [{"formationId": f["id"], "titre": f["titre"], "score": 0.0} for f in formations]
    matrice = vectorizer.fit_transform(corpus)
    scores = cosine_similarity(matrice[0:1], matrice[1:])[0]
    resultats = [{"formationId": f["id"], "titre": f["titre"], "score": round(float(s), 2)}
                 for f, s in zip(formations, scores)]
    resultats.sort(key=lambda r: r["score"], reverse=True)
    return resultats

for p in profils:
    print("\\n" + p["nom"])
    for r in recommander(p, formations)[:5]:
        print(f"  {r['score']:.0%} | {r['titre']}")''')
md('## 6. Tester ton propre profil\nModifie les champs puis réexécute la cellule. Des scores faibles sont possibles même pour une formation utile : ce modèle compare les mots employés.')
code('''mon_profil = {
    "objectif": "Développeur Backend Java",
    "domaineInteret": "Développement Web",
    "niveau": "Intermédiaire",
    "competences": ["Java", "Spring", "SQL"]
}
for r in recommander(mon_profil, formations)[:5]:
    print(f"{r['score']:.0%} | {r['titre']}")''')
md('## 7. Vérifier le comportement\nCes assertions vérifient le fonctionnement, pas la qualité pédagogique des recommandations. Sans jugements de pertinence indépendants, on ne peut pas annoncer une accuracy ou une précision réelle.')
code('''resultats = recommander(profils[0], formations)
assert len(resultats) == len(formations)
assert all(0 <= r["score"] <= 1 for r in resultats)
assert resultats == sorted(resultats, key=lambda r: r["score"], reverse=True)
assert recommander(profils[0], []) == []
assert all(r["score"] == 0 for r in recommander({}, formations))
assert recommander({}, [{"id": 99, "titre": ""}])[0]["score"] == 0
print("Vérifications fonctionnelles réussies.")''')
md('''## 8. Limites et lien avec SmartLearn

- Le notebook reproduit le moteur Python, pas la méthode Java de secours `MOTS_CLES`.
- FastAPI expose ce calcul via `POST /recommend` ; Spring Boot fournit les données de MySQL et Angular affiche les résultats.
- Les scores reposent sur la proximité textuelle, sans compréhension des synonymes, des prérequis ou de la progression.
- Les notes, clics et formations terminées ne sont pas utilisés.
- Pour évaluer la pertinence, faire annoter des paires profil/formation par des évaluateurs, puis mesurer par exemple Precision@5 ou NDCG@5 sur des données réservées à l’évaluation. Le dataset synthétique fourni sert à expliquer et tester le code, pas à prouver sa performance réelle.
''')

# Execute the actual notebook code and keep text outputs for immediate reading.
namespace = {}
count = 0
for cell in cells:
    if cell['cell_type'] != 'code':
        continue
    count += 1
    stream = io.StringIO()
    with contextlib.redirect_stdout(stream):
        exec(compile(''.join(cell['source']), f'notebook-cell-{count}', 'exec'), namespace)
    cell['execution_count'] = count
    if stream.getvalue():
        cell['outputs'] = [dict(output_type='stream', name='stdout', text=stream.getvalue().splitlines(keepends=True))]

# Compare every synthetic profile with the actual project's implementation.
from main import RecommendationRequest, recommend_formations
for profil in profils:
    actual = [r.model_dump() for r in recommend_formations(RecommendationRequest(profil=profil, formations=formations))]
    assert namespace['recommander'](profil, formations) == actual

notebook = dict(cells=cells, metadata={'kernelspec': {'display_name': 'Python 3', 'language': 'python', 'name': 'python3'}, 'language_info': {'name': 'python', 'version': '3.11'}}, nbformat=4, nbformat_minor=4)
path = OUT / 'SmartLearn_TFIDF_Recommandations.ipynb'
path.write_text(json.dumps(notebook, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Created {path}; {count} code cells executed; 4 profiles match the project engine exactly.')
