import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { SeanceService } from '../../services/seance.service';
import { Seance } from '../../models/planner.model';

@Component({
  selector: 'app-seances',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="seances-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Planification des Séances</h1>
          <p class="page-subtitle">Consultez et gérez vos séances de révision planifiées.</p>
        </div>
      </div>

      <div class="card">
        <div class="seances-list">
          <div *ngIf="seances.length === 0" class="empty-state">
            Aucune séance planifiée pour le moment.
          </div>
          <div *ngFor="let seance of seances" class="seance-card">
            <div class="seance-date">
              <span class="seance-day">{{ (seance.dateHeureDebut | date:'d') || '?' }}</span>
              <span class="seance-month">{{ (seance.dateHeureDebut | date:'MMM') || '?' }}</span>
            </div>
            <div class="seance-info">
              <h3>{{ seance.titre }}</h3>
              <p>{{ seance.description || 'Aucune description' }}</p>
              <div class="seance-meta">
                <span class="badge badge-secondary">{{ seance.dateHeureDebut | date:'HH:mm' }} - {{ seance.dateHeureFin | date:'HH:mm' }}</span>
                <span class="badge" [class.badge-success]="seance.statut === 'TERMINE'" [class.badge-warning]="seance.statut === 'EN_COURS'" [class.badge-secondary]="seance.statut === 'PLANIFIE'">
                  {{ seance.statut || 'PLANIFIE' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .seances-container { max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .seances-list { display: flex; flex-direction: column; gap: 1rem; }
    .seance-card { display: flex; gap: 1.5rem; padding: 1.25rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); }
    .seance-date { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 60px; padding: 0.5rem; background: var(--primary-light); border-radius: var(--radius-sm); }
    .seance-day { font-size: 1.5rem; font-weight: 800; color: var(--primary); }
    .seance-month { font-size: 0.8rem; text-transform: uppercase; color: var(--text-muted); }
    .seance-info { flex: 1; }
    .seance-info h3 { margin: 0 0 0.5rem 0; font-size: 1.1rem; }
    .seance-info p { color: var(--text-muted); margin: 0 0 0.75rem 0; font-size: 0.9rem; }
    .seance-meta { display: flex; gap: 0.5rem; }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
  `]
})
export class SeancesComponent implements OnInit {
  seances: Seance[] = [];

  constructor(
    public auth: AuthService,
    private seanceService: SeanceService
  ) {}

  ngOnInit(): void {
    const apprenantId = this.auth.user()?.id;
    if (apprenantId) {
      this.seanceService.getAllByApprenant(apprenantId).subscribe({
        next: (data) => (this.seances = data),
        error: () => (this.seances = [])
      });
    }
  }
}