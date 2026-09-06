import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './services/auth.service';

export const authGuard: CanActivateFn = async route => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try { await auth.restore(); } catch { return router.parseUrl('/login'); }
  const user = auth.user();
  if (!user) return router.parseUrl('/login');
  if (route.data['userOnly'] && auth.isAdmin) return router.parseUrl('/dashboard');
  if (route.data['admin'] && !auth.isAdmin) return router.parseUrl('/formations');
  if (route.paramMap.has('id') && route.data['ownProfile'] && !auth.isAdmin &&
      Number(route.paramMap.get('id')) !== user.id) {
    return router.parseUrl(`/apprenants/${user.id}/profil`);
  }
  return true;
};

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try { await auth.restore(); } catch { return true; }
  return auth.user() ? router.parseUrl(auth.home) : true;
};
