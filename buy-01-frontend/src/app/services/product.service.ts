import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getAllProducts(): Observable<Product[]> {
    console.log('[ProductService] getAllProducts - URL:', this.apiUrl);
    return this.http.get<Product[]>(this.apiUrl).pipe(
      timeout(10000),
      tap(products => console.log('[ProductService] Products loaded:', products?.length || 0)),
      catchError(err => {
        console.error('[ProductService] Error loading products:', err);
        throw err;
      })
    );
  }

  getProduct(id: string): Observable<Product> {
    const url = `${this.apiUrl}/${id}`;
    console.log('[ProductService] getProduct - URL:', url);
    const request$ = this.http.get<Product>(url);
    console.log('[ProductService] Observable created, about to subscribe');
    return request$.pipe(
      timeout(10000),
      tap(product => console.log('[ProductService] Product response received:', product)),
      catchError(err => {
        console.error('[ProductService] HTTP error:', err);
        throw err;
      })
    );
  }

  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }

  updateProduct(id: string, product: Product): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
