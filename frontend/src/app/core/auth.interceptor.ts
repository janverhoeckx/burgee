import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  let outgoing = req;
  if (req.url.includes('/api/admin/')) {
    const header = auth.basicHeader();
    if (header) {
      outgoing = req.clone({ setHeaders: { Authorization: header } });
    }
  }

  return next(outgoing).pipe(
    catchError((err) => {
      if (err?.status === 401 && req.url.includes('/api/admin/')) {
        auth.clear();
        void router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
