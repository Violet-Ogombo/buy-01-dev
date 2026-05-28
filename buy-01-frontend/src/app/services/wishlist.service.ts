import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, catchError, timeout, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { WishlistItemDTO } from '../models/wishlist.model';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private apiUrl = `${environment.apiUrl}/v1/wishlist`;
  private wishlistSubject = new BehaviorSubject<WishlistItemDTO[]>([]);
  readonly wishlist$ = this.wishlistSubject.asObservable();

  constructor(private http: HttpClient) {}

  getUserWishlist(): Observable<WishlistItemDTO[]> {
    return this.http.get<WishlistItemDTO[]>(this.apiUrl).pipe(
      timeout(10000),
      tap(items => this.wishlistSubject.next(items)),
      catchError(err => {
        console.error('[WishlistService] Error fetching wishlist:', err);
        throw err;
      })
    );
  }

  addToWishlist(productId: string): Observable<WishlistItemDTO> {
    return this.http.post<WishlistItemDTO>(`${this.apiUrl}/${productId}`, {}).pipe(
      timeout(10000),
      tap(() => this.getUserWishlist().subscribe()),
      catchError(err => {
        console.error('[WishlistService] Error adding to wishlist:', err);
        throw err;
      })
    );
  }

  removeFromWishlist(productId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${productId}`).pipe(
      timeout(10000),
      tap(() => this.getUserWishlist().subscribe()),
      catchError(err => {
        console.error('[WishlistService] Error removing from wishlist:', err);
        throw err;
      })
    );
  }

  getWishlistCount(): Observable<number> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/count`).pipe(
      timeout(10000),
      map(res => res.count),
      catchError(err => {
        console.error('[WishlistService] Error getting wishlist count:', err);
        throw err;
      })
    );
  }

  isInWishlist(productId: string): boolean {
    return this.wishlistSubject.getValue().some(item => item.productId === productId);
  }
}
