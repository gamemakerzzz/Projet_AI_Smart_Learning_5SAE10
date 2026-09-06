import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { IconComponent } from '../../shared/icon.component';
@Component({
  selector: 'app-login', standalone: true, imports: [FormsModule, IconComponent],
  template: `
    <div class="login-layout">
      <section class="login-story" aria-label="Bienvenue sur SmartLearn">
        <div class="brand"><span class="brand-icon"><app-icon name="book" /></span><span>SmartLearn<span class="brand-dot">.</span></span></div>
        <div class="story-content"><span class="story-label">LE SAVOIR OUVRE DES PORTES</span><h1>Apprenez aujourd'hui.<br><em>Construisez demain.</em></h1><p>Un espace pour explorer de nouvelles compétences et donner une direction à vos ambitions.</p>
          <div class="learning-art" aria-hidden="true"><div class="art-grid"></div><div class="art-card card-back"></div><div class="art-card card-front"><span class="art-icon"><app-icon name="spark" /></span><span class="art-eyebrow">VOTRE PROCHAINE ÉTAPE</span><strong>Une compétence.<br>De nouvelles possibilités.</strong><div class="art-line"><i></i></div><span class="art-footer">Un parcours qui vous ressemble <app-icon name="arrow" /></span></div><div class="art-tag"><app-icon name="check" /> À votre rythme</div></div>
        </div>
        <p class="story-footer">Votre prochain chapitre commence ici.</p>
      </section>
      <section class="login-form-panel">
        <div class="mobile-brand">SmartLearn<span>.</span></div>
        <div class="login-form-content"><p class="eyebrow">HEUREUX DE VOUS RETROUVER</p><h2>Connectez-vous<br>à votre espace.</h2><p class="intro">Retrouvez vos formations et vos objectifs.</p>
          <form #form="ngForm" (ngSubmit)="submit()">
            <label for="email">Adresse e-mail</label>
            <input id="email" name="email" type="email" class="form-control" [(ngModel)]="email" required email autocomplete="username" placeholder="vous@exemple.com" [readOnly]="busy">
            <label for="password">Mot de passe</label>
            <div class="password-field"><input id="password" name="password" [type]="showPassword ? 'text' : 'password'" class="form-control" [(ngModel)]="password" required autocomplete="current-password" placeholder="Votre mot de passe" [readOnly]="busy" (keyup)="checkCaps($event)"><button type="button" (click)="showPassword = !showPassword" [attr.aria-label]="showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'">{{ showPassword ? 'Masquer' : 'Afficher' }}</button></div>
            @if (capsLock) { <p class="caps-hint">La touche Verr. Maj est activée.</p> }
            @if (error) { <p class="login-error" role="alert">{{ error }}</p> }
            <button class="btn btn-primary submit-button" type="submit" [disabled]="form.invalid || busy">{{ busy ? 'Connexion en cours…' : 'Se connecter' }} <app-icon name="arrow" /></button>
          </form>
          <div class="account-help"><app-icon name="user" /><p>Vous n'avez pas encore de compte ?<br><strong>Contactez votre administrateur.</strong></p></div>
          <div class="secure-note"><app-icon name="shield" /> Un accès personnel à votre espace de formation.</div>
        </div>
        <p class="login-footer">SmartLearn · Apprentissage & compétences</p>
      </section>
    </div>
  `,
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email = ''; password = ''; error = ''; busy = false; showPassword = false; capsLock = false;
  constructor(private auth: AuthService, private router: Router) {}
  checkCaps(event: KeyboardEvent): void { this.capsLock = event.getModifierState('CapsLock'); }
  async submit(): Promise<void> {
    if (this.busy || !this.email || !this.password) return;
    this.busy = true; this.error = '';
    try {
      await this.auth.login(this.email, this.password);
      this.password = '';
      await this.router.navigateByUrl(this.auth.home);
    } catch (error: any) {
      this.error = error.status === 401 ? 'Adresse e-mail ou mot de passe incorrect.' :
        'Connexion impossible. Vérifiez que le serveur est disponible et réessayez.';
    } finally { this.busy = false; }
  }
}
