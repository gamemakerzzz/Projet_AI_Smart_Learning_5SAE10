import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Recommendation } from '../models/recommendation.model';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private apiUrl = '/api/recommendations';

  constructor(private http: HttpClient) {}

  getRecommendationsForApprenant(apprenantId: number): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }
}
