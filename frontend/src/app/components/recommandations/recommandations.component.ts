import { AuthService } from '../../services/auth.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApprenantService } from '../../services/apprenant.service';
import { RecommendationService } from '../../services/recommendation.service';
import { FormationService } from '../../services/formation.service';
import { Apprenant } from '../../models/apprenant.model';
import { Recommendation } from '../../models/recommendation.model';
import { Formation } from '../../models/formation.model';
import { Competence } from '../../models/competence.model';

@Component({
  selector: 'app-recommandations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="recommandations-container">
      <p *ngIf="errorMessage" role="alert">{{ errorMessage }}</p>
      <div class="page-header">
        <div>
          <h1 class="page-title">Vos recommandations</h1>
          <p class="page-subtitle">Des formations mises en relation avec vos objectifs et compétences.</p>
        </div>
      </div>

      <!-- Apprenant Selection Card -->
      <div class="card selection-card">
        <div class="selection-body">
          <div *ngIf="auth.isAdmin" style="flex: 1;">
            <label for="recommendation-learner" class="form-label">Sélectionner un apprenant pour l'analyse :</label>
            <select id="recommendation-learner" [disabled]="loading" class="form-control" [(ngModel)]="selectedApprenantId" (change)="onApprenantChange()">
              <option [value]="null" disabled>Choisir un apprenant...</option>
              <option *ngFor="let a of apprenants" [value]="a.id">
                {{ a.prenom }} {{ a.nom }} ({{ a.profil?.objectif || 'Profil non défini' }})
              </option>
            </select>
          </div>
          <button 
            class="btn btn-primary" 
            style="align-self: flex-end; padding: 0.8rem 1.6rem;"
            [disabled]="!selectedApprenantId || loading"
            (click)="fetchRecommendations()"
          >
            <span *ngIf="!loading">Découvrir mes recommandations</span>
            <span *ngIf="loading">Analyse en cours…</span>
          </button>
        </div>

        <!-- Learner Summary Preview -->
        <div *ngIf="currentApprenant" class="learner-summary">
          <div class="summary-col">
            <span class="sum-label">Objectif Vise :</span>
            <span class="sum-val">{{ currentApprenant.profil?.objectif || 'Non renseigné' }}</span>
          </div>
          <div class="summary-col">
            <span class="sum-label">Domaine d'intérêt :</span>
            <span class="sum-val">{{ currentApprenant.profil?.domaineInteret || 'Non renseigné' }}</span>
          </div>
          <div class="summary-col">
            <span class="sum-label">Compétences :</span>
            <div class="comps-tags">
              <span *ngFor="let c of currentCompetences" class="comp-chip">
                {{ c.nom }} ({{ c.niveau }}%)
              </span>
              <span *ngIf="currentCompetences.length === 0" style="color: var(--text-muted); font-size: 0.85rem;">
                Aucune compétence
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Recommendations Results -->
      <div *ngIf="hasSearched && !loading">
        <p class="method-note" *ngIf="recommendations.length">{{ recommendations[0].methode === 'MOTS_CLES' ? 'Mode de secours : comparaison par mots-clés. Le service ML est indisponible.' : 'Calcul par TF-IDF et similarité cosinus.' }} Le score mesure une correspondance textuelle, pas une probabilité de réussite.</p>
        <div class="results-header">
          <h2>Formations recommandées pour vous</h2>
          <span style="font-size: 0.9rem; color: var(--text-muted);">
            Classées de la plus pertinente à la moins pertinente
          </span>
        </div>

        <div class="recommendations-list" *ngIf="recommendations.length > 0">
          <div *ngFor="let r of recommendations; let i = index" class="card rec-card" [class.top-match]="i === 0 && r.score > 0">
            <div class="top-badge" *ngIf="i === 0 && r.score > 0">⭐ MEILLEURE RECOMMANDATION</div>
            
            <div class="rec-main">
              <div class="rec-score-circle" [style.border-color]="getScoreColor(r.score)">
                <span class="score-percent" [style.color]="getScoreColor(r.score)">
                  {{ formatScore(r.score) }}%
                </span>
                <span class="score-label">Pertinence</span>
              </div>

              <div class="rec-info">
                <div class="rec-tags">
                  <span class="badge badge-primary">{{ getFormationCategory(r.formationId) }}</span>
                  <span class="badge badge-secondary">{{ getFormationLevel(r.formationId) }}</span>
                </div>
                <h3 class="rec-title">{{ r.titre || getFormationTitle(r.formationId) }}</h3>
                <p class="rec-desc">{{ getFormationDesc(r.formationId) }}</p>
              </div>

              <div class="rec-action">
                <a [routerLink]="['/formations', r.formationId]" class="btn btn-secondary btn-sm">Voir la formation</a>
              </div>
            </div>
          </div>
        </div>

        <div *ngIf="recommendations.length === 0" class="empty-state">
          Aucune recommandation disponible.
        </div>
      </div>
    </div>
  `,
  styles: [`
    .method-note { font-size: 12px; color: var(--text-muted); background: #f0f5f3; padding: 14px; border-radius: 8px; margin-bottom: 24px; }
    @media (max-width: 600px) { .learner-summary { grid-template-columns: 1fr !important; } }

    .selection-card {
      margin-bottom: 2rem;
      background: #f1f7f3;
    }
    .selection-body {
      display: flex;
      gap: 1.5rem;
      align-items: center;
      flex-wrap: wrap;
    }
    .learner-summary {
      margin-top: 1.25rem;
      padding-top: 1.25rem;
      border-top: 1px solid var(--border-color);
      display: grid;
      grid-template-columns: 1fr 1fr 2fr;
      gap: 1rem;
    }
    @media (max-width: 800px) {
      .learner-summary { grid-template-columns: 1fr; }
    }
    .sum-label {
      font-size: 0.78rem;
      color: var(--text-dim);
      font-weight: 700;
      text-transform: uppercase;
      display: block;
      margin-bottom: 0.2rem;
    }
    .sum-val {
      font-weight: 700;
      color: var(--primary);
    }
    .comps-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }
    .comp-chip {
      background: var(--primary-light);
      color: var(--primary);
      padding: 0.25rem 0.6rem;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .results-header {
      margin-bottom: 1.5rem;
    }
    .recommendations-list {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }
    .rec-card {
      position: relative;
      transition: var(--transition);
    }
    .rec-card.top-match {
      border: 1px solid var(--primary);
      box-shadow: var(--shadow-glow);
    }
    .top-badge {
      position: absolute;
      top: -12px;
      right: 20px;
      background: linear-gradient(135deg, var(--primary), var(--accent));
      color: white;
      font-size: 0.72rem;
      font-weight: 800;
      padding: 0.2rem 0.75rem;
      border-radius: 12px;
      letter-spacing: 0.05em;
    }
    .rec-main {
      display: flex;
      align-items: center;
      gap: 1.75rem;
    }
    @media (max-width: 768px) {
      .rec-main { flex-direction: column; align-items: flex-start; }
    }
    .rec-score-circle {
      width: 84px;
      height: 84px;
      border-radius: 50%;
      border: 4px solid var(--primary);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #f4f8f5;
      flex-shrink: 0;
    }
    .score-percent {
      font-size: 1.4rem;
      font-weight: 900;
      line-height: 1;
    }
    .score-label {
      font-size: 0.65rem;
      color: var(--text-muted);
      text-transform: uppercase;
      margin-top: 2px;
    }
    .rec-info {
      flex: 1;
    }
    .rec-tags {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 0.4rem;
    }
    .rec-title {
      font-size: 1.3rem;
      font-weight: 800;
      margin-bottom: 0.3rem;
    }
    .rec-desc {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .rec-action {
      flex-shrink: 0;
    }
  `]
})
export class RecommandationsComponent implements OnInit {
  apprenants: Apprenant[] = [];
  allFormations: Formation[] = [];
  selectedApprenantId: number | null = null;
  currentApprenant: Apprenant | null = null;
  currentCompetences: Competence[] = [];

  recommendations: Recommendation[] = [];
  loading = false;
  hasSearched = false;

  constructor(
    public auth: AuthService,
    private route: ActivatedRoute,
    private apprenantService: ApprenantService,
    private recommendationService: RecommendationService,
    private formationService: FormationService
  ) {}

  ngOnInit(): void {
    this.loadFormations();
    if (!this.auth.isAdmin) {
      this.selectedApprenantId = this.auth.user()!.id;
      this.apprenantService.getById(Number(this.selectedApprenantId)).subscribe({
        next: user => { this.apprenants = [user]; this.onApprenantChange(); },
        error: () => this.errorMessage = 'Impossible de charger votre profil.'
      });
      return;
    }
    this.apprenantService.getAll().subscribe({
      next: data => {
        this.apprenants = data;
        const requested = Number(this.route.snapshot.queryParamMap.get('apprenantId'));
        this.selectedApprenantId = data.find(a => a.id === requested)?.id || data[0]?.id || null;
        this.onApprenantChange();
      },
      error: () => this.errorMessage = 'Impossible de charger les apprenants.'
    });
  }

  loadFormations(): void {
    this.formationService.getAll().subscribe({
      next: data => this.allFormations = data,
      error: () => this.errorMessage = 'Impossible de charger les formations.'
    });
  }

  errorMessage = '';

  onApprenantChange(): void {
    this.recommendations = [];
    this.hasSearched = false;
    this.currentCompetences = [];
    this.currentApprenant = null;
    if (!this.selectedApprenantId) return;
    this.currentApprenant = this.apprenants.find(a => a.id === Number(this.selectedApprenantId)) || null;
    const requestedId = Number(this.selectedApprenantId);
    this.apprenantService.getCompetences(requestedId).subscribe({
      next: (comps) => { if (requestedId === Number(this.selectedApprenantId)) this.currentCompetences = comps; },
      error: (err) => console.error(err)
    });
  }

  fetchRecommendations(): void {
    if (!this.selectedApprenantId || this.loading) return;
    this.loading = true;
    this.errorMessage = "";
    this.hasSearched = true;

    this.recommendationService.getRecommendationsForApprenant(Number(this.selectedApprenantId)).subscribe({
      next: (recs) => {
        this.recommendations = recs;
        this.loading = false;
      },
      error: () => {
        this.recommendations = [];
        this.errorMessage = 'Impossible de charger les recommandations. Réessayez.';
        this.loading = false;
      }
    });
  }

  formatScore(score: number): number {
    return Math.round(score > 1 ? score : score * 100);
  }

  getScoreColor(score: number): string {
    const val = this.formatScore(score);
    if (val >= 80) return 'var(--success)';
    if (val >= 50) return 'var(--primary)';
    return 'var(--text-muted)';
  }

  getFormationTitle(id: number): string {
    return this.allFormations.find(f => f.id === id)?.titre || 'Formation #' + id;
  }

  getFormationCategory(id: number): string {
    return this.allFormations.find(f => f.id === id)?.categorie?.nom || 'Sans catégorie';
  }

  getFormationLevel(id: number): string {
    return this.allFormations.find(f => f.id === id)?.niveau || 'Tous niveaux';
  }

  getFormationDesc(id: number): string {
    return this.allFormations.find(f => f.id === id)?.description || '';
  }
}
