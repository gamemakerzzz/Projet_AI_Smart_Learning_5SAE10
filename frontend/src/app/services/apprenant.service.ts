import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Apprenant, ApprenantRequest } from '../models/apprenant.model';
import { Profil } from '../models/profil.model';
import { Competence } from '../models/competence.model';

@Injectable({
  providedIn: 'root'
})
export class ApprenantService {
  private apiUrl = '/api/apprenants';
  private competencesApiUrl = '/api/competences';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Apprenant[]> {
    return this.http.get<Apprenant[]>(this.apiUrl);
  }

  getById(id: number): Observable<Apprenant> {
    return this.http.get<Apprenant>(`${this.apiUrl}/${id}`);
  }

  create(apprenant: ApprenantRequest): Observable<Apprenant> {
    return this.http.post<Apprenant>(this.apiUrl, apprenant);
  }

  update(id: number, apprenant: ApprenantRequest): Observable<Apprenant> {
    return this.http.put<Apprenant>(`${this.apiUrl}/${id}`, apprenant);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getProfil(apprenantId: number): Observable<Profil> {
    return this.http.get<Profil>(`${this.apiUrl}/${apprenantId}/profil`);
  }

  upsertProfil(apprenantId: number, profil: Profil): Observable<Profil> {
    return this.http.put<Profil>(`${this.apiUrl}/${apprenantId}/profil`, profil);
  }

  getCompetences(apprenantId: number): Observable<Competence[]> {
    return this.http.get<Competence[]>(`${this.apiUrl}/${apprenantId}/competences`);
  }

  addCompetence(apprenantId: number, competence: Competence): Observable<Competence> {
    return this.http.post<Competence>(`${this.apiUrl}/${apprenantId}/competences`, competence);
  }

  updateCompetence(competenceId: number, competence: Competence): Observable<Competence> {
    return this.http.put<Competence>(`${this.competencesApiUrl}/${competenceId}`, competence);
  }

  deleteCompetence(competenceId: number): Observable<void> {
    return this.http.delete<void>(`${this.competencesApiUrl}/${competenceId}`);
  }
}
