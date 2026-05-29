import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  let outgoing = req;

  const needsAuth = req.url.includes('/api/admin/') || req.url.includes('/api/auth/user');

  if (auth.method() === 'firebase') {
    const token = auth.firebaseToken();
    if (token && req.url.includes('/api/')) {
      outgoing = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
  } else if (needsAuth) {
    if (auth.method() === 'basic') {
      const header = auth.basicHeader();
      if (header) {
        outgoing = req.clone({ setHeaders: { Authorization: header } });
      }
    } else {
      outgoing = req.clone({ withCredentials: true });
    }
  }

  return next(outgoing).pipe(
    catchError((err) => {
      if (err?.status === 401) {
        const shouldRedirect =
          auth.method() === 'firebase'
            ? req.url.includes('/api/')
            : req.url.includes('/api/admin/');
        if (shouldRedirect) {
          if (auth.method() === 'basic') {
            auth.clear();
          }
          void router.navigate(['/login']);
        }
      }
      return throwError(() => err);
    }),
  );
};
