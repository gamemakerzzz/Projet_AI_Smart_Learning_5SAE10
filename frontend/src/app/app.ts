import { Component, HostListener, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from './services/auth.service';
import { UiService } from './services/ui.service';
import { IconComponent } from './shared/icon.component';
import { FeedbackComponent } from './shared/feedback.component';

@Component({
  selector: 'app-root', standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent, FeedbackComponent],
  template: `
    <app-feedback />
    @if (auth.user(); as user) {
      <a href="#main-content" class="skip-link" (click)="focusMain($event)">Aller au contenu</a>
      <div class="app-container">
        @if (menuOpen) { <button class="nav-scrim" aria-label="Fermer le menu" (click)="menuOpen = false"></button> }
        <aside id="navigation" class="sidebar" [class.is-open]="menuOpen">
          <a [routerLink]="auth.home" class="brand"><span class="brand-icon"><app-icon name="book" /></span><span>SmartLearn<span class="brand-dot">.</span></span></a>
          <div class="workspace-label">{{ auth.isAdmin ? 'ESPACE ADMINISTRATEUR' : 'VOTRE ESPACE' }}</div>
          <nav aria-label="Navigation principale"><ul class="nav-links">
            @if (auth.isAdmin) {
              <li class="nav-item"><a routerLink="/dashboard" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="grid" /> Vue d'ensemble</a></li>
            } @else {
              <li class="nav-item"><a routerLink="/accueil" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="grid" /> Mon espace</a></li>
            }
            <li class="nav-item"><a routerLink="/formations" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="book" /> Formations</a></li>
            @if (auth.isAdmin) {
              <li class="nav-item"><a routerLink="/categories" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="tag" /> Catégories</a></li>
              <li class="nav-item"><a routerLink="/apprenants" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="users" /> Apprenants</a></li>
            } @else {
              <li class="nav-item"><a [routerLink]="['/apprenants', user.id, 'profil']" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="user" /> Mon profil</a></li>
            }
            <li class="nav-item"><a routerLink="/recommandations" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="spark" /> Recommandations</a></li>
            <li class="nav-item"><a routerLink="/examens" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="calendar" /> Examens</a></li>
            <li class="nav-item"><a routerLink="/seances" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="book" /> Séances</a></li>
            <li class="nav-item"><a routerLink="/notifications" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="bell" /> Notifications</a></li>
            <li class="nav-item"><a routerLink="/planificateur" routerLinkActive="active" ariaCurrentWhenActive="page"><app-icon name="spark" /> Planificateur</a></li>
          </ul></nav>
          <div class="sidebar-note"><app-icon name="spark" /><strong>Apprendre, à votre rythme.</strong><p>Les bonnes compétences ouvrent de nouvelles perspectives.</p></div>
          <div class="account-block">
            <div class="avatar">{{ user.prenom.charAt(0) }}{{ user.nom.charAt(0) }}</div>
            <div class="account-info"><strong>{{ user.prenom }} {{ user.nom }}</strong><small>{{ auth.isAdmin ? 'Administrateur' : 'Apprenant' }}</small></div>
            <button class="icon-button logout-button" aria-label="Se déconnecter" title="Se déconnecter" [disabled]="loggingOut" (click)="logout()"><app-icon name="logout" /></button>
          </div>
        </aside>
        <div class="workspace">
          <header class="topbar">
            <div class="topbar-start"><button #menuButton class="icon-button menu-button" aria-label="Ouvrir le menu" [attr.aria-expanded]="menuOpen" aria-controls="navigation" (click)="menuOpen = !menuOpen"><app-icon name="menu" /></button>
            <span class="breadcrumb">{{ auth.isAdmin ? 'Administration' : 'Espace apprenant' }} <span>/</span> <strong>{{ pageTitle }}</strong></span></div>
            <span class="access-badge"><app-icon name="shield" /> {{ auth.isAdmin ? 'ADMIN' : 'USER' }}</span>
          </header>
          @if (ui.pending() > 0) { <div class="request-progress" role="progressbar" aria-label="Chargement en cours"></div> }
          <main id="main-content" tabindex="-1" class="main-content"><router-outlet /></main>
          <footer class="workspace-footer"><span>SmartLearn · Votre prochain chapitre commence ici.</span><span>Apprentissage & compétences</span></footer>
        </div>
      </div>
    } @else { <router-outlet /> }
  `
})
export class AppComponent {
  readonly ui = inject(UiService);
  menuOpen = false;
  loggingOut = false;
  pageTitle = '';
  constructor(public auth: AuthService, private router: Router) {
    this.updateTitle(router.url);
    router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed()).subscribe(event => {
      this.menuOpen = false;
      this.updateTitle(event.urlAfterRedirects);
    });
  }
private updateTitle(url: string): void {
    const titles: Record<string, string> = { dashboard: "Vue d'ensemble", accueil: 'Mon espace', formations: 'Formations', categories: 'Catégories', apprenants: 'Apprenants', recommandations: 'Recommandations', login: 'Connexion', examens: 'Examens', seances: 'Séances', notifications: 'Notifications', planificateur: 'Planificateur' };
    this.pageTitle = url.includes('/profil') ? 'Profil' : titles[url.split('/')[1]?.split('?')[0]] || 'Bienvenue';
    document.title = `${this.pageTitle} · SmartLearn`;
  }
  @HostListener('document:keydown.escape') closeMenu(): void { this.menuOpen = false; }
  focusMain(event: Event): void { event.preventDefault(); document.getElementById('main-content')?.focus(); }
  async logout(): Promise<void> {
    if (this.loggingOut) return;
    this.loggingOut = true;
    try { await this.auth.logout(); await this.router.navigateByUrl('/login'); }
    catch { this.ui.notify('Déconnexion impossible. Réessayez.', 'error'); }
    finally { this.loggingOut = false; }
  }
}
