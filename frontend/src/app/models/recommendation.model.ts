export interface Recommendation {
  formationId: number;
  titre?: string;
  score: number; // 0.0 to 1.0 or percentage
  description?: string;
  niveau?: string;
  categorieNom?: string;
}
