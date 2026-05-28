import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Product } from '../models/product.model';

export interface ProductSearchParams {
  keyword?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  pageSize?: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getAllProducts(filters: ProductSearchParams = {}): Observable<Product[]> {
    const params = this.buildHttpParams(filters);
    console.log('[ProductService] getAllProducts - URL:', this.apiUrl, 'params:', params.toString());
    return this.http.get<Product[]>(this.apiUrl, { params }).pipe(
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

  private buildHttpParams(filters: ProductSearchParams): HttpParams {
    let params = new HttpParams();

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return params;
  }
}
