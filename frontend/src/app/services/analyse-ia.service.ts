import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalyseIAResponse } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyseIAService {
  private apiUrl = '/api/analyse-ia';

  constructor(private http: HttpClient) {}

  analyserApprenant(apprenantId: number): Observable<AnalyseIAResponse> {
    return this.http.get<AnalyseIAResponse>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }
}
