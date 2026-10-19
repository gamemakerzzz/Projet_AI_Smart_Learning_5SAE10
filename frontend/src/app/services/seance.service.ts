import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Seance } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class SeanceService {
  private apiUrl = '/api/seances';

  constructor(private http: HttpClient) {}

  getAllByApprenant(apprenantId: number): Observable<Seance[]> {
    return this.http.get<Seance[]>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }

  getAllByApprenantAndFormation(apprenantId: number, formationId: number): Observable<Seance[]> {
    return this.http.get<Seance[]>(`${this.apiUrl}/apprenant/${apprenantId}/formation/${formationId}`);
  }

  create(apprenantId: number, seance: Partial<Seance>): Observable<Seance> {
    return this.http.post<Seance>(`${this.apiUrl}/apprenant/${apprenantId}`, seance);
  }

  update(id: number, seance: Partial<Seance>): Observable<Seance> {
    return this.http.put<Seance>(`${this.apiUrl}/${id}`, seance);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}