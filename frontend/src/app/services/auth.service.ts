import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface SessionUser {
  id: number; email: string; prenom: string; nom: string; role: 'ADMIN' | 'USER';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<SessionUser | null>(null);
  private initialized = false;
  constructor(private http: HttpClient) {}
  get isAdmin(): boolean { return this.user()?.role === 'ADMIN'; }
  get home(): string { return this.isAdmin ? '/dashboard' : '/accueil'; }

  async restore(): Promise<void> {
    if (this.initialized) return;
    await firstValueFrom(this.http.get('/api/auth/csrf'));
    try {
      this.user.set(await firstValueFrom(this.http.get<SessionUser>('/api/auth/me')));
    } catch (error: any) {
      if (error.status !== 401) throw error;
      this.user.set(null);
    }
    this.initialized = true;
  }

  async login(email: string, password: string): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
    const body = new HttpParams().set('email', email.trim()).set('password', password);
    await firstValueFrom(this.http.post('/api/auth/login', body));
    // Authentication rotates the session and clears the old CSRF token.
    await firstValueFrom(this.http.get('/api/auth/csrf'));
    this.user.set(await firstValueFrom(this.http.get<SessionUser>('/api/auth/me')));
    this.initialized = true;
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
    await firstValueFrom(this.http.post('/api/auth/logout', {}));
    this.clear();
  }

  clear(): void { this.user.set(null); this.initialized = false; }
}
