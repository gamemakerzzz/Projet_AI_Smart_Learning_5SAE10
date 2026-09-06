import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Formation, FormationRequest } from '../models/formation.model';
import { Chapitre } from '../models/chapitre.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {
  private apiUrl = '/api/formations';
  private chapitresApiUrl = '/api/chapitres';

  constructor(private http: HttpClient) {}

  getAll(niveau?: string): Observable<Formation[]> {
    let params = new HttpParams();
    if (niveau) {
      params = params.set('niveau', niveau);
    }
    return this.http.get<Formation[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.apiUrl}/${id}`);
  }

  search(keyword: string): Observable<Formation[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<Formation[]>(`${this.apiUrl}/search`, { params });
  }

  getByCategorie(categorieId: number): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/categorie/${categorieId}`);
  }

  create(request: FormationRequest): Observable<Formation> {
    return this.http.post<Formation>(this.apiUrl, request);
  }

  update(id: number, request: FormationRequest): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getChapitres(formationId: number): Observable<Chapitre[]> {
    return this.http.get<Chapitre[]>(`${this.apiUrl}/${formationId}/chapitres`);
  }

  addChapitre(formationId: number, chapitre: Chapitre): Observable<Chapitre> {
    return this.http.post<Chapitre>(`${this.apiUrl}/${formationId}/chapitres`, chapitre);
  }

  updateChapitre(chapitreId: number, chapitre: Chapitre): Observable<Chapitre> {
    return this.http.put<Chapitre>(`${this.chapitresApiUrl}/${chapitreId}`, chapitre);
  }

  deleteChapitre(chapitreId: number): Observable<void> {
    return this.http.delete<void>(`${this.chapitresApiUrl}/${chapitreId}`);
  }
}
