import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, convertToParamMap, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './services/auth.service';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { Router } from '@angular/router';

describe('Access guards', () => {
  const session = { user: () => ({ id: 2, role: 'USER' }), isAdmin: false, restore: async () => {} };
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: session }]
  }));
  async function check(data: object, id?: string) {
    const route = { data, paramMap: convertToParamMap(id ? { id } : {}) } as ActivatedRouteSnapshot;
    return await TestBed.runInInjectionContext(() => authGuard(route, {} as RouterStateSnapshot));
  }
  it('redirects users away from administration', async () => {
    expect((await check({ admin: true }) as UrlTree).toString()).toBe('/formations');
  });
  it('redirects another profile to the signed-in user', async () => {
    expect((await check({ ownProfile: true }, '99') as UrlTree).toString()).toBe('/apprenants/2/profil');
  });
  it('allows the signed-in user profile', async () => {
    expect(await check({ ownProfile: true }, '2')).toBe(true);
  });
  it('allows any signed-in user to open flashcards', async () => {
    expect(await check({})).toBe(true);
  });
  it('sends a logged-out direct Flashcards visit to login', async () => {
    TestBed.overrideProvider(AuthService, { useValue: { restore: async () => {}, user: () => null } });
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    const harness = await RouterTestingHarness.create('/flashcards');
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(harness.routeNativeElement?.textContent).toContain('Connectez-vous');
    expect(harness.routeNativeElement?.textContent).not.toContain('sans compte');
  });
  it('opens the login page as the default entry for logged-out visitors', async () => {
    TestBed.overrideProvider(AuthService, { useValue: { restore: async () => {}, user: () => null } });
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/login');
  });
});
