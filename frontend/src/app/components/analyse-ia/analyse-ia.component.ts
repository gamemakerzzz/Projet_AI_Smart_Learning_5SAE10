import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AnalyseIAService } from '../../services/analyse-ia.service';
import { AnalyseIAResponse } from '../../models/planner.model';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-analyse-ia',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="analyse-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Analyse IA</h1>
          <p class="page-subtitle">Analyse de votre profil d'apprentissage et recommandations personnalisées.</p>
        </div>
      </div>

      <div *ngIf="analyse" class="analyse-grid">
        <div class="stat-card niveau">
          <div class="stat-value">{{ analyse.niveauGlobal }}</div>
          <div class="stat-label">Niveau global</div>
        </div>
        <div class="stat-card score">
          <div class="stat-value">{{ analyse.scoreProgression }}%</div>
          <div class="stat-label">Score de progression</div>
        </div>
        <div class="stat-card style">
          <div class="stat-value">{{ analyse.styleApprentissage }}</div>
          <div class="stat-label">Style d'apprentissage</div>
        </div>
      </div>

      <div *ngIf="analyse && analyse.forces && analyse.forces.length > 0" class="card">
        <h2><app-icon name="spark" /> Vos forces</h2>
        <ul class="list-forces">
          <li *ngFor="let f of analyse.forces">{{ f }}</li>
        </ul>
      </div>

      <div *ngIf="analyse && analyse.faiblesses && analyse.faiblesses.length > 0" class="card">
        <h2><app-icon name="close" /> À améliorer</h2>
        <ul class="list-faiblesses">
          <li *ngFor="let f of analyse.faiblesses">{{ f }}</li>
        </ul>
      </div>

      <div *ngIf="analyse && analyse.recommandationsPersonnalisees && analyse.recommandationsPersonnalisees.length > 0" class="card">
        <h2><app-icon name="arrow" /> Recommandations</h2>
        <ul class="list-recommandations">
          <li *ngFor="let r of analyse.recommandationsPersonnalisees">{{ r }}</li>
        </ul>
      </div>

      <div *ngIf="analyse && analyse.competencesEvolution && competencesEntries.length > 0" class="card">
        <h2>Évolution des compétences</h2>
        <div class="comp-evolution">
          <div *ngFor="let item of competencesEntries" class="comp-item">
            <span class="comp-name">{{ item.key }}</span>
            <div class="bar-track"><div class="bar-fill" [style.width.%]="item.value"></div></div>
            <span class="comp-value">{{ item.value }}%</span>
          </div>
        </div>
      </div>

      <div *ngIf="!analyse && !loading" class="empty-state">
        Aucune analyse disponible. Complétez votre profil et vos compétences.
      </div>
      <div *ngIf="loading" class="empty-state">Analyse en cours...</div>
    </div>
  `,
  styles: [`
    .analyse-container { max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .analyse-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
    .stat-card { padding: 1.5rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); text-align: center; }
    .stat-card.niveau { border-top: 4px solid var(--primary); }
    .stat-card.score { border-top: 4px solid var(--success, #10b981); }
    .stat-card.style { border-top: 4px solid var(--warning, #f59e0b); }
    .stat-value { font-size: 2rem; font-weight: 800; word-break: break-word; }
    .stat-label { font-size: 0.85rem; color: var(--text-muted); margin-top: 0.25rem; }
    .card { padding: 1.5rem; margin-bottom: 1.5rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); }
    .card h2 { margin: 0 0 1rem 0; font-size: 1.2rem; display: flex; align-items: center; gap: 0.5rem; }
    .list-forces, .list-faiblesses, .list-recommandations { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.5rem; }
    .list-forces li { background: #dcfce7; color: #166534; padding: 0.75rem 1rem; border-radius: 8px; }
    .list-faiblesses li { background: #fef2f2; color: #991b1b; padding: 0.75rem 1rem; border-radius: 8px; }
    .list-recommandations li { background: #eff6ff; color: #1e40af; padding: 0.75rem 1rem; border-radius: 8px; border-left: 4px solid var(--primary); }
    .comp-evolution { display: flex; flex-direction: column; gap: 0.75rem; }
    .comp-item { display: flex; align-items: center; gap: 1rem; }
    .comp-name { min-width: 140px; font-size: 0.9rem; }
    .bar-track { flex: 1; height: 20px; background: var(--bg-muted); border-radius: 10px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 10px; background: var(--primary); transition: width 0.4s ease; }
    .comp-value { min-width: 40px; text-align: right; font-weight: 600; font-size: 0.85rem; }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
  `]
})
export class AnalyseIAComponent implements OnInit {
  analyse: AnalyseIAResponse | null = null;
  loading = false;

  constructor(
    public auth: AuthService,
    private analyseIAService: AnalyseIAService
  ) {}

  get competencesEntries(): { key: string; value: number }[] {
    if (!this.analyse?.competencesEvolution) return [];
    return Object.entries(this.analyse.competencesEvolution)
      .map(([key, value]) => ({ key, value }));
  }

  ngOnInit(): void {
    const apprenantId = this.auth.user()?.id;
    if (apprenantId) {
      this.loading = true;
      this.analyseIAService.analyserApprenant(apprenantId).subscribe({
        next: (data) => { this.analyse = data; this.loading = false; },
        error: () => { this.analyse = null; this.loading = false; }
      });
    }
  }
}
