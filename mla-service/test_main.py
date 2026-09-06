import unittest
from main import ProfilData, FormationData, RecommendationRequest, recommend_formations

class RecommendationTests(unittest.TestCase):
    def test_unrelated_course_has_no_angular_boost(self):
        result = recommend_formations(RecommendationRequest(
            profil=ProfilData(objectif="angular web"),
            formations=[FormationData(id=1, titre="management finance")]))
        self.assertEqual(result[0].score, 0.0)

    def test_empty_vocabulary(self):
        result = recommend_formations(RecommendationRequest(
            profil=ProfilData(), formations=[FormationData(id=1, titre="!")]))
        self.assertEqual(result[0].score, 0.0)

    def test_null_fields_do_not_create_similarity(self):
        result = recommend_formations(RecommendationRequest(
            profil=ProfilData(objectif=None, niveau=None, domaineInteret=None),
            formations=[FormationData(id=1, titre="finance", description=None, niveau=None)]))
        self.assertEqual(result[0].score, 0.0)

    def test_relevant_course_ranks_first(self):
        result = recommend_formations(RecommendationRequest(
            profil=ProfilData(objectif="python programmation"),
            formations=[FormationData(id=1, titre="finance"),
                        FormationData(id=2, titre="python programmation")]))
        self.assertEqual(result[0].formationId, 2)
        self.assertGreater(result[0].score, result[1].score)

    def test_empty_catalogue(self):
        self.assertEqual(recommend_formations(RecommendationRequest(
            profil=ProfilData(), formations=[])), [])

if __name__ == "__main__":
    unittest.main()
