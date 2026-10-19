import { Routes } from '@angular/router';
import {HomeComponent} from './components/home/home.component';
import {LoginComponent} from './components/login/login.component';
import {DashboardComponent} from './components/dashboard/dashboard.component';
import {CategoriesComponent} from './components/categories/categories.component';
import {FormationsComponent} from './components/formations/formations.component';
import {FormationDetailComponent} from './components/formation-detail/formation-detail.component';
import {ApprenantsComponent} from './components/apprenants/apprenants.component';
import {ApprenantProfilComponent} from './components/apprenant-profil/apprenant-profil.component';
import {RecommandationsComponent} from './components/recommandations/recommandations.component';
import {ExamensComponent} from './components/examens/examens.component';
import {SeancesComponent} from './components/seances/seances.component';
import {NotificationsComponent} from './components/notifications/notifications.component';
import {PlanificateurComponent} from './components/planificateur/planificateur.component';
import {ChapitresRevoirComponent} from './components/chapitres-revoir/chapitres-revoir.component';
import {StatistiquesComponent} from './components/statistiques/statistiques.component';
import { authGuard, guestGuard } from './auth.guard';

export const routes: Routes = [
  { path: 'login', component:LoginComponent,canActivate: [guestGuard] },
  { path: 'accueil', component:HomeComponent,canActivate: [authGuard], data: { userOnly: true } },
  { path: '', redirectTo: 'accueil', pathMatch: 'full' },
  { path: 'dashboard', component:DashboardComponent,canActivate: [authGuard], data: { admin: true } },
  { path: 'categories', component:CategoriesComponent,canActivate: [authGuard], data: { admin: true } },
  { path: 'formations', component:FormationsComponent,canActivate: [authGuard] },
  { path: 'formations/:id', component:FormationDetailComponent,canActivate: [authGuard] },
  { path: 'apprenants', component:ApprenantsComponent,canActivate: [authGuard], data: { admin: true } },
  { path: 'apprenants/:id/profil', component:ApprenantProfilComponent,canActivate: [authGuard], data: { ownProfile: true } },
  { path: 'recommandations', component:RecommandationsComponent,canActivate: [authGuard] },
  { path: 'examens', component:ExamensComponent,canActivate: [authGuard] },
  { path: 'seances', component:SeancesComponent,canActivate: [authGuard] },
  { path: 'notifications', component:NotificationsComponent,canActivate: [authGuard] },
  { path: 'planificateur', component:PlanificateurComponent,canActivate: [authGuard] },
  { path: 'chapitres-revoir', component:ChapitresRevoirComponent,canActivate: [authGuard] },
  { path: 'statistiques', component:StatistiquesComponent,canActivate: [authGuard] },
  { path: '**', redirectTo: 'accueil' }
];