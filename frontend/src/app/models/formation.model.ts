import { Categorie } from './categorie.model';

export interface Formation {
  id?: number;
  titre: string;
  description?: string;
  prix: number;
  niveau: string;
  motsCles?: string;
  categorie?: Categorie;
}

export interface FormationRequest {
  titre: string;
  description?: string;
  prix: number;
  niveau: string;
  motsCles?: string;
  categorieId: number;
}
