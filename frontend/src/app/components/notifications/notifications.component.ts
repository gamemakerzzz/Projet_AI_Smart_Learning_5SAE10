import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../models/planner.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notifications-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Notifications</h1>
          <p class="page-subtitle">Consultez vos notifications et alertes.</p>
        </div>
      </div>

      <div class="card">
        <div class="notifications-list">
          <div *ngIf="notifications.length === 0" class="empty-state">
            Aucune notification pour le moment.
          </div>
          <div *ngFor="let notif of notifications" class="notification-card" [class.unread]="notif.statut === 'NON_LU'">
            <div class="notif-icon" [class.badge-info]="notif.type === 'INFO'" [class.badge-warning]="notif.type === 'ALERTE'" [class.badge-success]="notif.type === 'SUCCES'">
              {{ notif.type === 'INFO' ? 'ℹ️' : notif.type === 'ALERTE' ? '⚠️' : '✅' }}
            </div>
            <div class="notif-content">
              <p>{{ notif.message }}</p>
              <span class="notif-date">{{ notif.dateCreation | date:'dd/MM/yyyy HH:mm' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .notifications-container { max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .notifications-list { display: flex; flex-direction: column; gap: 1rem; }
    .notification-card { display: flex; gap: 1rem; padding: 1.25rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); }
    .notification-card.unread { border-left: 4px solid var(--primary); }
    .notif-icon { display: flex; align-items: center; justify-content: center; min-width: 40px; height: 40px; border-radius: 50%; font-size: 1.2rem; }
    .notif-content { flex: 1; }
    .notif-content p { margin: 0 0 0.5rem 0; font-size: 0.95rem; }
    .notif-date { font-size: 0.8rem; color: var(--text-muted); }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
  `]
})
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [];

  constructor(
    public auth: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const apprenantId = this.auth.user()?.id;
    if (apprenantId) {
      this.notificationService.getAllByApprenant(apprenantId).subscribe({
        next: (data) => (this.notifications = data),
        error: () => (this.notifications = [])
      });
    }
  }
}