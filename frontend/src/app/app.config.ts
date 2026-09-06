import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
registerLocaleData(localeFr);
import { feedbackInterceptor } from './feedback.interceptor';
import { authInterceptor } from './auth.interceptor';
import {
  ApplicationConfig,
  provideZoneChangeDetection
} from '@angular/core';

import {
  provideRouter,
  withComponentInputBinding,
  withHashLocation
} from '@angular/router';

import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    provideZoneChangeDetection(),

    provideRouter(
      routes,
      withComponentInputBinding(),
      withHashLocation()
    ),

    provideHttpClient(withInterceptors([authInterceptor, feedbackInterceptor]))
  ]
};