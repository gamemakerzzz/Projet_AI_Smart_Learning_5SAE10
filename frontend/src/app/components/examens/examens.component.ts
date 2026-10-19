import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { ExamenService } from '../../services/examen.service';
import { Examen } from '../../models/planner.model';

@Component({
  selector: 'app-examens',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="examens-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Calendrier des Examens</h1>
          <p class="page-subtitle">Consultez et gérez vos examens planifiés.</p>
        </div>
      </div>

      <div class="card">
        <div class="examens-list">
          <div *ngIf="examens.length === 0" class="empty-state">
            Aucun examen planifié pour le moment.
          </div>
          <div *ngFor="let examen of examens" class="examen-card">
            <div class="examen-date">
              <span class="examen-day">{{ (examen.dateExamen | date:'d') || '?' }}</span>
              <span class="examen-month">{{ (examen.dateExamen | date:'MMM') || '?' }}</span>
            </div>
            <div class="examen-info">
              <h3>{{ examen.titre }}</h3>
              <p>{{ examen.description || 'Aucune description' }}</p>
              <div class="examen-meta">
                <span class="badge badge-secondary">{{ examen.dureeMinutes }} min</span>
                <span class="badge badge-primary">Coefficient {{ examen.coefficient }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .examens-container { max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .examens-list { display: flex; flex-direction: column; gap: 1rem; }
    .examen-card { display: flex; gap: 1.5rem; padding: 1.25rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); }
    .examen-date { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 60px; padding: 0.5rem; background: var(--primary-light); border-radius: var(--radius-sm); }
    .examen-day { font-size: 1.5rem; font-weight: 800; color: var(--primary); }
    .examen-month { font-size: 0.8rem; text-transform: uppercase; color: var(--text-muted); }
    .examen-info { flex: 1; }
    .examen-info h3 { margin: 0 0 0.5rem 0; font-size: 1.1rem; }
    .examen-info p { color: var(--text-muted); margin: 0 0 0.75rem 0; font-size: 0.9rem; }
    .examen-meta { display: flex; gap: 0.5rem; }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
  `]
})
export class ExamensComponent implements OnInit {
  examens: Examen[] = [];

  constructor(
    public auth: AuthService,
    private examenService: ExamenService
  ) {}

  ngOnInit(): void {
    const apprenantId = this.auth.user()?.id;
    if (apprenantId) {
      this.examenService.getAllByApprenant(apprenantId).subscribe({
        next: (data) => (this.examens = data),
        error: () => (this.examens = [])
      });
    }
  }
}