import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from '../models/planner.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = '/api/notifications';

  constructor(private http: HttpClient) {}

  getAllByApprenant(apprenantId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/apprenant/${apprenantId}`);
  }

  getAllByApprenantAndType(apprenantId: number, type: string): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/apprenant/${apprenantId}/type/${type}`);
  }

  create(apprenantId: number, notification: Partial<Notification>): Observable<Notification> {
    return this.http.post<Notification>(`${this.apiUrl}/apprenant/${apprenantId}`, notification);
  }

  update(id: number, notification: Partial<Notification>): Observable<Notification> {
    return this.http.put<Notification>(`${this.apiUrl}/${id}`, notification);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}