# 🎓 SMARTLEARN — Plateforme Intelligente de Gestion et Recommandation de Formations

SMARTLEARN est une application web Full Stack dotée d'un moteur de Recommandation basé sur l'Intelligence Artificielle (Machine Learning), développée dans le cadre d'un projet universitaire.

---

## 🏗️ Architecture Globale du Projet

```
+------------------+         REST API         +----------------------+
|  Angular 19/21   | <---------------------> |  Spring Boot (Java)  |
| (Frontend Web)   |   http://localhost:4200  | (Backend Core)       |
+------------------+                          +----------+-----------+
                                                         |
                                            +------------+------------+
                                            |                         |
                                     JPA / Hibernate           REST API (JSON)
                                            |                         |
                                            v                         v
                                   +-----------------+    +------------------------+
                                   |  MySQL / H2 DB  |    |  Python FastAPI (MLA)  |
                                   |  (Persistance)  |    |  TF-IDF + Cosine Sim   |
                                   +-----------------+    +------------------------+
```

---

## 🛠️ Stack Technique

- **Backend** : Java 17, Spring Boot 3.5, Spring Data JPA, Spring Validation, Maven, REST API.
- **Frontend** : Angular (Standalone components), TypeScript, Reactive Forms, HttpClient, Router.
- **Base de Données** : MySQL pour le fonctionnement normal ; H2 en mémoire pour les tests.
- **MLA (Machine Learning)** : Python 3.11, FastAPI, scikit-learn (`TfidfVectorizer`, `cosine_similarity`).
- **DevOps & CI/CD** : Docker, Docker Compose, GitLab CI/CD.

---

## 📂 Structure du Projet

```
smartlearn/
│
├── backend/
│   ├── src/main/java/com/smartlearn/
│   │   ├── config/ (DataInitializer, WebConfig)
│   │   ├── controller/ (Categories, Formations, Apprenants, Recommandations)
│   │   ├── dto/ (Data Transfer Objects & Mappers)
│   │   ├── entity/ (Categorie, Formation, Chapitre, Apprenant, Profil, Competence)
│   │   ├── exception/ (GlobalExceptionHandler)
│   │   ├── repository/ (Interfaces Spring Data JPA)
│   │   └── service/ (Logique Métier & Client REST MLA)
│   ├── src/test/java/com/smartlearn/ (Tests Unitaires / Integration)
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/app/
│   │   ├── components/ (Dashboard, Formations, Categories, Apprenants, Profil, Recommandations)
│   │   ├── models/ (Interfaces TypeScript)
│   │   └── services/ (Services RxJS HttpClient)
│   ├── package.json
│   └── Dockerfile
│
├── mla-service/
│   ├── main.py (Microservice FastAPI TF-IDF)
│   ├── requirements.txt
│   └── Dockerfile
│
├── docker-compose.yml
├── .gitlab-ci.yml
└── README.md
```

---

## 🚀 Guide d'Installation et Lancement

### Option 1 : Lancement avec Docker Compose (Recommandé)

Une seule commande permet de builder et lancer tous les services (MySQL, Backend, Frontend, MLA) :

```bash
copy .env.example .env
# Modifier les valeurs de .env avant le premier démarrage
docker compose up --build
```

- **Frontend Angular** : `http://localhost:4200`
- **Backend Spring Boot** : `http://localhost:8082`
- **Microservice MLA FastAPI** : `http://localhost:8000`

Les ports Docker MySQL et backend sont respectivement `3307` et `8082` sur la
machine hôte, afin d'éviter les conflits avec MySQL/Java déjà installés. Les
services communiquent entre eux avec les ports internes `3306` et `8081`.

### CI/CD GitLab

Le pipeline `.gitlab-ci.yml` compile et teste les trois composants, puis publie
les images `backend`, `frontend` et `mla-service` dans le GitLab Container
Registry avec le tag du commit. La branche par défaut reçoit aussi le tag
`latest`. Le job de publication nécessite un runner GitLab autorisant Docker-in-Docker.

---

### Option 2 : Lancement Manuel par composant

#### 1. Backend Spring Boot
```bash
cd backend
mvn spring-boot:run
```
Créer au préalable la base MySQL `smartlearn` et configurer `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.

*(Le backend démarre sur `http://localhost:8081` et se charge automatiquement des données de test via `DataInitializer.java`).*

#### 2. Frontend Angular
```bash
cd frontend
npm install
npm start
```
*(Le frontend démarre sur `http://localhost:4200`).*

#### 3. Microservice MLA Python FastAPI
```bash
cd mla-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
*(Le service MLA démarre sur `http://localhost:8000`).*

---

## 🤖 Algorithme de Recommandation (Machine Learning)

1. **Représentation du Profil Apprenant** :
   Concaténation de : `Objectif` + `Domaine d'intérêt` + `Compétences` + `Niveau`.
   *Exemple* : *"développeur backend développement web java spring sql intermédiaire"*

2. **Représentation de la Formation** :
   Concaténation de : `Titre` + `Description` + `Mots-clés` + `Catégorie` + `Niveau`.
   *Exemple* : *"spring boot avancé développement d'api rest professionnelles java spring springboot backend rest api it avancé"*

3. **Vectorisation & Calcul** :
   - Application de `TfidfVectorizer` (scikit-learn) avec n-grammes (1,2).
   - Calcul du score par **Similarité Cosinus** (`cosine_similarity`).
   - Restitution des formations triées par score descendant exprimé en pourcentage (ex: **94%**, **83%**, **61%**).

---

## 🎯 Endpoints REST Principaux

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/api/categories` | Liste des catégories |
| `POST` | `/api/categories` | Créer une catégorie |
| `GET` | `/api/formations` | Liste des formations (avec filtre `niveau`) |
| `GET` | `/api/formations/search?keyword=java` | Recherche par mot-clé |
| `GET` | `/api/formations/categorie/{id}` | Formations par catégorie |
| `POST` | `/api/formations` | Créer une formation |
| `GET` | `/api/formations/{id}/chapitres` | Chapitres d'une formation |
| `GET` | `/api/apprenants` | Liste des apprenants |
| `GET` | `/api/apprenants/{id}/profil` | Profil d'un apprenant |
| `PUT` | `/api/apprenants/{id}/profil` | Créer/Modifier le profil |
| `GET` | `/api/apprenants/{id}/competences` | Compétences d'un apprenant |
| `POST` | `/api/apprenants/{id}/competences` | Ajouter une compétence |
| `GET` | `/api/recommendations/apprenant/{id}` | Obtenir les recommandations MLA |

---

## 🎬 Scénario de Démonstration devant le Jury

1. **Administration / Gestion** :
   - Accéder à la page **Formations**, cliquer sur **+ Nouvelle Formation**.
   - Créer la formation *"Spring Boot Avancé"* (Catégorie: *IT*, Mots-clés: *java spring backend rest api*).
2. **Apprenant & Profil** :
   - Naviguer vers **Apprenants & Profils**, sélectionner l'apprenant modèle **Ahmed Benali**.
   - Définir son objectif (*Développeur Backend*), son intérêt (*Développement Web*) et ajouter ses compétences (*Java: 80%*, *Spring: 50%*, *SQL: 70%*).
3. **Recommandation MLA** :
   - Cliquer sur **✨ Recommandations MLA** ou le bouton **"OBTENIR MES RECOMMANDATIONS"**.
4. **Résultat attendu** : les formations sont triées par similarité avec le profil. Les scores sont calculés à partir du corpus et ne sont pas des pourcentages garantis ni des probabilités de réussite.

## Vérification

- Backend : `cd backend` puis `mvn test` (base H2 isolée configurée dans la classe de test, transactions annulées après chaque test).
- Frontend : `cd frontend` puis `npm test -- --watch=false` et `npm run build`.
- Python : à la racine, `python -m unittest discover -s mla-service -v`.

Les appels du navigateur utilisent `/api`. Le serveur Angular les relaie vers `localhost:8081` en développement ; Nginx les relaie vers le service Docker `backend:8081`.

Le moteur Python utilise TF-IDF et la similarité cosinus sans scores imposés. Si le service Python échoue, le backend utilise un score de recouvrement de mots-clés ; ce mode de secours ne produit pas les mêmes scores que TF-IDF.

## Connexion et rôles

La page `/#/login` permet de se connecter avec une adresse e-mail et un mot de passe.

- **ADMIN** : tableau de bord, gestion des catégories, formations, chapitres et comptes apprenants, consultation des profils et recommandations.
- **USER** : catalogue et chapitres en lecture seule, modification de son propre profil et de ses compétences, recommandations personnelles.

Les contrôles existent dans les routes Angular et dans l'API Spring Security. Le serveur refuse les données d'un autre apprenant, même si l'identifiant est modifié dans l'URL. Les nouveaux comptes créés par l'administrateur reçoivent toujours le rôle USER ; aucun rôle transmis par le navigateur n'est accepté.

Les comptes existants deviennent USER via la colonne `role` de la table `apprenants`. Il n'y a pas de nouvelle table métier ni de fichier application.properties supplémentaire.

Au premier démarrage sans administrateur, un compte `admin@smartlearn.local` est créé avec un mot de passe aléatoire affiché une seule fois dans les logs du backend (`Initial administrator account`). Enregistrer ce mot de passe. On peut définir `SMARTLEARN_ADMIN_EMAIL` et `SMARTLEARN_ADMIN_PASSWORD` avant ce premier démarrage ; ces variables ne réinitialisent pas un compte déjà créé.

Dans cette installation locale, les identifiants générés ont été enregistrés dans `.local/admin-credentials.txt` (dossier exclu de Git). Les comptes de démonstration existants sont `ahmed@example.com` et `sarah@example.com`, avec le mot de passe initial `password123`, sauf modification préalable.

L'administrateur crée les comptes USER depuis **Apprenants → Nouvel Apprenant**, puis communique les identifiants à l'apprenant. Il n'y a pas d'inscription publique ni de choix libre du rôle.

La connexion utilise une session serveur et un cookie HttpOnly, avec protection CSRF. Angular demande un nouveau jeton CSRF après la connexion. Les mots de passe sont hachés avec BCrypt. Pour un déploiement HTTPS, activer `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`.

Référence : [Spring Security — protection CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

