import { inject } from '@angular/core';
import { UiService } from '../../services/ui.service';
import { ModalA11yDirective } from '../../shared/modal-a11y.directive';
import { AuthService } from '../../services/auth.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ApprenantService } from '../../services/apprenant.service';
import { Apprenant } from '../../models/apprenant.model';
import { Profil } from '../../models/profil.model';
import { Competence } from '../../models/competence.model';

@Component({
  selector: 'app-apprenant-profil',
  standalone: true,
  imports: [ModalA11yDirective, CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="profil-container" *ngIf="apprenant">
      <div style="margin-bottom: 1.5rem;">
        <a *ngIf="auth.isAdmin" routerLink="/apprenants" class="btn btn-secondary btn-sm">Retour aux apprenants</a>
      </div>

      <!-- Apprenant Info Header Card -->
      <div class="card header-card">
        <div class="user-info-wrapper">
          <div class="large-avatar">{{ apprenant.prenom.charAt(0) }}{{ apprenant.nom.charAt(0) }}</div>
          <div>
            <h1 class="user-fullname">{{ apprenant.prenom }} {{ apprenant.nom }}</h1>
            <p class="user-email">✉️ {{ apprenant.email }}</p>
          </div>
          <div style="margin-left: auto;">
            <a [routerLink]="['/recommandations']" [queryParams]="{ apprenantId: apprenant.id }" class="btn btn-primary">
              Recommandations MLA
            </a>
          </div>
        </div>
      </div>

      <div class="profil-sections-grid">
        <!-- Section Profil -->
        <div class="card">
          <div class="section-title-bar">
            <h2>Profil d'Apprentissage</h2>
            <button class="btn btn-secondary btn-sm" (click)="toggleProfilEdit()">
              {{ editingProfil ? 'Annuler' : '✏️ Modifier Profil' }}
            </button>
          </div>

          <!-- View Mode -->
          <div *ngIf="!editingProfil" class="profil-details">
            <div class="detail-row">
              <span class="detail-label">Niveau global :</span>
              <span class="badge badge-secondary">{{ profil?.niveau || 'Non défini' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Objectif professionnel :</span>
              <span class="detail-value">{{ profil?.objectif || 'Non renseigné' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Domaine d'intérêt :</span>
              <span class="detail-value">{{ profil?.domaineInteret || 'Non renseigné' }}</span>
            </div>
          </div>

          <!-- Edit Mode -->
          <form *ngIf="editingProfil" [formGroup]="profilForm" (ngSubmit)="saveProfil()" style="margin-top: 1rem;">
            <div class="form-group">
              <label class="form-label" for="apprenant-profil-niveau">Niveau d'expérience</label>
              <select id="apprenant-profil-niveau" formControlName="niveau" class="form-control">
                <option value="Débutant">Débutant</option>
                <option value="Intermédiaire">Intermédiaire</option>
                <option value="Avancé">Avancé</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="apprenant-profil-objectif">Objectif professionnel</label>
              <input id="apprenant-profil-objectif" type="text" formControlName="objectif" class="form-control" placeholder="ex: Développeur Backend">
            </div>

            <div class="form-group">
              <label class="form-label" for="apprenant-profil-domaineInteret">Domaine d'intérêt</label>
              <input id="apprenant-profil-domaineInteret" type="text" formControlName="domaineInteret" class="form-control" placeholder="ex: Développement Web">
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
              <button type="submit" class="btn btn-primary btn-sm" [disabled]="ui.saving() > 0">Sauvegarder le profil</button>
            </div>
          </form>
        </div>

        <!-- Section Compétences -->
        <div class="card">
          <div class="section-title-bar">
            <h2>Compétences ({{ competences.length }})</h2>
            <button class="btn btn-primary btn-sm" (click)="openCompetenceModal()">+ Ajouter</button>
          </div>

          <div class="competences-list">
            <div *ngFor="let c of competences" class="competence-item">
              <div class="comp-header">
                <span class="comp-name">{{ c.nom }}</span>
                <span class="comp-level">{{ c.niveau }} %</span>
                <div class="comp-actions">
                  <button class="btn-icon" (click)="openCompetenceModal(c)" aria-label="Modifier">Modifier</button>
                  <button class="btn-icon danger" (click)="deleteCompetence(c.id!)" aria-label="Supprimer">Supprimer</button>
                </div>
              </div>
              <div class="progress-bar-bg">
                <div class="progress-bar-fill" [style.width.%]="c.niveau"></div>
              </div>
            </div>

            <div *ngIf="competences.length === 0" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">
              Aucune compétence ajoutée pour le moment.
            </div>
          </div>
        </div>
      </div>

      <!-- Modal Add/Edit Competence -->
      <div class="modal-backdrop" *ngIf="showCompModal">
        <div class="modal-content" appModal (dismiss)="closeCompetenceModal()">
          <div class="modal-header">
            <h2 class="modal-title">{{ editCompMode ? 'Modifier la Compétence' : 'Ajouter une Compétence' }}</h2>
            <button aria-label="Fermer la fenêtre" class="close-btn" (click)="closeCompetenceModal()">&times;</button>
          </div>

          <form [formGroup]="competenceForm" (ngSubmit)="saveCompetence()">
            <div class="form-group">
              <label class="form-label" for="apprenant-profil-nom">Nom de la compétence *</label>
              <input id="apprenant-profil-nom" type="text" formControlName="nom" class="form-control" placeholder="ex: Java, Spring Boot, SQL...">
            </div>

            <div class="form-group">
              <label class="form-label" for="apprenant-profil-niveau">Niveau de maîtrise (0 à 100 %) *</label>
              <input id="apprenant-profil-niveau" type="number" min="0" max="100" formControlName="niveau" class="form-control" placeholder="80">
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-secondary" (click)="closeCompetenceModal()">Annuler</button>
              <button type="submit" class="btn btn-primary" [disabled]="competenceForm.invalid || ui.saving() > 0">Enregistrer</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .header-card {
      margin-bottom: 2rem;
      background: #f1f7f3;
    }
    .user-info-wrapper {
      display: flex;
      align-items: center;
      gap: 1.5rem;
    }
    .large-avatar {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--primary), var(--accent));
      color: white;
      font-size: 1.5rem;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: var(--shadow-glow);
    }
    .user-fullname {
      font-size: 1.6rem;
      font-weight: 800;
    }
    .user-email {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .profil-sections-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }
    @media (max-width: 900px) {
      .profil-sections-grid { grid-template-columns: 1fr; }
    }
    .section-title-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 0.75rem;
    }
    .section-title-bar h2 {
      font-size: 1.15rem;
      font-weight: 700;
    }
    .profil-details {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin-top: 0.5rem;
    }
    .detail-row {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .detail-label {
      font-size: 0.82rem;
      color: var(--text-dim);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .detail-value {
      font-size: 1rem;
      font-weight: 600;
      color: var(--text-main);
    }
    .competences-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .competence-item {
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      padding: 0.85rem;
      border-radius: var(--radius-sm);
    }
    .comp-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.4rem;
    }
    .comp-name {
      font-weight: 700;
      font-size: 0.95rem;
    }
    .comp-level {
      font-size: 0.85rem;
      font-weight: 700;
      color: var(--primary);
    }
    .comp-actions {
      display: flex;
      gap: 0.3rem;
      margin-left: 0.5rem;
    }
    .btn-icon {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 0.85rem;
      opacity: 0.7;
    }
    .btn-icon:hover { opacity: 1; }
    .progress-bar-bg {
      width: 100%;
      height: 8px;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 4px;
      overflow: hidden;
    }
    .progress-bar-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--primary), var(--secondary));
      border-radius: 4px;
      transition: width 0.4s ease;
    }
  `]
})
export class ApprenantProfilComponent implements OnInit {
  readonly ui = inject(UiService);
  apprenant: Apprenant | null = null;
  profil: Profil | null = null;
  competences: Competence[] = [];
  apprenantId!: number;

  editingProfil = false;
  profilForm: FormGroup;

  showCompModal = false;
  editCompMode = false;
  selectedCompId: number | null = null;
  competenceForm: FormGroup;

  constructor(
    public auth: AuthService,
    private route: ActivatedRoute,
    private apprenantService: ApprenantService,
    private fb: FormBuilder
  ) {
    this.profilForm = this.fb.group({
      niveau: ['Intermédiaire'],
      objectif: [''],
      domaineInteret: ['']
    });

    this.competenceForm = this.fb.group({
      nom: ['', Validators.required],
      niveau: [80, [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.apprenantId = Number(params['id']);
        this.loadApprenantData();
      }
    });
  }

  loadApprenantData(): void {
    const requestedId = this.apprenantId;
    this.apprenant = null;
    this.profil = null;
    this.competences = [];
    this.editingProfil = false;
    this.showCompModal = false;
    this.profilForm.reset({ niveau: 'Intermédiaire', objectif: '', domaineInteret: '' });
    this.apprenantService.getById(this.apprenantId).subscribe({
      next: (data) => {
        if (requestedId !== this.apprenantId) return;
        this.apprenant = data;
        this.profil = data.profil || null;
        if (this.profil) {
          this.profilForm.patchValue({
            niveau: this.profil.niveau || 'Intermédiaire',
            objectif: this.profil.objectif || '',
            domaineInteret: this.profil.domaineInteret || ''
          });
        }
      },
      error: (err) => console.error(err)
    });

    this.apprenantService.getCompetences(this.apprenantId).subscribe({
      next: (comps) => { if (requestedId === this.apprenantId) this.competences = comps; },
      error: (err) => console.error(err)
    });
  }

  toggleProfilEdit(): void {
    this.editingProfil = !this.editingProfil;
  }

  saveProfil(): void {
    if (this.ui.saving() > 0 || this.profilForm.invalid) return;
    const payload: Profil = this.profilForm.value;
    this.apprenantService.upsertProfil(this.apprenantId, payload).subscribe({
      next: (updatedProfil) => {
        this.profil = updatedProfil;
        this.editingProfil = false;
      },
      error: () => { /* Feedback is provided by the HTTP interceptor. */ }
    });
  }

  openCompetenceModal(comp?: Competence): void {
    if (comp) {
      this.editCompMode = true;
      this.selectedCompId = comp.id!;
      this.competenceForm.patchValue({
        nom: comp.nom,
        niveau: comp.niveau
      });
    } else {
      this.editCompMode = false;
      this.selectedCompId = null;
      this.competenceForm.reset({ niveau: 80 });
    }
    this.showCompModal = true;
  }

  closeCompetenceModal(): void {
    this.showCompModal = false;
  }

  saveCompetence(): void {
    if (this.competenceForm.invalid || this.ui.saving() > 0) { this.competenceForm.markAllAsTouched(); return; }
    const payload: Competence = this.competenceForm.value;

    if (this.editCompMode && this.selectedCompId) {
      this.apprenantService.updateCompetence(this.selectedCompId, payload).subscribe({
        next: () => {
          this.loadApprenantData();
          this.closeCompetenceModal();
        },
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    } else {
      this.apprenantService.addCompetence(this.apprenantId, payload).subscribe({
        next: () => {
          this.loadApprenantData();
          this.closeCompetenceModal();
        },
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }

  async deleteCompetence(compId: number): Promise<void> {
    if (await this.ui.confirm('Voulez-vous supprimer cette compétence ?')) {
      this.apprenantService.deleteCompetence(compId).subscribe({
        next: () => this.loadApprenantData(),
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }
}
