import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../services/auth.service';
import { FormBuilder } from '@angular/forms';
import { of } from 'rxjs';
import { FormationsComponent } from './formations.component';
import { FormationService } from '../../services/formation.service';
import { CategorieService } from '../../services/categorie.service';

describe('Catalogue filters', () => {
  it('combines filters and restores results when cleared', () => {
    const formations = [
      { id: 1, titre: 'Java intro', niveau: 'Débutant', categorie: { id: 1 } },
      { id: 2, titre: 'Java avancé', niveau: 'Avancé', categorie: { id: 1 } },
      { id: 3, titre: 'Java finance', niveau: 'Débutant', categorie: { id: 2 } }
    ];
    const service = { getAll: () => of(formations) } as unknown as FormationService;
    const component = TestBed.runInInjectionContext(() => new FormationsComponent({ isAdmin: true } as AuthService, service, {} as CategorieService, new FormBuilder()));
    component.loadFormations();
    component.searchKeyword = ' JAVA ';
    component.selectedCategorieId = 1;
    component.selectedNiveau = 'Débutant';
    component.onSearch();
    expect(component.formations.map(f => f.id)).toEqual([1]);
    component.selectedNiveau = '';
    component.onNiveauFilter();
    expect(component.formations.map(f => f.id)).toEqual([2, 1]);
    component.selectedCategorieId = null;
    component.onCategorieFilter();
    expect(component.formations.length).toBe(3);
    component.searchKeyword = 'missing';
    component.onSearch();
    expect(component.formations.length).toBe(0);
  });
});
