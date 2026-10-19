import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { AuthService } from '../../services/auth.service';
import { ExamenService } from '../../services/examen.service';
import { FormationService } from '../../services/formation.service';
import { ApprenantService } from '../../services/apprenant.service';

import { Examen } from '../../models/planner.model';
import { Formation } from '../../models/formation.model';
import { Apprenant } from '../../models/apprenant.model';

@Component({
  selector: 'app-examens',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h2>Examens</h2>
      <button class="btn btn-primary" (click)="openCreate()" *ngIf="canCreate()">
        + Nouvel examen
      </button>
    </div>

    <!-- Filtres -->
    <div class="filters">
      <select [value]="filterFormationId() ?? ''" (change)="onFilterFormation($event)">
        <option value="">Toutes les formations</option>
        <option *ngFor="let f of formations()" [value]="f.id">{{ f.titre }}</option>
      </select>
      <input type="date" [value]="filterFrom()" (change)="filterFrom.set($any($event.target).value)" />
      <input type="date" [value]="filterTo()" (change)="filterTo.set($any($event.target).value)" />
    </div>

    <p *ngIf="loading()">Chargement...</p>
    <p class="error" *ngIf="error() && !showModal()">{{ error() }}</p>
    <p *ngIf="!loading() && filteredExamens().length === 0">Aucun examen trouvé.</p>

    <table *ngIf="filteredExamens().length > 0">
      <thead>
        <tr>
          <th>Titre</th>
          <th>Formation</th>
          <th *ngIf="isAdmin()">Apprenant</th>
          <th>Date</th>
          <th>Durée</th>
          <th>Coef.</th>
          <th *ngIf="isAdmin()"></th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let e of filteredExamens()">
          <td>{{ e.titre }}</td>
          <td>{{ formationName(e.formationId) }}</td>
          <td *ngIf="isAdmin()">{{ apprenantName(e.apprenantId) }}</td>
          <td>{{ e.dateExamen | date: 'dd/MM/yyyy HH:mm' }}</td>
          <td>{{ e.dureeMinutes }} min</td>
          <td>{{ e.coefficient }}</td>
          <td *ngIf="isAdmin()">
            <button (click)="openEdit(e)">Modifier</button>
            <button (click)="remove(e)">Supprimer</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div class="modal" *ngIf="showModal()">
      <form [formGroup]="form" (ngSubmit)="saveExamen()">
        <h3>{{ editingId() !== null ? 'Modifier' : 'Créer' }} un examen</h3>

        <!-- Admin uniquement -->
        <div class="form-group" *ngIf="isAdmin()">
          <label for="examen-apprenant">Apprenant *</label>
          <select id="examen-apprenant" formControlName="apprenantId">
            <option [ngValue]="null">Sélectionner un apprenant...</option>
            <option *ngFor="let a of apprenants()" [ngValue]="a.id">
              {{ a.prenom }} {{ a.nom }}
            </option>
          </select>
          <small class="error" *ngIf="form.get('apprenantId')?.touched && form.get('apprenantId')?.invalid">
            Apprenant obligatoire
          </small>
        </div>

        <div class="form-group">
          <label for="examen-titre">Titre *</label>
          <input id="examen-titre" formControlName="titre" />
        </div>

        <div class="form-group">
          <label for="examen-description">Description</label>
          <textarea id="examen-description" formControlName="description"></textarea>
        </div>

        <div class="form-group">
          <label for="examen-formation">Formation *</label>
          <select id="examen-formation" formControlName="formationId">
            <option [ngValue]="null">Sélectionner...</option>
            <option *ngFor="let f of formations()" [ngValue]="f.id">{{ f.titre }}</option>
          </select>
        </div>

        <div class="form-group">
          <label for="examen-date">Date *</label>
          <input id="examen-date" type="datetime-local" formControlName="dateExamen" />
        </div>

        <div class="form-group">
          <label for="examen-duree">Durée (minutes) *</label>
          <input id="examen-duree" type="number" min="1" formControlName="dureeMinutes" />
        </div>

        <div class="form-group">
          <label for="examen-coef">Coefficient *</label>
          <input id="examen-coef" type="number" min="0" step="0.5" formControlName="coefficient" />
        </div>

        <p class="error" *ngIf="error()">{{ error() }}</p>

        <div class="actions">
          <button type="button" (click)="closeModal()">Annuler</button>
          <button type="submit" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Enregistrement...' : 'Enregistrer' }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:2rem; }
    .filters { display:flex; gap:.75rem; margin-bottom:1.5rem; flex-wrap:wrap; align-items:center; }
    .filters select, .filters input { padding:.6rem .9rem; border:1px solid var(--border-color); border-radius:var(--radius-sm); background:var(--bg-card); color:var(--text); transition:border-color .2s, box-shadow .2s; }
    .filters select:focus, .filters input:focus { outline:none; border-color:var(--primary); box-shadow:0 0 0 3px var(--primary-light); }
    button { padding:.5rem 1rem; border:none; border-radius:var(--radius-sm); cursor:pointer; font-weight:600; transition:transform .1s, box-shadow .2s; }
    button:hover { transform:translateY(-1px); }
    .btn-primary { background:var(--primary); color:#fff; }
    .btn-primary:hover { box-shadow:0 4px 12px rgba(0,0,0,.15); }
    table { width:100%; border-collapse:collapse; }
    th, td { padding:.75rem; border-bottom:1px solid var(--border-color); text-align:left; }
    .modal { position:fixed; inset:0; background:rgba(0,0,0,.5); display:flex; align-items:center; justify-content:center; }
    .modal form { background:var(--bg-card); padding:1.5rem; border-radius:var(--radius); width:min(520px,90vw); }
    .form-group { margin-bottom:1rem; display:flex; flex-direction:column; gap:.25rem; }
    .actions { display:flex; gap:.5rem; justify-content:flex-end; }
    .error { color:#dc2626; }
  `]
})
export class ExamensComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private examenService = inject(ExamenService);
  private formationService = inject(FormationService);
  private apprenantService = inject(ApprenantService);

  // État
  examens = signal<Examen[]>([]);
  formations = signal<Formation[]>([]);
  apprenants = signal<Apprenant[]>([]);
  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);

  showModal = signal(false);
  editingId = signal<number | null>(null);

  // Filtres
  filterFormationId = signal<number | null>(null);
  filterFrom = signal('');
  filterTo = signal('');

  // Rôle : fonctionne que auth.isAdmin soit un booléen, un signal ou une méthode
  isAdmin = computed(() => {
    const v: any = (this.auth as any).isAdmin;
    return typeof v === 'function' ? !!v.call(this.auth) : !!v;
  });
  canCreate = computed(() => this.isAdmin());

  form: FormGroup = this.fb.group({
    apprenantId: [null as number | null],
    titre: ['', Validators.required],
    description: [''],
    formationId: [null as number | null, Validators.required],
    dateExamen: ['', Validators.required],
    dureeMinutes: [60, [Validators.required, Validators.min(1)]],
    coefficient: [1, [Validators.required, Validators.min(0)]]
  });

  filteredExamens = computed(() => {
    const fid = this.filterFormationId();
    const from = this.filterFrom();
    const to = this.filterTo();
    return this.examens().filter(e => {
      if (fid && e.formationId !== fid) return false;
      const d = new Date(e.dateExamen).getTime();
      if (from && d < new Date(from).getTime()) return false;
      if (to && d > new Date(to + 'T23:59:59').getTime()) return false;
      return true;
    });
  });

  ngOnInit(): void {
    this.formationService.getAll().subscribe(f => this.formations.set(f));
    if (this.isAdmin()) {
      this.apprenantService.getAll().subscribe(a => this.apprenants.set(a));
    }
    this.loadExamens();
  }

  // ---------- Chargement ----------
  loadExamens(): void {
    this.loading.set(true);
    this.error.set(null);

    const request$ = this.isAdmin()
      ? this.examenService.getAll()                                  // ADMIN : tous
      : this.examenService.getAllByApprenant(this.currentUserId());  // USER : les siens

    request$.subscribe({
      next: list => { this.examens.set(list); this.loading.set(false); },
      error: () => { this.error.set('Erreur de chargement des examens.'); this.loading.set(false); }
    });
  }

  // ---------- Modal ----------
  openCreate(): void {
    this.editingId.set(null);
    this.error.set(null);
    this.form.reset({
      apprenantId: null,
      titre: '',
      description: '',
      formationId: null,
      dateExamen: '',
      dureeMinutes: 60,
      coefficient: 1
    });
    this.applyApprenantRule(false);
    this.showModal.set(true);
  }

  openEdit(e: Examen): void {
    this.editingId.set(e.id ?? null);
    this.error.set(null);
    this.form.reset({
      apprenantId: e.apprenantId ?? null,   // propriétaire de l'examen (et non e.id)
      titre: e.titre,
      description: e.description ?? '',
      formationId: e.formationId ?? null,
      dateExamen: this.toLocalInput(e.dateExamen),
      dureeMinutes: e.dureeMinutes,
      coefficient: e.coefficient
    });
    this.applyApprenantRule(true);
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  /** Obligatoire pour l'admin à la création, verrouillé en modification. */
  private applyApprenantRule(isEdit: boolean): void {
    const ctrl = this.form.get('apprenantId')!;
    if (this.isAdmin()) {
      ctrl.setValidators(Validators.required);
      if (isEdit) {
        ctrl.disable();
      } else {
        ctrl.enable();
      }
    } else {
      ctrl.clearValidators();
      ctrl.disable();
    }
    ctrl.updateValueAndValidity();
  }

  // ---------- Enregistrement ----------
  saveExamen(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const raw = this.form.getRawValue(); // inclut les contrôles désactivés
    const id = this.editingId();

    // Création : l'admin utilise l'apprenant choisi, le USER utilise son ID.
    // Aucun repli de l'admin vers son propre ID.
    const apprenantId: number | null | undefined =
      this.isAdmin() ? raw.apprenantId : this.currentUserId();

    if (id === null && apprenantId == null) {
      this.error.set('Veuillez sélectionner un apprenant.');
      return;
    }

    // L'apprenantId passe dans l'URL à la création, pas dans le corps
    const payload: Partial<Examen> = {
      titre: raw.titre,
      description: raw.description || undefined,
      formationId: raw.formationId ?? undefined,
      dateExamen: raw.dateExamen,
      dureeMinutes: Number(raw.dureeMinutes),
      coefficient: Number(raw.coefficient)
    };

    this.saving.set(true);
    this.error.set(null);

    const request$ = id !== null
      ? this.examenService.update(id, payload)
      : this.examenService.create(apprenantId as number, payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.loadExamens();
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? "Erreur lors de l'enregistrement.");
      }
    });
  }

  // ---------- Suppression ----------
  remove(e: Examen): void {
    if (e.id == null) return;
    if (!confirm(`Supprimer l'examen « ${e.titre} » ?`)) return;
    this.examenService.delete(e.id).subscribe({
      next: () => this.loadExamens(),
      error: () => this.error.set('Suppression impossible.')
    });
  }

  // ---------- Utilitaires ----------
  onFilterFormation(event: Event): void {
    const v = (event.target as HTMLSelectElement).value;
    this.filterFormationId.set(v ? Number(v) : null);
  }

  formationName(id?: number | null): string {
    if (id == null) return '—';
    return this.formations().find(f => f.id === id)?.titre ?? '—';
  }

  apprenantName(id?: number | null): string {
    if (id == null) return '—';
    const a = this.apprenants().find(x => x.id === id);
    return a ? `${a.prenom} ${a.nom}` : '—';
  }

  private currentUserId(): number {
    return this.auth.user()?.id as number;
  }

  private toLocalInput(iso?: string | null): string {
    // "2026-10-19T14:30:00" -> "2026-10-19T14:30" pour datetime-local
    return iso ? iso.substring(0, 16) : '';
  }
}