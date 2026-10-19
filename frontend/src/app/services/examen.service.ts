import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Examen } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class ExamenService {
  private apiUrl = '/api/examens';

  constructor(private http: HttpClient) {}

  /** ADMIN uniquement : tous les examens de tous les apprenants */
  getAll(): Observable<Examen[]> {
    return this.http.get<Examen[]>(this.apiUrl);
  }

  getAllByApprenant(apprenantId: number): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }

  getAllByApprenantAndFormation(apprenantId: number, formationId: number): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${this.apiUrl}/apprenant/${apprenantId}/formation/${formationId}`);
  }

  getAllByApprenantAndUpcoming(apprenantId: number): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${this.apiUrl}/apprenant/${apprenantId}/a-venir`);
  }

  create(apprenantId: number, examen: Partial<Examen>): Observable<Examen> {
    return this.http.post<Examen>(`${this.apiUrl}/apprenant/${apprenantId}`, examen);
  }

  update(id: number, examen: Partial<Examen>): Observable<Examen> {
    return this.http.put<Examen>(`${this.apiUrl}/${id}`, examen);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}