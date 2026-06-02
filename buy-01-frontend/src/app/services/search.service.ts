import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ProductSearchDTO } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private apiUrl = `${environment.apiUrl}/v1/products`;

  constructor(private http: HttpClient) {}

  searchByKeyword(keyword: string): Observable<ProductSearchDTO[]> {
    let params = new HttpParams();
    if (keyword) params = params.set('keyword', keyword);

    return this.http.get<ProductSearchDTO[]>(`${this.apiUrl}/search`, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[SearchService] Error searching by keyword:', err);
        throw err;
      })
    );
  }

  filterByPrice(minPrice?: number, maxPrice?: number): Observable<ProductSearchDTO[]> {
    let params = new HttpParams();
    if (minPrice !== undefined && minPrice !== null) params = params.set('minPrice', minPrice.toString());
    if (maxPrice !== undefined && maxPrice !== null) params = params.set('maxPrice', maxPrice.toString());

    return this.http.get<ProductSearchDTO[]>(`${this.apiUrl}/filter`, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[SearchService] Error filtering by price:', err);
        throw err;
      })
    );
  }

  searchAndFilter(keyword?: string, category?: string, minPrice?: number, maxPrice?: number): Observable<ProductSearchDTO[]> {
    let params = new HttpParams();
    if (keyword) params = params.set('keyword', keyword);
    if (category && category !== 'all') params = params.set('category', category);
    if (minPrice !== undefined && minPrice !== null) params = params.set('minPrice', minPrice.toString());
    if (maxPrice !== undefined && maxPrice !== null) params = params.set('maxPrice', maxPrice.toString());

    return this.http.get<ProductSearchDTO[]>(`${this.apiUrl}/search-and-filter`, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[SearchService] Error searching and filtering:', err);
        throw err;
      })
    );
  }

  getFilteredAndSorted(category?: string, minPrice?: number, maxPrice?: number, sortBy: string = 'popularity'): Observable<ProductSearchDTO[]> {
    let params = new HttpParams().set('sortBy', sortBy);
    if (category && category !== 'all') params = params.set('category', category);
    if (minPrice !== undefined && minPrice !== null) params = params.set('minPrice', minPrice.toString());
    if (maxPrice !== undefined && maxPrice !== null) params = params.set('maxPrice', maxPrice.toString());

    return this.http.get<ProductSearchDTO[]>(`${this.apiUrl}/filter-and-sort`, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[SearchService] Error filtering and sorting:', err);
        throw err;
      })
    );
  }
}
