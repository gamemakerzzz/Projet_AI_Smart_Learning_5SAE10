import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreneauProbabiliteRequest, CreneauProbabiliteResponse } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class SeanceProbabiliteService {
  private apiUrl = '/api/seance/probabilite';

  constructor(private http: HttpClient) {}

  calculerProbabilites(apprenantId: number, creneaux: CreneauProbabiliteRequest[]): Observable<CreneauProbabiliteResponse[]> {
    return this.http.post<CreneauProbabiliteResponse[]>(
      `${this.apiUrl}/${apprenantId}`, creneaux
    );
  }
}
