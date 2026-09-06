import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ApprenantService } from '../../services/apprenant.service';
import { FormationService } from '../../services/formation.service';
import { Apprenant } from '../../models/apprenant.model';
import { Competence } from '../../models/competence.model';
import { Formation } from '../../models/formation.model';
import { IconComponent } from '../../shared/icon.component';
@Component({ selector: 'app-home', standalone: true, imports: [CommonModule, RouterLink, IconComponent],
  templateUrl: './home.component.html', styleUrl: './home.component.css' })
export class HomeComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly learners = inject(ApprenantService);
  private readonly courses = inject(FormationService);
  loading = true; error = false;
  learner: Apprenant | null = null;
  skills: Competence[] = [];
  formations: Formation[] = [];
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true; this.error = false;
    const id = this.auth.user()!.id;
    forkJoin({ learner: this.learners.getById(id), skills: this.learners.getCompetences(id), formations: this.courses.getAll() }).subscribe({
      next: result => { this.learner = result.learner; this.skills = result.skills; this.formations = [...result.formations].sort((a,b) => (b.id || 0) - (a.id || 0)); this.loading = false; },
      error: () => { this.loading = false; this.error = true; }
    });
  }
  get steps(): { title: string; done: boolean }[] {
    return [ { title: 'Définir mon objectif', done: !!this.learner?.profil?.objectif?.trim() },
      { title: 'Choisir mon domaine', done: !!this.learner?.profil?.domaineInteret?.trim() },
      { title: 'Indiquer mon niveau', done: !!this.learner?.profil?.niveau?.trim() },
      { title: 'Ajouter mes compétences', done: this.skills.length > 0 } ];
  }
  get completeness(): number { return this.steps.filter(s => s.done).length * 25; }
}
