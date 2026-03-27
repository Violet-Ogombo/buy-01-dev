import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from '../services/session.service';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const session = inject(SessionService);
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        session.clear();
        toast.info('Your session has expired. Please log in again.');
        router.navigate(['/login']);
      } else if (error.status === 403) {
        console.error('Access denied (403)');
        toast.error('You do not have permission to perform this action.');
      } else if (error.status >= 400 && error.status < 600) {
        console.error('HTTP error', error);
        toast.error('An error occurred while communicating with the server.');
      }
      return throwError(() => error);
    })
  );
};
