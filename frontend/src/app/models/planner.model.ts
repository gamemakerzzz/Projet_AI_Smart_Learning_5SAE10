export interface Examen {
  id?: number;
  titre: string;
  description?: string;
  dateExamen: string;
  dureeMinutes: number;
  formationId?: number;
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