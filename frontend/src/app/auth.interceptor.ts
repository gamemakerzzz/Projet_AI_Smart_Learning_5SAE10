import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(catchError(error => {
    if (error.status === 401 && req.url.startsWith('/api/') && !req.url.startsWith('/api/auth/')) {
      auth.clear();
      void router.navigateByUrl('/login');
    }
    return throwError(() => error);
  }));
};
