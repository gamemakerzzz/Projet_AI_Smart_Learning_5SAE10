import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ChapitreRevoirService } from '../../services/chapitre-revoir.service';
import { ChapitreRevoirResponse } from '../../models/planner.model';

@Component({
  selector: 'app-chapitres-revoir',
  standalone: true,
  imports: [],
  template: `
    <div class="chapitres-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Chapitres à revoir</h1>
          <p class="page-subtitle">Chapitres nécessitant des révisions basés sur vos compétences.</p>
        </div>
      </div>

      <div class="card">
        <div *ngIf="chapitres.length === 0 && !loading" class="empty-state">
          Aucun chapitre à revoir détecté. Continuez vos efforts !
        </div>
        <div *ngIf="loading" class="empty-state">Analyse en cours...</div>
        <div *ngIf="chapitres.length > 0" class="chapitres-list">
          <div *ngFor="let c of chapitres" class="chapitre-card" [class.high-priority]="c.priorite && c.priorite >= 3">
            <div class="chapitre-priorite">
              <span class="priority-badge" [class.high]="c.priorite && c.priorite >= 3" [class.medium]="c.priorite && c.priorite === 1" [class.low]="c.priorite && c.priorite === 0">
                {{ c.priorite || 0 }}
              </span>
            </div>
            <div class="chapitre-info">
              <h3>{{ c.titre }}</h3>
              <p class="formation-name">{{ c.formationTitre }}</p>
              <p class="raison">{{ c.raison }}</p>
              <span class="badge badge-secondary">Ordre {{ c.ordre }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chapitres-container { max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 2rem; }
    .page-title { font-size: 1.8rem; font-weight: 800; }
    .page-subtitle { color: var(--text-muted); margin-top: 0.25rem; }
    .chapitres-list { display: flex; flex-direction: column; gap: 1rem; }
    .chapitre-card { display: flex; gap: 1rem; padding: 1.25rem; border: 1px solid var(--border-color); border-radius: var(--radius); background: var(--bg-card); border-left: 4px solid var(--text-muted); }
    .chapitre-card.high-priority { border-left-color: var(--danger, #ef4444); }
    .chapitre-priorite { display: flex; align-items: center; }
    .priority-badge { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; background: var(--bg-muted); color: var(--text-muted); }
    .priority-badge.high { background: #fee2e2; color: #dc2626; }
    .priority-badge.medium { background: #fef3c7; color: #d97706; }
    .priority-badge.low { background: #dbeafe; color: #2563eb; }
    .chapitre-info { flex: 1; }
    .chapitre-info h3 { margin: 0 0 0.25rem 0; font-size: 1.05rem; }
    .formation-name { color: var(--text-muted); font-size: 0.85rem; margin: 0 0 0.5rem 0; }
    .raison { font-size: 0.9rem; margin: 0 0 0.5rem 0; }
    .empty-state { text-align: center; padding: 3rem; color: var(--text-muted); }
  `]
})
export class ChapitresRevoirComponent implements OnInit {
  chapitres: ChapitreRevoirResponse[] = [];
  loading = false;

  constructor(
    public auth: AuthService,
    private chapitreRevoirService: ChapitreRevoirService
  ) {}

  ngOnInit(): void {
    const apprenantId = this.auth.user()?.id;
    if (apprenantId) {
      this.loading = true;
      this.chapitreRevoirService.detectChapitresARevoir(apprenantId).subscribe({
        next: (data) => { this.chapitres = data; this.loading = false; },
        error: () => { this.chapitres = []; this.loading = false; }
      });
    }
  }
}