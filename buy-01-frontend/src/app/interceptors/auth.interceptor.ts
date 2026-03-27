import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionService } from '../services/session.service';
import { HttpXsrfTokenExtractor } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const token = session.getToken();
  const xsrfTokenExtractor = inject(HttpXsrfTokenExtractor);

  // Public endpoints that don't need XSRF protection
  const publicEndpoints = ['/api/auth/register', '/api/auth/login'];
  const isPublicEndpoint = publicEndpoints.some(endpoint => req.url.includes(endpoint));

  let clonedReq = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  // Add XSRF token if present (Angular automatically extracts from cookie)
  // This provides CSRF protection for state-changing operations (POST, PUT, DELETE)
  // Skip for public endpoints like register and login
  if (!isPublicEndpoint) {
    const xsrfToken = xsrfTokenExtractor.getToken();
    if (xsrfToken && !clonedReq.headers.has('X-XSRF-TOKEN')) {
      clonedReq = clonedReq.clone({
        setHeaders: {
          'X-XSRF-TOKEN': xsrfToken
        }
      });
    }
  }

  return next(clonedReq);
};
