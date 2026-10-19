import { inject } from '@angular/core';
import { UiService } from '../../services/ui.service';
import { ModalA11yDirective } from '../../shared/modal-a11y.directive';
import { IconComponent } from '../../shared/icon.component';
import { AuthService } from '../../services/auth.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { CategorieService } from '../../services/categorie.service';
import { Formation, FormationRequest } from '../../models/formation.model';
import { Categorie } from '../../models/categorie.model';

@Component({
  selector: 'app-formations',
  standalone: true,
  imports: [ModalA11yDirective, CommonModule, FormsModule, ReactiveFormsModule, RouterLink, IconComponent],
  template: `
    <div class="formations-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Catalogue des Formations</h1>
          <p class="page-subtitle">{{ auth.isAdmin ? "Consulter, rechercher et gérer l'offre de formation" : "Découvrez les formations adaptées à vos objectifs" }}</p>
        </div>
        <button *ngIf="auth.isAdmin" class="btn btn-primary" (click)="openModal()">+ Nouvelle Formation</button>
      </div>

      <!-- Filters & Search Bar -->
      <div class="filters-bar">
        <div class="search-box">
          <span class="search-icon"><app-icon name="search" /></span>
          <input 
            type="text" 
            class="form-control search-input" 
            aria-label="Rechercher une formation" placeholder="Titre, compétence, mot-clé…" 
            [(ngModel)]="searchKeyword"
            (input)="onSearch()"
          />
        </div>

        <div class="filter-selects">
          <select aria-label="Filtrer par catégorie" class="form-control" [(ngModel)]="selectedCategorieId" (change)="onCategorieFilter()">
            <option [ngValue]="null">Toutes les catégories</option>
            <option *ngFor="let cat of categories" [ngValue]="cat.id">{{ cat.nom }}</option>
          </select>

          <select aria-label="Filtrer par niveau" class="form-control" [(ngModel)]="selectedNiveau" (change)="onNiveauFilter()">
            <option value="">Tous les niveaux</option>
            <option value="Débutant">Débutant</option>
            <option value="Intermédiaire">Intermédiaire</option>
            <option value="Avancé">Avancé</option>
          </select>
        </div>
      </div>

      <div class="catalog-toolbar">
        <span aria-live="polite">{{ loading ? 'Chargement…' : formations.length + ' formation(s) disponible(s)' }}</span>
        <select class="form-control sort-control" aria-label="Trier les formations" [(ngModel)]="sortOrder" (change)="onSearch()">
          <option value="recent">Derniers ajouts</option><option value="title">Titre : A à Z</option><option value="price">Prix croissant</option>
        </select>
      </div>
      <div *ngIf="loading" class="loading-state" role="status">Chargement du catalogue…</div>
      <div *ngIf="loadError" class="error-state" role="alert"><span>Le catalogue n'a pas pu être chargé.</span><button class="btn btn-secondary" (click)="loadFormations()">Réessayer</button></div>
      <div *ngIf="!loading && !loadError && !formations.length" class="empty-state"><app-icon name="search" /><h3>Aucune formation à afficher</h3><p>Essayez d'autres mots-clés ou réinitialisez vos filtres.</p><button class="btn btn-secondary" (click)="resetFilters()">Réinitialiser les filtres</button></div>
      <div *ngIf="!auth.isAdmin && !loading && !loadError" class="course-grid">
        <article class="course-card" *ngFor="let f of visibleFormations; let i = index">
          <div class="course-cover" [class.tone-1]="i % 3 === 1" [class.tone-2]="i % 3 === 2"><span>{{ f.categorie?.nom }}</span><app-icon name="book" /></div>
          <div class="course-body"><div><span class="badge badge-secondary">{{ f.niveau }}</span></div><h3>{{ f.titre }}</h3><p>{{ f.description || 'Découvrez les chapitres de cette formation.' }}</p><div class="course-bottom"><strong>{{ f.prix === 0 ? 'Gratuit' : (f.prix | currency:'EUR') }}</strong><a [routerLink]="['/formations', f.id]" class="text-link">Découvrir <app-icon name="arrow" /></a></div></div>
        </article>
      </div>
      <!-- Administrator catalogue -->
      <div class="table-container" *ngIf="auth.isAdmin && !loading && !loadError && formations.length">
        <table class="custom-table">
          <thead>
            <tr>
              <th>Formation</th>
              <th>Catégorie</th>
              <th>Niveau</th>
              <th>Mots-clés</th>
              <th>Prix</th>
              <th style="text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let f of visibleFormations">
              <td>
                <div style="font-weight: 700; color: var(--text-main);">{{ f.titre }}</div>
                <div style="font-size: 0.8rem; color: var(--text-muted); line-height: 1.3;">{{ f.description }}</div>
              </td>
              <td><span class="badge badge-primary">{{ f.categorie?.nom }}</span></td>
              <td><span class="badge badge-secondary">{{ f.niveau }}</span></td>
              <td>
                <span class="mots-cles-tag">{{ f.motsCles || '-' }}</span>
              </td>
              <td style="font-weight: 700; color: var(--success);">{{ f.prix }} €</td>
              <td style="text-align: right;">
                <div style="display: inline-flex; gap: 0.4rem;">
                  <a [routerLink]="['/formations', f.id]" class="btn btn-secondary btn-sm" title="Voir détails et chapitres">Chapitres</a>
                  <button *ngIf="auth.isAdmin" class="btn btn-secondary btn-sm" (click)="openModal(f)" aria-label="Modifier la formation">Modifier</button>
                  <button *ngIf="auth.isAdmin" class="btn btn-danger btn-sm" (click)="deleteFormation(f.id!)" aria-label="Supprimer la formation">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr *ngIf="formations.length === 0">
              <td colspan="6" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                Aucune formation ne correspond aux critères de recherche.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <nav class="pagination" aria-label="Pagination du catalogue" *ngIf="formations.length && !loading && !loadError">
        <span>{{ (page - 1) * pageSize + 1 }}–{{ Math.min(page * pageSize, formations.length) }} sur {{ formations.length }} formations</span>
        <div><button class="btn btn-secondary btn-sm" [disabled]="page === 1" (click)="page = page - 1">Précédent</button><span>Page {{ page }} / {{ pageCount }}</span><button class="btn btn-secondary btn-sm" [disabled]="page >= pageCount" (click)="page = page + 1">Suivant</button></div>
      </nav>
      <!-- Modal Add / Edit Formation -->
      <div class="modal-backdrop" *ngIf="showModal">
        <div class="modal-content" appModal (dismiss)="closeModal()">
          <div class="modal-header">
            <h2 class="modal-title">{{ editMode ? 'Modifier la Formation' : 'Créer une Formation' }}</h2>
            <button aria-label="Fermer la fenêtre" class="close-btn" (click)="closeModal()">&times;</button>
          </div>

          <form [formGroup]="formationForm" (ngSubmit)="saveFormation()">
            <div class="form-group">
              <label class="form-label" for="formations-titre">Titre de la formation *</label>
              <input id="formations-titre" type="text" formControlName="titre" class="form-control" placeholder="ex: Spring Boot Avancé"><small class="field-error" *ngIf="formationForm.get('titre')?.touched && formationForm.get('titre')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
            </div>

            <div class="form-group">
              <label class="form-label" for="formations-description">Description</label>
              <textarea id="formations-description" formControlName="description" class="form-control" rows="3" placeholder="Présentation du cours..."></textarea>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
              <div class="form-group">
                <label class="form-label" for="formations-prix">Prix (€) *</label>
              <input id="formations-prix" type="number" step="0.01" formControlName="prix" class="form-control" placeholder="199.00"><small class="field-error" *ngIf="formationForm.get('prix')?.touched && formationForm.get('prix')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
              </div>

              <div class="form-group">
                <label class="form-label" for="formations-niveau">Niveau *</label>
              <select id="formations-niveau" formControlName="niveau" class="form-control">
                  <option value="Débutant">Débutant</option>
                  <option value="Intermédiaire">Intermédiaire</option>
                  <option value="Avancé">Avancé</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="formations-categorieId">Catégorie *</label>
              <select id="formations-categorieId" formControlName="categorieId" class="form-control">
                <option [value]="null" disabled>Sélectionner une catégorie</option>
                <option *ngFor="let cat of categories" [ngValue]="cat.id">{{ cat.nom }}</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="formations-motsCles">Mots-clés (séparés par des espaces)</label>
              <input id="formations-motsCles" type="text" formControlName="motsCles" class="form-control" placeholder="ex: java spring backend rest api"><small class="field-error" *ngIf="formationForm.get('motsCles')?.touched && formationForm.get('motsCles')?.invalid">Vérifiez ce champ : valeur obligatoire ou format incorrect.</small>
            </div>

            <div *ngIf="errorMessage" style="color: var(--danger); background: rgba(239, 68, 68, 0.1); padding: 0.75rem; border-radius: 8px; margin-bottom: 1rem; font-size: 0.85rem;">
              {{ errorMessage }}
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-secondary" (click)="closeModal()">Annuler</button>
              <button type="submit" class="btn btn-primary" [disabled]="formationForm.invalid || ui.saving() > 0">
                {{ editMode ? 'Enregistrer' : 'Créer' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .filters-bar {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }
    .search-box {
      position: relative;
      flex: 1;
      min-width: 280px;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-muted);
    }
    .search-input {
      padding-left: 2.2rem;
    }
    .filter-selects {
      display: flex;
      gap: 0.75rem;
    }
    .mots-cles-tag {
      font-size: 0.8rem;
      color: var(--accent);
      background: var(--accent-light);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-family: monospace;
    }
  `]
})
export class FormationsComponent implements OnInit {
  readonly ui = inject(UiService);
  readonly Math = Math;
  loading = true;
  loadError = false;
  page = 1;
  pageSize = 6;
  sortOrder = 'recent';
  get pageCount(): number { return Math.max(1, Math.ceil(this.formations.length / this.pageSize)); }
  get visibleFormations(): Formation[] { return this.formations.slice((this.page - 1) * this.pageSize, this.page * this.pageSize); }
  resetFilters(): void { this.searchKeyword = ''; this.selectedCategorieId = null; this.selectedNiveau = ''; this.applyFilters(); }
  formations: Formation[] = [];
  categories: Categorie[] = [];
  searchKeyword = '';
  selectedCategorieId: number | null = null;
  selectedNiveau = '';

  showModal = false;
  editMode = false;
  selectedId: number | null = null;
  formationForm: FormGroup;
  errorMessage: string | null = null;

  constructor(
    public auth: AuthService,
    private formationService: FormationService,
    private categorieService: CategorieService,
    private fb: FormBuilder
  ) {
    this.formationForm = this.fb.group({
      titre: ['', Validators.required],
      description: [''],
      prix: [0, [Validators.required, Validators.min(0)]],
      niveau: ['Débutant', Validators.required],
      motsCles: [''],
      categorieId: [null, Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadFormations();
  }

  loadCategories(): void {
    this.categorieService.getAll().subscribe({
      next: (data) => this.categories = data,
      error: (err) => console.error(err)
    });
  }

  private allFormations: Formation[] = [];

  loadFormations(): void {
    this.loading = true; this.loadError = false;
    this.formationService.getAll().subscribe({
      next: (data) => { this.allFormations = data; this.applyFilters(); this.loading = false; },
      error: () => { this.loadError = true; this.loading = false; }
    });
  }

  private applyFilters(): void {
    this.page = 1;
    const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase();
    const keyword = normalize(this.searchKeyword.trim());
    this.formations = this.allFormations.filter(f =>
      (!keyword || normalize([f.titre, f.description, f.motsCles, f.categorie?.nom].join(' ')).includes(keyword)) &&
      (!this.selectedCategorieId || f.categorie?.id === Number(this.selectedCategorieId)) &&
      (!this.selectedNiveau || f.niveau.toLocaleLowerCase() === this.selectedNiveau.toLocaleLowerCase())
    );
    this.formations.sort((a,b) => this.sortOrder === 'title' ? a.titre.localeCompare(b.titre, 'fr') :
      this.sortOrder === 'price' ? a.prix - b.prix : (b.id || 0) - (a.id || 0));
  }

  onSearch(): void { this.applyFilters(); }
  onCategorieFilter(): void { this.applyFilters(); }
  onNiveauFilter(): void { this.applyFilters(); }

  openModal(formation?: Formation): void {
    this.errorMessage = null;
    if (formation) {
      this.editMode = true;
      this.selectedId = formation.id!;
      this.formationForm.patchValue({
        titre: formation.titre,
        description: formation.description || '',
        prix: formation.prix,
        niveau: formation.niveau,
        motsCles: formation.motsCles || '',
        categorieId: formation.categorie?.id
      });
    } else {
      this.editMode = false;
      this.selectedId = null;
      this.formationForm.reset({
        prix: 0,
        niveau: 'Débutant',
        categorieId: this.categories.length > 0 ? this.categories[0].id : null
      });
    }
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveFormation(): void {
    if (this.formationForm.invalid || this.ui.saving() > 0) { this.formationForm.markAllAsTouched(); return; }
    this.errorMessage = null;

    const payload: FormationRequest = {
      ...this.formationForm.value,
      categorieId: Number(this.formationForm.value.categorieId)
    };

    if (this.editMode && this.selectedId) {
      this.formationService.update(this.selectedId, payload).subscribe({
        next: () => {
          this.loadFormations();
          this.closeModal();
        },
        error: (err) => this.errorMessage = err.error?.message || 'Erreur lors de la mise à jour'
      });
    } else {
      this.formationService.create(payload).subscribe({
        next: () => {
          this.loadFormations();
          this.closeModal();
        },
        error: (err) => this.errorMessage = err.error?.message || 'Erreur lors de la création'
      });
    }
  }

  async deleteFormation(id: number): Promise<void> {
    if (await this.ui.confirm('Voulez-vous vraiment supprimer cette formation ?')) {
      this.formationService.delete(id).subscribe({
        next: () => this.loadFormations(),
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }
}
