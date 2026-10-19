import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanificationIAResponse } from '../models/planner.model';

@Injectable({ providedIn: 'root' })
export class PlanificationIAService {
  private apiUrl = '/api/planification-ia';

  constructor(private http: HttpClient) {}

  genererPlanification(
    apprenantId: number,
    dateDebut: string,
    dateFin: string,
    formationIds?: number[]
  ): Observable<PlanificationIAResponse> {
    return this.http.post<PlanificationIAResponse>(`${this.apiUrl}/generer`, {
      apprenantId,
      dateDebut,
      dateFin,
      formationIds
    });
  }
}