export interface Examen {
  id?: number;
  titre: string;
  description?: string;
  dateExamen: string;
  dureeMinutes: number;
  formationId?: number;
  apprenantId?: number;   // AJOUT : propriétaire de l'examen
  coefficient: number;
}

export interface Seance {
  id?: number;
  titre: string;
  description?: string;
  dateHeureDebut: string;
  dateHeureFin: string;
  statut?: string;
  formationId?: number;
}

export interface Notification {
  id?: number;
  message: string;
  type?: string;
  statut?: string;
  dateCreation?: string;
}

export interface PlanificationRequest {
  apprenantId: number;
  dateDebut: string;
  dateFin: string;
  formationIds?: number[];
}

export interface PlanificationResponse {
  id: number;
  titre: string;
  dateHeureDebut: string;
  dateHeureFin: string;
  statut?: string;
  formationId?: number;
  formationTitre?: string;
}

export interface ChapitreRevoirResponse {
  chapitreId: number;
  titre: string;
  formationId?: number;
  formationTitre?: string;
  ordre?: number;
  raison?: string;
  priorite?: number;
}

export interface StatistiquesResponse {
  apprenantId: number;
  apprenantNom: string;
  totalExamens: number;
  totalSeances: number;
  totalNotifications: number;
  totalChapitres: number;
  chapitresARevoir: number;
  competencesByLevel?: { [key: string]: number };
  formationProgress?: FormationProgress[];
  averageCompetenceLevel?: number;
}

export interface FormationProgress {
  formationId: number;
  titre: string;
  totalChapitres: number;
  chapitresFaibles: number;
}

export interface AnalyseIAResponse {
  styleApprentissage: string;
  forces: string[];
  faiblesses: string[];
  recommandationsPersonnalisees: string[];
  scoreProgression: number;
  competencesEvolution: { [key: string]: number };
  niveauGlobal: string;
}

export interface SeanceIAResponse {
  date: string;
  heureDebut: string;
  heureFin: string;
  titre: string;
  description: string;
  formationId?: number;
  formationTitre?: string;
  priorite?: number;
  motif?: string;
}

export interface PlanificationIAResponse {
  apprenantId: number;
  dateDebut: string;
  dateFin: string;
  totalSeances: number;
  periode: string;
  seances: SeanceIAResponse[];
}

export interface CreneauProbabiliteRequest {
  heure: number;
  jourSemaine: number;
  dureeMin: number;
  joursAvantExamen: number;
  seancesMemeJour?: number;
}

export interface CreneauProbabiliteResponse {
  heure: number;
  jourSemaine: number;
  dureeMin: number;
  probabilite: number;
  source: string;
}

export interface SeanceAvecProbabilite extends SeanceIAResponse {
  probabilite?: number;
  source?: string;
  heure?: number;
  dureeMin?: number;
}