export interface Recommendation {
  formationId: number;
  methode?: 'TF_IDF' | 'MOTS_CLES';
  titre?: string;
  score: number; // 0.0 to 1.0 or percentage
  description?: string;
  niveau?: string;
  categorieNom?: string;
}
