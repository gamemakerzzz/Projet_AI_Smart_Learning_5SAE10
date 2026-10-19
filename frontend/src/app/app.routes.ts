import { HomeComponent } from './components/home/home.component';
import { authGuard, guestGuard } from './auth.guard';
import { LoginComponent } from './components/login/login.component';
import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { CategoriesComponent } from './components/categories/categories.component';
import { FormationsComponent } from './components/formations/formations.component';
import { FormationDetailComponent } from './components/formation-detail/formation-detail.component';
import { ApprenantsComponent } from './components/apprenants/apprenants.component';
import { ApprenantProfilComponent } from './components/apprenant-profil/apprenant-profil.component';
import { RecommandationsComponent } from './components/recommandations/recommandations.component';

export const routes: Routes = [
  { path: 'tutor', canActivate: [authGuard], loadComponent: () => import('./components/tutor/tutor.component').then(m => m.TutorComponent) },
  { path: 'forum', canActivate: [authGuard], loadComponent: () => import('./components/forum/forum.component').then(m => m.ForumComponent) },
  { path: 'flashcards', canActivate: [authGuard], loadComponent: () => import('./components/flashcards/flashcards.component').then(m => m.FlashcardsComponent) },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'accueil', component: HomeComponent, canActivate: [authGuard], data: { userOnly: true } },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard], data: { admin: true } },
  { path: 'categories', component: CategoriesComponent, canActivate: [authGuard], data: { admin: true } },
  { path: 'formations', component: FormationsComponent, canActivate: [authGuard] },
  { path: 'formations/:id', component: FormationDetailComponent, canActivate: [authGuard] },
  { path: 'apprenants', component: ApprenantsComponent, canActivate: [authGuard], data: { admin: true } },
  { path: 'apprenants/:id/profil', component: ApprenantProfilComponent, canActivate: [authGuard], data: { ownProfile: true } },
  { path: 'recommandations', component: RecommandationsComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'accueil' }
];
