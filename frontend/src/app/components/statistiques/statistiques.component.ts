import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { StatistiquesService } from '../../services/statistiques.service';
import { StatistiquesResponse } from '../../models/planner.model';

@Component({
  selector: 'app-statistiques',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="statistiques-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Statistiques</h1>
          <p class="page-subtitle">Votre progression et indicateurs d'apprentissage.</p>
        </div>
      </div>

      <div *ngIf="stats" class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalExamens }}</div>
          <div class="stat-label">Examens</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalSeances }}</div>
          <div class="stat-label">Séances</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalNotifications }}</div>
          <div class="stat-label">Notifications</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalChapitres }}</div>
          <div class="stat-label">Chapitres</div>
        </div>
        <div class="stat-card highlight">
          <div class="stat-value">{{ stats.chapitresARevoir }}</div>
          <div class="stat-label">À revoir</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.averageCompetenceLevel | number:'1.0-0' }}%</div>
          <div class="stat-label">Niveau moyen</div>
        </div>
      </div>

      <div *ngIf="stats && stats.competencesByLevel" class="card">
        <h2>Répartition des compétences</h2>
        <div class="comp-levels">
          <div class="level-bar low">
            <span class="level-label">Faible (&lt;50%)</span>
            <div class="bar-track"><div class="bar-fill" [style.width.%]="(stats.competencesByLevel['faible'] || 0) * 10"></div></div>
            <span class="level-count">{{ stats.competencesByLevel['faible'] || 0 }}</span>
          </div>
          <div class="level-bar medium">
            <span class="level-label">Moyen (50-75%)</span>
            <div class="bar-track"><div class="bar-fill" [style.width.%]="(stats.competencesByLevel['moyen'] || 0) * 10"></div></div>
            <span class="level-count">{{ stats.competencesByLevel['moyen'] || 0 }}</span>
          </div>
          <div class="level-bar good">
            <span class="level-label">Bon (75-90%)</span>
            <div class="bar-track"><div class="bar-fill" [style.width.%]="(stats.competencesByLevel['bon'] || 0) * 10"></div></div>
            <span class="level-count">{{ stats.competencesByLevel['bon'] || 0 }}</span>
          </div>
          <div class="level-bar advanced">
            <span class="level-label">Avancé (&gt;90%)</span>
            <div class="bar-track"><div class="bar-fill" [style.width.%]="(stats.competencesByLevel['avance'] || 0) * 10"></div></div>
            <span class="level-count">{{ stats.competencesByLevel['avance'] || 0 }}</span>
          </div>
        </div>
      </div>

      <div *ngIf="stats && stats.formationProgress && stats.formationProgress.length > 0" class="card">
        <h2>Progression par formation</h2>
        <div class="formation-progress-list">
          <div *ngFor="let fp of stats.formationProgress" class="formation-progress-item">
            <div class="fp-info">
              <span class="fp-title">{{ fp.titre }}</span>
              <span class="fp-detail">{{ fp.chapitresFaibles }} / {{ fp.totalChapitres }} chapitres à revoir</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill danger" [style.width.%]="fp.totalChapitres > 0 ? (fp.chapitresFaibles / fp.totalChapitres) * 100 : 0"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .statistiques-container { max-width: 1100px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
    .stat-card { padding: 1.5rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); text-align: center; }
    .stat-card.highlight { border-color: var(--primary); background: var(--primary-light); }
    .stat-value { font-size: 2rem; font-weight: 800; }
    .stat-label { font-size: 0.85rem; color: var(--text-muted); margin-top: 0.25rem; }
    .card { padding: 1.5rem; margin-bottom: 1.5rem; }
    .card h2 { margin: 0 0 1rem 0; font-size: 1.2rem; }
    .comp-levels { display: flex; flex-direction: column; gap: 1rem; }
    .level-bar { display: flex; align-items: center; gap: 1rem; }
    .level-label { min-width: 160px; font-size: 0.9rem; }
    .bar-track { flex: 1; height: 24px; background: var(--bg-muted); border-radius: 12px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 12px; transition: width 0.4s ease; }
    .level-bar.low .bar-fill { background: #ef4444; }
    .level-bar.medium .bar-fill { background: #f59e0b; }
    .level-bar.good .bar-fill { background: #10b981; }
    .level-bar.advanced .bar-fill { background: #6366f1; }
    .level-count { min-width: 30px; text-align: right; font-weight: 600; }
    .formation-progress-list { display: flex; flex-direction: column; gap: 1rem; }
    .formation-progress-item { display: flex; flex-direction: column; gap: 0.5rem; }
    .fp-info { display: flex; justify-content: space-between; }
    .fp-title { font-weight: 600; }
    .fp-detail { font-size: 0.85rem; color: var(--text-muted); }
    .bar-fill.danger { background: #ef4444; }
  `]
})
export class StatistiquesComponent implements OnInit {
  stats: StatistiquesResponse | null = null;

  constructor(
    public auth: AuthService,
    private statistiquesService: StatistiquesService
  ) {}

  ngOnInit(): void {
    const apprenantId = this.auth.user()?.id;
    if (apprenantId) {
      this.statistiquesService.getStatistiques(apprenantId).subscribe({
        next: (data) => (this.stats = data),
        error: () => (this.stats = null)
      });
    }
  }
}