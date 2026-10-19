import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChapitreRevoirResponse } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class ChapitreRevoirService {
  private apiUrl = '/api/chapitres-revoir';

  constructor(private http: HttpClient) {}

  detectChapitresARevoir(apprenantId: number): Observable<ChapitreRevoirResponse[]> {
    return this.http.get<ChapitreRevoirResponse[]>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }
}