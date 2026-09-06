import { inject } from '@angular/core';
import { UiService } from '../../services/ui.service';
import { ModalA11yDirective } from '../../shared/modal-a11y.directive';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApprenantService } from '../../services/apprenant.service';
import { Apprenant, ApprenantRequest } from '../../models/apprenant.model';

@Component({
  selector: 'app-apprenants',
  standalone: true,
  imports: [ModalA11yDirective, CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="apprenants-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Gestion des Apprenants</h1>
          <p class="page-subtitle">Liste des étudiants, profils d'apprentissage et compétences</p>
        </div>
        <button class="btn btn-primary" (click)="openModal()">+ Nouvel Apprenant</button>
      </div>

      <!-- Apprenants Table -->
      <div class="table-container">
        <table class="custom-table">
          <thead>
            <tr>
              <th>Apprenant</th>
              <th>Email</th>
              <th>Objectif Vise</th>
              <th>Niveau</th>
              <th style="text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let a of apprenants">
              <td>
                <div style="display: flex; align-items: center; gap: 0.75rem;">
                  <div class="avatar">{{ a.prenom.charAt(0) }}{{ a.nom.charAt(0) }}</div>
                  <div>
                    <div style="font-weight: 700; color: var(--text-main);">{{ a.prenom }} {{ a.nom }}</div>
                    <div style="font-size: 0.78rem; color: var(--text-dim);">ID: {{ a.id }}</div>
                  </div>
                </div>
              </td>
              <td style="color: var(--text-muted);">{{ a.email }}</td>
              <td>
                <span class="badge badge-accent">{{ a.profil?.objectif || 'Non renseigné' }}</span>
              </td>
              <td>
                <span class="badge badge-secondary">{{ a.profil?.niveau || 'N/A' }}</span>
              </td>
              <td style="text-align: right;">
                <div style="display: inline-flex; gap: 0.4rem;">
                  <a [routerLink]="['/apprenants', a.id, 'profil']" class="btn btn-primary btn-sm">
                    👤 Voir Profil & Compétences
                  </a>
                  <button class="btn btn-secondary btn-sm" (click)="openModal(a)" aria-label="Modifier">Modifier</button>
                  <button class="btn btn-danger btn-sm" (click)="deleteApprenant(a.id!)" aria-label="Supprimer">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr *ngIf="apprenants.length === 0">
              <td colspan="5" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                Aucun apprenant trouvé.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Modal Add/Edit Apprenant -->
      <div class="modal-backdrop" *ngIf="showModal">
        <div class="modal-content" appModal (dismiss)="closeModal()">
          <div class="modal-header">
            <h2 class="modal-title">{{ editMode ? 'Modifier l\'Apprenant' : 'Créer un Apprenant' }}</h2>
            <button aria-label="Fermer la fenêtre" class="close-btn" (click)="closeModal()">&times;</button>
          </div>

          <form [formGroup]="apprenantForm" (ngSubmit)="saveApprenant()">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
              <div class="form-group">
                <label class="form-label" for="apprenants-nom">Nom *</label>
              <input id="apprenants-nom" type="text" formControlName="nom" class="form-control" placeholder="ex: Benali"><small class="field-error" *ngIf="apprenantForm.get('nom')?.touched && apprenantForm.get('nom')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
              </div>

              <div class="form-group">
                <label class="form-label" for="apprenants-prenom">Prénom *</label>
              <input id="apprenants-prenom" type="text" formControlName="prenom" class="form-control" placeholder="ex: Ahmed"><small class="field-error" *ngIf="apprenantForm.get('prenom')?.touched && apprenantForm.get('prenom')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="apprenants-email">Adresse Email *</label>
              <input id="apprenants-email" type="email" formControlName="email" class="form-control" placeholder="ahmed@example.com"><small class="field-error" *ngIf="apprenantForm.get('email')?.touched && apprenantForm.get('email')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
            </div>

            <div class="form-group">
              <label class="form-label" for="apprenants-password">{{ editMode ? 'Nouveau mot de passe (optionnel)' : 'Mot de passe *' }}</label>
              <input id="apprenants-password" type="password" formControlName="password" class="form-control" placeholder="••••••••"><small class="field-error" *ngIf="apprenantForm.get('password')?.touched && apprenantForm.get('password')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
            </div>

            <div *ngIf="errorMessage" style="color: var(--danger); background: rgba(239, 68, 68, 0.1); padding: 0.75rem; border-radius: 8px; margin-bottom: 1rem; font-size: 0.85rem;">
              {{ errorMessage }}
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-secondary" (click)="closeModal()">Annuler</button>
              <button type="submit" class="btn btn-primary" [disabled]="apprenantForm.invalid || ui.saving() > 0">
                {{ editMode ? 'Enregistrer' : 'Créer' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .avatar {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--primary), var(--accent));
      color: white;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
    }
  `]
})
export class ApprenantsComponent implements OnInit {
  readonly ui = inject(UiService);
  apprenants: Apprenant[] = [];
  showModal = false;
  editMode = false;
  selectedId: number | null = null;
  apprenantForm: FormGroup;
  errorMessage: string | null = null;

  constructor(
    private apprenantService: ApprenantService,
    private fb: FormBuilder
  ) {
    this.apprenantForm = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadApprenants();
  }

  loadApprenants(): void {
    this.apprenantService.getAll().subscribe({
      next: (data) => this.apprenants = data,
      error: (err) => console.error(err)
    });
  }

  openModal(apprenant?: Apprenant): void {
    this.errorMessage = null;
    if (apprenant) {
      this.editMode = true;
      this.selectedId = apprenant.id!;
      this.apprenantForm.get('password')?.clearValidators();
      this.apprenantForm.get('password')?.updateValueAndValidity();
      this.apprenantForm.patchValue({
        nom: apprenant.nom,
        prenom: apprenant.prenom,
        email: apprenant.email,
        password: ''
      });
    } else {
      this.editMode = false;
      this.selectedId = null;
      this.apprenantForm.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
      this.apprenantForm.get('password')?.updateValueAndValidity();
      this.apprenantForm.reset();
    }
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveApprenant(): void {
    if (this.apprenantForm.invalid || this.ui.saving() > 0) { this.apprenantForm.markAllAsTouched(); return; }
    this.errorMessage = null;

    const payload: ApprenantRequest = this.apprenantForm.value;

    if (this.editMode && this.selectedId) {
      this.apprenantService.update(this.selectedId, payload).subscribe({
        next: () => {
          this.loadApprenants();
          this.closeModal();
        },
        error: (err) => this.errorMessage = err.error?.message || 'Erreur lors de la modification'
      });
    } else {
      this.apprenantService.create(payload).subscribe({
        next: () => {
          this.loadApprenants();
          this.closeModal();
        },
        error: (err) => this.errorMessage = err.error?.message || 'Erreur lors de la création'
      });
    }
  }

  async deleteApprenant(id: number): Promise<void> {
    if (await this.ui.confirm('Voulez-vous vraiment supprimer cet apprenant ?')) {
      this.apprenantService.delete(id).subscribe({
        next: () => this.loadApprenants(),
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }
}
