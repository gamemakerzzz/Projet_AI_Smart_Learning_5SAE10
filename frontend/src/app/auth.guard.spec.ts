import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, convertToParamMap, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './services/auth.service';

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
});
