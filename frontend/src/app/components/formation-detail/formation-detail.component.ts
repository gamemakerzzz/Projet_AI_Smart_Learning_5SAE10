import { inject } from '@angular/core';
import { UiService } from '../../services/ui.service';
import { ModalA11yDirective } from '../../shared/modal-a11y.directive';
import { AuthService } from '../../services/auth.service';
import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { Formation } from '../../models/formation.model';
import { Chapitre } from '../../models/chapitre.model';

@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [ModalA11yDirective, CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="formation-detail-container" *ngIf="formation">
      <div style="margin-bottom: 1.5rem;">
        <a routerLink="/formations" class="btn btn-secondary btn-sm">Retour au catalogue</a>
      </div>

      <!-- Formation Banner -->
      <div class="card formation-banner">
        <div class="banner-top">
          <span class="badge badge-primary">{{ formation.categorie?.nom }}</span>
          <span class="badge badge-secondary">{{ formation.niveau }}</span>
          <span style="margin-left: auto; font-size: 1.3rem; font-weight: 800; color: var(--success);">
            {{ formation.prix }} €
          </span>
        </div>
        <h1 style="font-size: 2rem; font-weight: 800; margin: 0.75rem 0;">{{ formation.titre }}</h1>
        <p style="color: var(--text-muted); font-size: 1rem; max-width: 800px;">{{ formation.description }}</p>
        <div *ngIf="formation.motsCles" style="margin-top: 1rem;">
          <span style="font-size: 0.85rem; color: var(--text-dim); margin-right: 0.5rem;">Mots-clés :</span>
          <span class="mots-tag">{{ formation.motsCles }}</span>
        </div>
      </div>

      <!-- Chapitres Header -->
      <div class="page-header" style="margin-top: 2.5rem; margin-bottom: 1.5rem;">
        <div>
          <h2 style="font-size: 1.5rem; font-weight: 700;">Chapitres du cours ({{ chapitres.length }})</h2>
          <p class="page-subtitle">Programme et leçons incluses dans la formation</p>
        </div>
        <button *ngIf="auth.isAdmin" class="btn btn-primary" (click)="openChapitreModal()">+ Ajouter un Chapitre</button>
      </div>

      <!-- Chapitres Accordion/List -->
      <div class="chapitres-list">
        <div *ngFor="let c of chapitres" class="card chapitre-card">
          <div class="chapitre-header">
            <div style="display: flex; align-items: center; gap: 0.75rem;">
              <span class="chapitre-number">#{{ c.ordre }}</span>
              <h3 class="chapitre-title">{{ c.titre }}</h3>
            </div>
            <div style="display: flex; gap: 0.4rem;">
              <button *ngIf="auth.isAdmin" class="btn btn-secondary btn-sm" (click)="openChapitreModal(c)">Modifier</button>
              <button *ngIf="auth.isAdmin" class="btn btn-danger btn-sm" (click)="deleteChapitre(c.id!)">Supprimer</button>
            </div>
          </div>
          <p class="chapitre-contenu">{{ c.contenu || 'Aucun détail de contenu disponible.' }}</p>
        </div>

        <div *ngIf="chapitres.length === 0" class="empty-state">
          Aucun chapitre n'a encore été ajouté à cette formation.
        </div>
      </div>

      <!-- Modal Add/Edit Chapitre -->
      <div class="modal-backdrop" *ngIf="showModal">
        <div class="modal-content" appModal (dismiss)="closeChapitreModal()">
          <div class="modal-header">
            <h2 class="modal-title">{{ editMode ? 'Modifier le Chapitre' : 'Nouveau Chapitre' }}</h2>
            <button aria-label="Fermer la fenêtre" class="close-btn" (click)="closeChapitreModal()">&times;</button>
          </div>

          <form [formGroup]="chapitreForm" (ngSubmit)="saveChapitre()">
            <div class="form-group">
              <label class="form-label" for="formation-detail-titre">Titre du chapitre *</label>
              <input id="formation-detail-titre" type="text" formControlName="titre" class="form-control" placeholder="ex: Introduction à Spring Boot">
            </div>

            <div class="form-group">
              <label class="form-label" for="formation-detail-ordre">Ordre d'affichage *</label>
              <input id="formation-detail-ordre" type="number" formControlName="ordre" class="form-control" placeholder="1">
            </div>

            <div class="form-group">
              <label class="form-label" for="formation-detail-contenu">Contenu du chapitre</label>
              <textarea id="formation-detail-contenu" formControlName="contenu" class="form-control" rows="4" placeholder="Description détaillée du cours..."></textarea>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-secondary" (click)="closeChapitreModal()">Annuler</button>
              <button type="submit" class="btn btn-primary" [disabled]="chapitreForm.invalid || ui.saving() > 0">Enregistrer</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .formation-banner {
      background: linear-gradient(135deg, rgba(30, 41, 59, 0.9), rgba(19, 27, 46, 0.9));
      border: 1px solid var(--border-color);
      box-shadow: var(--shadow-lg);
    }
    .banner-top {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .mots-tag {
      font-size: 0.85rem;
      color: var(--accent);
      background: var(--accent-light);
      padding: 0.25rem 0.6rem;
      border-radius: 6px;
      font-family: monospace;
    }
    .chapitres-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .chapitre-card {
      transition: var(--transition);
    }
    .chapitre-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }
    .chapitre-number {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: var(--primary-light);
      color: var(--primary);
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
    }
    .chapitre-title {
      font-size: 1.1rem;
      font-weight: 700;
    }
    .chapitre-contenu {
      color: var(--text-muted);
      font-size: 0.9rem;
      padding-left: 2.6rem;
    }
    .empty-state {
      text-align: center;
      padding: 2.5rem;
      color: var(--text-muted);
      background: var(--bg-card);
      border-radius: var(--radius-md);
      border: 1px dashed var(--border-color);
    }
  `]
})
export class FormationDetailComponent implements OnInit {
  readonly ui = inject(UiService);
  formation: Formation | null = null;
  chapitres: Chapitre[] = [];
  formationId!: number;

  showModal = false;
  editMode = false;
  selectedChapitreId: number | null = null;
  chapitreForm: FormGroup;

  constructor(
    public auth: AuthService,
    private route: ActivatedRoute,
    private formationService: FormationService,
    private fb: FormBuilder
  ) {
    this.chapitreForm = this.fb.group({
      titre: ['', Validators.required],
      ordre: [1, [Validators.required, Validators.min(1)]],
      contenu: ['']
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.formationId = Number(params['id']);
        this.loadFormation();
        this.loadChapitres();
      }
    });
  }

  loadFormation(): void {
    this.formationService.getById(this.formationId).subscribe({
      next: (data) => this.formation = data,
      error: (err) => console.error(err)
    });
  }

  loadChapitres(): void {
    this.formationService.getChapitres(this.formationId).subscribe({
      next: (data) => this.chapitres = data,
      error: (err) => console.error(err)
    });
  }

  openChapitreModal(chapitre?: Chapitre): void {
    if (chapitre) {
      this.editMode = true;
      this.selectedChapitreId = chapitre.id!;
      this.chapitreForm.patchValue({
        titre: chapitre.titre,
        ordre: chapitre.ordre,
        contenu: chapitre.contenu || ''
      });
    } else {
      this.editMode = false;
      this.selectedChapitreId = null;
      this.chapitreForm.reset({
        ordre: this.chapitres.length + 1
      });
    }
    this.showModal = true;
  }

  closeChapitreModal(): void {
    this.showModal = false;
  }

  saveChapitre(): void {
    if (this.chapitreForm.invalid || this.ui.saving() > 0) { this.chapitreForm.markAllAsTouched(); return; }

    const payload: Chapitre = this.chapitreForm.value;

    if (this.editMode && this.selectedChapitreId) {
      this.formationService.updateChapitre(this.selectedChapitreId, payload).subscribe({
        next: () => {
          this.loadChapitres();
          this.closeChapitreModal();
        },
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    } else {
      this.formationService.addChapitre(this.formationId, payload).subscribe({
        next: () => {
          this.loadChapitres();
          this.closeChapitreModal();
        },
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }

  async deleteChapitre(chapitreId: number): Promise<void> {
    if (await this.ui.confirm('Voulez-vous supprimer ce chapitre ?')) {
      this.formationService.deleteChapitre(chapitreId).subscribe({
        next: () => this.loadChapitres(),
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }
}
