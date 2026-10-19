import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { PlanificationIAService } from '../../services/planification-ia.service';
import { SeanceProbabiliteService } from '../../services/seance-probabilite.service';
import { FormationService } from '../../services/formation.service';
import { ApprenantService } from '../../services/apprenant.service';
import { PlanificationIAResponse, SeanceIAResponse, CreneauProbabiliteRequest, SeanceAvecProbabilite } from '../../models/planner.model';
import { Formation } from '../../models/formation.model';
import { Apprenant } from '../../models/apprenant.model';

@Component({
  selector: 'app-planification-ia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="planification-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Planification IA</h1>
          <p class="page-subtitle">Répartition intelligente des séances de révision par date d'examen, coefficient, faiblesses et probabilité de réalisation.</p>
        </div>
      </div>

      <div class="card">
        <div class="form-grid">
          <!-- Admin uniquement -->
          <div class="form-group" *ngIf="isAdmin">
            <label class="form-label">Apprenant *</label>
            <select class="form-select" [(ngModel)]="selectedApprenantId">
              <option [ngValue]="null">Sélectionner un apprenant...</option>
              <option *ngFor="let a of apprenants" [ngValue]="a.id">{{ a.prenom }} {{ a.nom }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Date de début</label>
            <input type="date" class="form-input" [(ngModel)]="dateDebut" />
          </div>
          <div class="form-group">
            <label class="form-label">Date de fin</label>
            <input type="date" class="form-input" [(ngModel)]="dateFin" />
          </div>
          <div class="form-group">
            <label class="form-label">Formations</label>
            <select class="form-select" [(ngModel)]="selectedFormationIds" multiple>
              <option *ngFor="let f of formations" [ngValue]="f.id">{{ f.titre }}</option>
            </select>
            <small class="form-hint">Laisser vide pour toutes les formations</small>
          </div>
          <div class="form-group">
            <button class="btn btn-primary" (click)="generer()" [disabled]="!canGenerate()">Générer le plan</button>
          </div>
        </div>
      </div>

      <div *ngIf="error" class="empty-state error">{{ error }}</div>
      <div *ngIf="loading" class="empty-state">Calcul du plan de révision par l'IA…</div>

      <div *ngIf="plan && plan.seances.length === 0 && !loading" class="empty-state">
        Aucune séance générée pour cette période (pas d'examen ou de faiblesse détectée).
      </div>

      <div *ngIf="plan && plan.seances.length > 0" class="results-card">
        <div class="results-summary">
          <span class="badge">{{ plan.totalSeances }}</span> séance(s) planifiée(s)
          <span class="badge info">{{ plan.periode }}</span>
        </div>
        <div class="seances-list">
          <div *ngFor="let s of seancesAvecProbabilite" class="seance-item" [class.high-priority]="(s.priorite ?? 0) >= 10">
            <div class="seance-date">{{ s.date }}</div>
            <div class="seance-time">{{ s.heureDebut }} - {{ s.heureFin }}</div>
            <div class="seance-content">
              <div class="seance-title">{{ s.titre }}</div>
              <div class="seance-description">{{ s.description }}</div>
              <div *ngIf="s.formationTitre" class="seance-formation">{{ s.formationTitre }}</div>
              <div class="seance-motif">{{ s.motif }}</div>
            </div>
            <div class="seance-priorite">
              <span class="priority-badge"
                    [class.high]="(s.priorite ?? 0) >= 10"
                    [class.medium]="(s.priorite ?? 0) >= 5 && (s.priorite ?? 0) < 10"
                    [class.low]="(s.priorite ?? 0) < 5">
                P{{ s.priorite ?? 0 }}
              </span>
            </div>
            <div class="seance-proba">
              <span class="proba-badge"
                    [class.keep]="(s.probabilite ?? 0) >= 0.5"
                    [class.drop]="(s.probabilite ?? 0) < 0.5">
                {{ (s.probabilite ?? 0) * 100 | number:'1.0-0' }}% {{ s.source || 'ML' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="!plan && !loading && !error && dateDebut && dateFin" class="empty-state">
        Cliquez sur "Générer le plan" pour calculer votre planification.
      </div>
    </div>
  `,
  styles: [`
    .planification-container { max-width: 1000px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .card { padding: 1.5rem; margin-bottom: 1.5rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; align-items: end; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-label { font-size: 0.85rem; font-weight: 600; color: var(--text); }
    .form-input, .form-select { padding: 0.6rem 0.8rem; border: 1px solid var(--border-color); border-radius: var(--radius); font-size: 0.9rem; background: var(--bg-input); color: var(--text); }
    .form-select option { background: var(--bg-card); }
    .form-hint { font-size: 0.75rem; color: var(--text-muted); }
    .results-card { border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); overflow: hidden; }
    .results-summary { display: flex; gap: 1rem; padding: 1rem 1.5rem; background: var(--primary-light); border-bottom: 1px solid var(--border-color); }
    .badge { padding: 0.25rem 0.75rem; border-radius: 20px; font-size: 0.85rem; font-weight: 600; color: var(--primary); }
    .badge.info { color: var(--text); background: var(--bg-muted); }
    .seances-list { display: flex; flex-direction: column; gap: 0.75rem; padding: 1rem 1.5rem; }
    .seance-item { display: flex; gap: 1rem; padding: 1rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); align-items: center; }
    .seance-item.high-priority { border-left: 4px solid var(--danger, #ef4444); }
    .seance-date { min-width: 100px; font-weight: 600; color: var(--primary); }
    .seance-time { min-width: 110px; font-size: 0.85rem; color: var(--text-muted); }
    .seance-content { flex: 1; }
    .seance-title { font-weight: 600; margin-bottom: 0.25rem; }
    .seance-description { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.25rem; }
    .seance-formation { font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 0.25rem; }
    .seance-motif { font-size: 0.8rem; font-style: italic; color: var(--text-muted); }
    .seance-priorite { display: flex; align-items: center; }
    .priority-badge { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.85rem; background: var(--bg-muted); color: var(--text-muted); }
    .priority-badge.high { background: #fee2e2; color: #dc2626; }
    .priority-badge.medium { background: #fef3c7; color: #d97706; }
    .priority-badge.low { background: #dbeafe; color: #2563eb; }
    .seance-proba { display: flex; align-items: center; }
    .proba-badge { padding: 0.25rem 0.75rem; border-radius: 20px; font-size: 0.8rem; font-weight: 700; white-space: nowrap; }
    .proba-badge.keep { background: #dcfce7; color: #166534; }
    .proba-badge.drop { background: #fef2f2; color: #991b1b; }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
    .empty-state.error { color: #dc2626; }
  `]
})
export class PlanificationIAComponent implements OnInit {
  dateDebut = '';
  dateFin = '';
  selectedFormationIds: number[] = [];
  selectedApprenantId: number | null = null;
  formations: Formation[] = [];
  apprenants: Apprenant[] = [];
  plan: PlanificationIAResponse | null = null;
  seancesAvecProbabilite: SeanceAvecProbabilite[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    public auth: AuthService,
    private planificationIAService: PlanificationIAService,
    private seanceProbabiliteService: SeanceProbabiliteService,
    private formationService: FormationService,
    private apprenantService: ApprenantService
  ) {}

  /** Fonctionne que auth.isAdmin soit un booléen, un signal ou une méthode. */
  get isAdmin(): boolean {
    const v: any = (this.auth as any).isAdmin;
    return typeof v === 'function' ? !!v.call(this.auth) : !!v;
  }

  ngOnInit(): void {
    const aujourdHui = new Date().toISOString().split('T')[0];
    const dansUnMois = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    this.dateDebut = aujourdHui;
    this.dateFin = dansUnMois;

    this.formationService.getAll().subscribe({
      next: (data) => this.formations = data,
      error: () => this.formations = []
    });

    if (this.isAdmin) {
      this.apprenantService.getAll().subscribe({
        next: (data) => this.apprenants = data,
        error: () => this.apprenants = []
      });
    }
  }

  /** USER : son propre ID. ADMIN : l'apprenant choisi, jamais son propre ID. */
  private targetApprenantId(): number | null {
    return this.isAdmin ? this.selectedApprenantId : (this.auth.user()?.id ?? null);
  }

  canGenerate(): boolean {
    return !!this.dateDebut && !!this.dateFin && !this.loading && this.targetApprenantId() != null;
  }

  generer(): void {
    const apprenantId = this.targetApprenantId();
    if (apprenantId == null || !this.dateDebut || !this.dateFin) return;

    this.loading = true;
    this.error = null;
    this.plan = null;
    this.seancesAvecProbabilite = [];

    const formationIds = this.selectedFormationIds.length > 0 ? this.selectedFormationIds : undefined;

    this.planificationIAService.genererPlanification(apprenantId, this.dateDebut, this.dateFin, formationIds).subscribe({
      next: (data) => {
        this.plan = data;
        this.loading = false;
        // Affichage immédiat, les probabilités arrivent ensuite
        this.seancesAvecProbabilite = data.seances.map(s => ({ ...s }));
        this.calculerProbabilites(apprenantId, data.seances);
      },
      error: (err) => {
        this.plan = null;
        this.loading = false;
        this.error = err?.status === 403
          ? "Accès refusé : vous ne pouvez pas générer un plan pour cet apprenant."
          : "Erreur lors de la génération du plan.";
      }
    });
  }

  private toMinutes(hhmm: string): number {
    const [h, m] = (hhmm || '').split(':');
    return (parseInt(h, 10) || 0) * 60 + (parseInt(m, 10) || 0);
  }

  private calculerProbabilites(apprenantId: number, seances: SeanceIAResponse[]): void {
    // Compte des séances par jour, pour seancesMemeJour
    const parJour = new Map<string, number>();

    const creneaux: CreneauProbabiliteRequest[] = seances.map((s) => {
      const debut = this.toMinutes(s.heureDebut);
      const fin = this.toMinutes(s.heureFin);
      const dureeMin = fin > debut ? fin - debut : 60;

      const dateObj = new Date(s.date);
      const jourSemaine = dateObj.getDay() === 0 ? 6 : dateObj.getDay() - 1; // lundi = 0

      // Jours entre la séance et la fin de période (approximation de l'échéance)
      const joursAvantExamen = Math.max(
        1,
        Math.round((new Date(this.dateFin).getTime() - dateObj.getTime()) / 86400000)
      );

      const deja = parJour.get(s.date) ?? 0;
      parJour.set(s.date, deja + 1);

      return {
        heure: Math.floor(debut / 60),
        jourSemaine,
        dureeMin,
        joursAvantExamen,
        seancesMemeJour: deja
      };
    });

    this.seanceProbabiliteService.calculerProbabilites(apprenantId, creneaux).subscribe({
      next: (probabilites) => {
        this.seancesAvecProbabilite = seances.map((s, i) => ({
          ...s,
          probabilite: probabilites[i]?.probabilite ?? 0.5,
          source: probabilites[i]?.source ?? 'ML',
          heure: creneaux[i]?.heure,
          dureeMin: creneaux[i]?.dureeMin
        }));
      },
      error: () => {
        this.seancesAvecProbabilite = seances.map((s) => ({
          ...s,
          probabilite: 0.5,
          source: 'REGLES'
        }));
      }
    });
  }
}