import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { FormationService } from '../../services/formation.service';
import { PlanificationService } from '../../services/planification.service';
import { Formation } from '../../models/formation.model';
import { PlanificationRequest, PlanificationResponse } from '../../models/planner.model';

@Component({
  selector: 'app-planificateur',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="planificateur-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Planificateur Intelligent</h1>
          <p class="page-subtitle">Organisez vos séances de révision de façon automatique.</p>
        </div>
      </div>

      <div class="card">
        <h2>Générer un plan de révision</h2>
        <form (ngSubmit)="generatePlan()" class="plan-form">
          <div class="form-group">
            <label class="form-label" for="plan-date-debut">Date de début *</label>
            <input id="plan-date-debut" type="date" [(ngModel)]="form.dateDebut" name="dateDebut" class="form-control" required>
          </div>
          <div class="form-group">
            <label class="form-label" for="plan-date-fin">Date de fin *</label>
            <input id="plan-date-fin" type="date" [(ngModel)]="form.dateFin" name="dateFin" class="form-control" required>
          </div>
          <div class="form-group">
            <span class="form-label">Formations à planifier</span>
            <div class="formations-checkboxes">
              <label *ngFor="let f of formations" class="checkbox-label">
                <input type="checkbox" [checked]="isSelected(f.id)" (change)="toggleFormation(f.id)">
                {{ f.titre }}
              </label>
            </div>
            <div *ngIf="formations.length === 0" class="text-muted">Chargement des formations...</div>
          </div>
          <button type="submit" class="btn btn-primary" [disabled]="loading">
            {{ loading ? 'Génération...' : 'Générer le plan' }}
          </button>
        </form>
      </div>

      <div *ngIf="resultats.length > 0" class="card">
        <h2>Résultats de la planification ({{ resultats.length }} séances)</h2>
        <div class="resultats-list">
          <div *ngFor="let r of resultats" class="resultat-card">
            <div class="resultat-date">
              <span class="resultat-day">{{ r.dateHeureDebut | date:'d' }}</span>
              <span class="resultat-month">{{ r.dateHeureDebut | date:'MMM' }}</span>
            </div>
            <div class="resultat-info">
              <h3>{{ r.titre }}</h3>
              <p>{{ r.formationTitre || 'Formation' }}</p>
              <div class="resultat-meta">
                <span class="badge badge-secondary">{{ r.dateHeureDebut | date:'HH:mm' }} - {{ r.dateHeureFin | date:'HH:mm' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="resultats.length === 0 && hasSearched && !loading" class="card empty-state">
        Aucune séance générée pour cette période.
      </div>
    </div>
  `,
  styles: [`
    .planificateur-container { max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .card { padding: 1.5rem; margin-bottom: 1.5rem; }
    .plan-form { display: flex; flex-direction: column; gap: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-label { font-size: 0.85rem; font-weight: 600; }
    .form-control { padding: 0.6rem; border: 1px solid var(--border-color); border-radius: var(--radius-sm); }
    .formations-checkboxes { display: flex; flex-direction: column; gap: 0.5rem; }
    .checkbox-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
    .resultats-list { display: flex; flex-direction: column; gap: 1rem; margin-top: 1rem; }
    .resultat-card { display: flex; gap: 1.5rem; padding: 1.25rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); }
    .resultat-date { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 60px; padding: 0.5rem; background: var(--primary-light); border-radius: var(--radius-sm); }
    .resultat-day { font-size: 1.5rem; font-weight: 800; color: var(--primary); }
    .resultat-month { font-size: 0.8rem; text-transform: uppercase; color: var(--text-muted); }
    .resultat-info { flex: 1; }
    .resultat-info h3 { margin: 0 0 0.5rem 0; font-size: 1.1rem; }
    .resultat-info p { color: var(--text-muted); margin: 0 0 0.75rem 0; font-size: 0.9rem; }
    .resultat-meta { display: flex; gap: 0.5rem; }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
    .text-muted { color: var(--text-muted); font-size: 0.9rem; }
  `]
})
export class PlanificateurComponent {
  formations: Formation[] = [];
  selectedFormationIds: number[] = [];
  form = { dateDebut: '', dateFin: '' };
  loading = false;
  hasSearched = false;
  resultats: PlanificationResponse[] = [];

  constructor(
    public auth: AuthService,
    private formationService: FormationService,
    private planificationService: PlanificationService
  ) {
    this.formationService.getAll().subscribe({
      next: (data) => (this.formations = data),
      error: () => (this.formations = [])
    });
  }

  isSelected(formationId: number | undefined): boolean {
    return formationId !== undefined && this.selectedFormationIds.includes(formationId);
  }

  toggleFormation(formationId: number | undefined): void {
    if (formationId === undefined) return;
    const index = this.selectedFormationIds.indexOf(formationId);
    if (index > -1) {
      this.selectedFormationIds.splice(index, 1);
    } else {
      this.selectedFormationIds.push(formationId);
    }
  }

  generatePlan(): void {
    const apprenantId = this.auth.user()?.id;
    if (!apprenantId || !this.form.dateDebut || !this.form.dateFin) return;

    this.loading = true;
    this.hasSearched = true;
    const request: PlanificationRequest = {
      apprenantId,
      dateDebut: this.form.dateDebut,
      dateFin: this.form.dateFin,
      formationIds: this.selectedFormationIds.length > 0 ? this.selectedFormationIds : undefined
    };

    this.planificationService.generate(request).subscribe({
      next: (data) => {
        this.resultats = data;
        this.loading = false;
      },
      error: () => {
        this.resultats = [];
        this.loading = false;
      }
    });
  }
}