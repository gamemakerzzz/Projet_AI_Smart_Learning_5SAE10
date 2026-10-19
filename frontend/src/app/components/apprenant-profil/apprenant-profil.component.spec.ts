import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject } from 'rxjs';
import { ApprenantProfilComponent } from './apprenant-profil.component';
import { ApprenantService } from '../../services/apprenant.service';
import { AuthService } from '../../services/auth.service';
import { Apprenant } from '../../models/apprenant.model';

describe('Learner profile switching', () => {
  it('clears the previous profile and ignores late responses for another learner', () => {
    const first = new Subject<Apprenant>();
    const second = new Subject<Apprenant>();
    const service = { getById: (id: number) => id === 1 ? first : second, getCompetences: () => of([]) } as unknown as ApprenantService;
    const component = TestBed.runInInjectionContext(() => new ApprenantProfilComponent(
      {} as AuthService, {} as ActivatedRoute, service, new FormBuilder()));
    component.apprenantId = 1;
    component.loadApprenantData();
    first.next({ id: 1, nom: 'One', prenom: 'User', email: 'one@example.com', profil: { objectif: 'Java' } });
    expect(component.profilForm.value.objectif).toBe('Java');
    component.apprenantId = 2;
    component.loadApprenantData();
    expect(component.profilForm.value.objectif).toBe('');
    second.next({ id: 2, nom: 'Two', prenom: 'User', email: 'two@example.com' });
    first.next({ id: 1, nom: 'One', prenom: 'User', email: 'one@example.com', profil: { objectif: 'Old data' } });
    expect(component.apprenant?.id).toBe(2);
    expect(component.profilForm.value.objectif).toBe('');
  });
});
