import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StatistiquesResponse } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class StatistiquesService {
  private apiUrl = '/api/statistiques';

  constructor(private http: HttpClient) {}

  getStatistiques(apprenantId: number): Observable<StatistiquesResponse> {
    return this.http.get<StatistiquesResponse>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }
}