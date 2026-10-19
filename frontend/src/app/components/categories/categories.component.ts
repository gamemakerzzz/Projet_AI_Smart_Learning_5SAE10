import { inject } from '@angular/core';
import { UiService } from '../../services/ui.service';
import { ModalA11yDirective } from '../../shared/modal-a11y.directive';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CategorieService } from '../../services/categorie.service';
import { Categorie } from '../../models/categorie.model';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [ModalA11yDirective, CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="categories-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">Catégories de Formation</h1>
          <p class="page-subtitle">Gérer les thématiques et domaines d'apprentissage</p>
        </div>
        <button class="btn btn-primary" (click)="openModal()">+ Nouvelle Catégorie</button>
      </div>

      <!-- Categories Grid -->
      <div class="categories-grid">
        <div *ngFor="let cat of categories" class="card category-card">
          <div class="category-header">
            <span class="badge badge-primary">ID: {{ cat.id }}</span>
            <div class="category-actions">
              <button class="btn btn-secondary btn-sm" (click)="openModal(cat)" aria-label="Modifier">Modifier</button>
              <button class="btn btn-danger btn-sm" (click)="deleteCategorie(cat.id!)" aria-label="Supprimer">Supprimer</button>
            </div>
          </div>
          <h3 class="category-name">{{ cat.nom }}</h3>
          <p class="category-desc">{{ cat.description || 'Aucune description fournie.' }}</p>
        </div>

        <div *ngIf="categories.length === 0" class="empty-state">
          Aucune catégorie créée. Cliquez sur "+ Nouvelle Catégorie" pour démarrer.
        </div>
      </div>

      <!-- Modal Add / Edit -->
      <div class="modal-backdrop" *ngIf="showModal">
        <div class="modal-content" appModal (dismiss)="closeModal()">
          <div class="modal-header">
            <h2 class="modal-title">{{ editMode ? 'Modifier la Catégorie' : 'Nouvelle Catégorie' }}</h2>
            <button aria-label="Fermer la fenêtre" class="close-btn" (click)="closeModal()">&times;</button>
          </div>
          
          <form [formGroup]="categoryForm" (ngSubmit)="saveCategory()">
            <div class="form-group">
              <label class="form-label" for="categories-nom">Nom de la catégorie *</label>
              <input id="categories-nom" type="text" formControlName="nom" class="form-control" placeholder="ex: IT, Cloud, Management...">
              <div *ngIf="categoryForm.get('nom')?.touched && categoryForm.get('nom')?.invalid" style="color: var(--danger); font-size: 0.8rem; margin-top: 0.3rem;">
                Le nom est obligatoire.
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="categories-description">Description</label>
              <textarea id="categories-description" formControlName="description" class="form-control" rows="3" placeholder="Description de la catégorie..."></textarea>
            </div>

            <div *ngIf="errorMessage" style="color: var(--danger); background: rgba(239, 68, 68, 0.1); padding: 0.75rem; border-radius: 8px; margin-bottom: 1rem; font-size: 0.85rem;">
              {{ errorMessage }}
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-secondary" (click)="closeModal()">Annuler</button>
              <button type="submit" class="btn btn-primary" [disabled]="categoryForm.invalid || ui.saving() > 0">
                {{ editMode ? 'Enregistrer' : 'Créer' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .categories-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1.5rem;
    }
    .category-card {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    .category-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    .category-actions {
      display: flex;
      gap: 0.4rem;
    }
    .category-name {
      font-size: 1.25rem;
      font-weight: 700;
      margin-bottom: 0.5rem;
    }
    .category-desc {
      color: var(--text-muted);
      font-size: 0.88rem;
      flex: 1;
    }
    .empty-state {
      grid-column: 1 / -1;
      text-align: center;
      padding: 3rem;
      color: var(--text-muted);
      background: var(--bg-card);
      border-radius: var(--radius-md);
      border: 1px dashed var(--border-color);
    }
  `]
})
export class CategoriesComponent implements OnInit {
  readonly ui = inject(UiService);
  categories: Categorie[] = [];
  showModal = false;
  editMode = false;
  selectedId: number | null = null;
  categoryForm: FormGroup;
  errorMessage: string | null = null;

  constructor(
    private categorieService: CategorieService,
    private fb: FormBuilder
  ) {
    this.categoryForm = this.fb.group({
      nom: ['', Validators.required],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.categorieService.getAll().subscribe({
      next: (data) => this.categories = data,
      error: (err) => console.error(err)
    });
  }

  openModal(categorie?: Categorie): void {
    this.errorMessage = null;
    if (categorie) {
      this.editMode = true;
      this.selectedId = categorie.id!;
      this.categoryForm.patchValue({
        nom: categorie.nom,
        description: categorie.description || ''
      });
    } else {
      this.editMode = false;
      this.selectedId = null;
      this.categoryForm.reset();
    }
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveCategory(): void {
    if (this.categoryForm.invalid || this.ui.saving() > 0) { this.categoryForm.markAllAsTouched(); return; }
    this.errorMessage = null;

    const payload: Categorie = this.categoryForm.value;

    if (this.editMode && this.selectedId) {
      this.categorieService.update(this.selectedId, payload).subscribe({
        next: () => {
          this.loadCategories();
          this.closeModal();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Erreur lors de la modification';
        }
      });
    } else {
      this.categorieService.create(payload).subscribe({
        next: () => {
          this.loadCategories();
          this.closeModal();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Erreur lors de la création';
        }
      });
    }
  }

  async deleteCategorie(id: number): Promise<void> {
    if (await this.ui.confirm('Voulez-vous vraiment supprimer cette catégorie ?')) {
      this.categorieService.delete(id).subscribe({
        next: () => this.loadCategories(),
        error: () => { /* Feedback is provided by the HTTP interceptor. */ }
      });
    }
  }
}
