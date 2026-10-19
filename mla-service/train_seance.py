import json
import numpy as np
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.metrics import accuracy_score, f1_score, roc_auc_score

FEATURES = ["heure", "jour_semaine", "duree_min", "jours_avant_examen", "seances_meme_jour"]

def make_data(n=5000, seed=42):
    rng = np.random.default_rng(seed)
    heure = rng.integers(7, 24, n)
    jour = rng.integers(0, 7, n)              # 0 = lundi
    duree = rng.choice([30, 45, 60, 90, 120, 180], n)
    jours_ex = rng.integers(1, 46, n)
    meme_jour = rng.integers(0, 4, n)

    z = (1.2
         - 0.9 * (heure >= 21)                # tard le soir
         - 0.6 * (heure <= 8)                 # très tôt
         - 0.012 * (duree - 60)               # séances longues
         - 0.5 * meme_jour                    # journée chargée
         + 0.4 * (jour >= 5)                  # week-end
         + 0.03 * np.maximum(0, 10 - jours_ex)  # examen proche : plus motivé
         + rng.normal(0, 0.5, n))
    y = (rng.random(n) < 1 / (1 + np.exp(-z))).astype(int)   # 1 = séance réalisée
    X = np.column_stack([heure, jour, duree, jours_ex, meme_jour])
    return X, y

if __name__ == "__main__":
    X, y = make_data()
    Xtr, Xte, ytr, yte = train_test_split(X, y, test_size=0.2, random_state=0, stratify=y)

    base = LogisticRegression(max_iter=1000).fit(Xtr, ytr)
    grid = GridSearchCV(
        RandomForestClassifier(random_state=0),
        {"n_estimators": [100, 200], "max_depth": [4, 6, 8], "min_samples_leaf": [5, 10]},
        cv=3, scoring="f1", n_jobs=-1,
    ).fit(Xtr, ytr)
    model = grid.best_estimator_

    def ev(m):
        p = m.predict(Xte)
        return {"accuracy": round(accuracy_score(yte, p), 3),
                "F1": round(f1_score(yte, p), 3),
                "AUC": round(roc_auc_score(yte, m.predict_proba(Xte)[:, 1]), 3)}

    metrics = {
        "baseline_logreg": ev(base),
        "random_forest": ev(model),
        "best_params": grid.best_params_,
        "feature_importance": dict(zip(FEATURES, [round(float(v), 3) for v in model.feature_importances_])),
        "data": "synthetique (5000 lignes)",
    }
    joblib.dump(model, "seance_model.joblib")
    json.dump(metrics, open("metrics_seance.json", "w"), indent=2)
    print(json.dumps(metrics, indent=2))
