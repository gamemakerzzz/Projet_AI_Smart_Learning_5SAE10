
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Notification } from '../models/planner.model';

export interface UnreadCount {
  count: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly apiUrl = '/api/notifications';

  // Compteur de notifications non lues
  private readonly unreadCountSubject = new BehaviorSubject<number>(0);
  public readonly unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private http: HttpClient) {}

  getAllByApprenant(apprenantId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(
      `${this.apiUrl}/apprenant/${apprenantId}`
    );
  }

  getAllByApprenantAndType(
    apprenantId: number,
    type: string
  ): Observable<Notification[]> {
    return this.http.get<Notification[]>(
      `${this.apiUrl}/apprenant/${apprenantId}/type/${type}`
    );
  }

  getAllByApprenantAndStatut(
    apprenantId: number,
    statut: string
  ): Observable<Notification[]> {
    return this.http.get<Notification[]>(
      `${this.apiUrl}/apprenant/${apprenantId}/statut/${statut}`
    );
  }

  getUnreadCount(apprenantId: number): Observable<UnreadCount> {
    return this.http.get<UnreadCount>(
      `${this.apiUrl}/apprenant/${apprenantId}/unread-count`
    );
  }

  create(
    apprenantId: number,
    notification: Partial<Notification>
  ): Observable<Notification> {
    return this.http.post<Notification>(
      `${this.apiUrl}/apprenant/${apprenantId}`,
      notification
    );
  }

  update(
    id: number,
    notification: Partial<Notification>
  ): Observable<Notification> {
    return this.http.put<Notification>(
      `${this.apiUrl}/${id}`,
      notification
    );
  }

  markAsRead(id: number): Observable<Notification> {
    return this.http.patch<Notification>(
      `${this.apiUrl}/${id}/read`,
      {}
    );
  }

  markAllAsRead(apprenantId: number): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/apprenant/${apprenantId}/mark-all-read`,
      {}
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteAllByApprenant(apprenantId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/apprenant/${apprenantId}`
    );
  }

  // Mettre à jour le compteur de notifications non lues
  updateUnreadCount(count: number): void {
    this.unreadCountSubject.next(count);
  }
}
