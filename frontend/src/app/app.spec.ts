import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AppComponent } from './app';
import { AuthService, SessionUser } from './services/auth.service';

describe('Role navigation', () => {
  const user = signal<SessionUser | null>(null);
  beforeEach(async () => {
    user.set(null);
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: {
        user, get isAdmin() { return user()?.role === 'ADMIN'; }, home: '/formations'
      } }]
    }).compileComponents();
  });
  it('hides navigation before login', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.sidebar')).toBeNull();
  });
  it('shows management links for admins', async () => {
    user.set({ id: 1, email: 'admin@example.com', nom: 'Admin', prenom: 'Test', role: 'ADMIN' });
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.brand').textContent).toContain('SmartLearn');
    expect(fixture.nativeElement.querySelectorAll('.nav-links a').length).toBe(5);
    expect(fixture.nativeElement.textContent).toContain('Administration');
  });
  it('shows only personal links for users', async () => {
    user.set({ id: 2, email: 'user@example.com', nom: 'User', prenom: 'Test', role: 'USER' });
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.nav-links a').length).toBe(4);
    expect(fixture.nativeElement.textContent).toContain('Mon profil');
    expect(fixture.nativeElement.querySelector('a[href="/apprenants"]')).toBeNull();
  });
});
