import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanificationRequest, PlanificationResponse } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class PlanificationService {
  private apiUrl = '/api/planification';

  constructor(private http: HttpClient) {}

  generate(request: PlanificationRequest): Observable<PlanificationResponse[]> {
    return this.http.post<PlanificationResponse[]>(this.apiUrl, request);
  }

  getAllByApprenant(apprenantId: number): Observable<PlanificationResponse[]> {
    return this.http.get<PlanificationResponse[]>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }
}