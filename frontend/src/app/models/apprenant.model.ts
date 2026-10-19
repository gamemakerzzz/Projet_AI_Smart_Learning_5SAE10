import { Profil } from './profil.model';

export interface Apprenant {
  id?: number;
  nom: string;
  prenom: string;
  email: string;
  password?: string;
  profil?: Profil;
}

export interface ApprenantRequest {
  nom: string;
  prenom: string;
  email: string;
  password?: string;
}
