import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { FormationService } from '../../services/formation.service';
import { AuthService } from '../../services/auth.service';
import { Formation } from '../../models/formation.model';
import { IconComponent } from '../../shared/icon.component';
interface Summary { formations: number; categories: number; apprenants: number; competences: number; profils: number; }
@Component({ selector: 'app-dashboard', standalone: true, imports: [CommonModule, RouterLink, IconComponent],
  templateUrl: './dashboard.component.html', styleUrl: './dashboard.component.css' })
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly courses = inject(FormationService);
  loading = true; error = false;
  summary: Summary = { formations: 0, categories: 0, apprenants: 0, competences: 0, profils: 0 };
  formations: Formation[] = [];
  ngOnInit(): void { this.loadData(); }
  loadData(): void {
    this.loading = true; this.error = false;
    forkJoin({ summary: this.http.get<Summary>('/api/dashboard'), formations: this.courses.getAll() }).subscribe({
      next: result => { this.summary = result.summary; this.formations = [...result.formations].sort((a,b) => (b.id || 0) - (a.id || 0)); this.loading = false; },
      error: () => { this.error = true; this.loading = false; }
    });
  }
  get coverage(): number { return this.summary.apprenants ? Math.round(this.summary.profils / this.summary.apprenants * 100) : 0; }
  get distribution(): { name: string; count: number; width: number }[] {
    const counts = new Map<string, number>();
    this.formations.forEach(f => { const name = f.categorie?.nom || 'Sans catégorie'; counts.set(name, (counts.get(name) || 0) + 1); });
    return [...counts].sort((a,b) => b[1] - a[1]).map(([name, count]) => ({ name, count, width: count / Math.max(1, this.formations.length) * 100 }));
  }
}
