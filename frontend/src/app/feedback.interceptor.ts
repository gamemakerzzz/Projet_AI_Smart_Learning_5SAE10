import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { finalize, tap } from 'rxjs';
import { UiService } from './services/ui.service';
export const feedbackInterceptor: HttpInterceptorFn = (request, next) => {
  const ui = inject(UiService);
  if (!request.url.startsWith('/api/') || request.url.startsWith('/api/auth/')) return next(request);
  const mutation = !['GET', 'HEAD', 'OPTIONS'].includes(request.method);
  ui.pending.update(n => n + 1);
  if (mutation) ui.saving.update(n => n + 1);
  return next(request).pipe(tap({
    next: event => {
      if (mutation && event instanceof HttpResponse) ui.notify(request.method === 'DELETE' ? 'Suppression effectuée.' : 'Modifications enregistrées.', 'success');
    },
    error: error => {
      const message = error.status === 0 ? 'Le serveur est indisponible. Vérifiez votre connexion et réessayez.' :
        error.status === 401 ? 'Votre session a expiré. Reconnectez-vous.' :
        error.status === 403 ? 'Cette action est indisponible pour votre compte. Si votre session a expiré, reconnectez-vous.' :
        error.error?.message || 'Une erreur est survenue. Réessayez dans un instant.';
      ui.notify(message, 'error');
    }
  }), finalize(() => {
    ui.pending.update(n => Math.max(0, n - 1));
    if (mutation) ui.saving.update(n => Math.max(0, n - 1));
  }));
};
