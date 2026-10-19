import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Component, signal } from '@angular/core';
import { AppComponent } from './app';
import { AuthService, SessionUser } from './services/auth.service';

@Component({ standalone: true, template: 'Login route' })
class LoginRouteProbe {
  static instances = 0;
  constructor() { LoginRouteProbe.instances++; }
}

describe('Role navigation', () => {
  const user = signal<SessionUser | null>(null);
  beforeEach(async () => {
    user.set(null);
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([{ path: 'login', component: LoginRouteProbe }]), { provide: AuthService, useValue: {
        user, get isAdmin() { return user()?.role === 'ADMIN'; }, home: '/formations'
      } }]
    }).compileComponents();
  });
  it('hides navigation before login', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.sidebar')).toBeNull();
  });
  it('keeps the active routed view when the authenticated layout appears', async () => {
    LoginRouteProbe.instances = 0;
    const fixture = TestBed.createComponent(AppComponent);
    await TestBed.inject(Router).navigateByUrl('/login');
    await fixture.whenStable();
    const outlet = fixture.nativeElement.querySelector('router-outlet');
    expect(LoginRouteProbe.instances).toBe(1);
    user.set({ id: 1, email: 'admin@example.com', nom: 'Admin', prenom: 'Test', role: 'ADMIN' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBe(outlet);
    expect(LoginRouteProbe.instances).toBe(1);
    expect(fixture.nativeElement.querySelector('.sidebar')).not.toBeNull();
  });
  it('shows management links for admins', async () => {
    user.set({ id: 1, email: 'admin@example.com', nom: 'Admin', prenom: 'Test', role: 'ADMIN' });
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.brand').textContent).toContain('SmartLearn');
    expect(fixture.nativeElement.querySelectorAll('.nav-links a').length).toBe(16);
    expect(fixture.nativeElement.querySelector('a[href="/tutor"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/forum"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Administration');
  });
  it('shows only personal links for users', async () => {
    user.set({ id: 2, email: 'user@example.com', nom: 'User', prenom: 'Test', role: 'USER' });
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.nav-links a').length).toBe(15);
    expect(fixture.nativeElement.querySelector('a[href="/tutor"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/forum"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Mon profil');
    expect(fixture.nativeElement.querySelector('a[href="/apprenants"]')).toBeNull();
  });
});
