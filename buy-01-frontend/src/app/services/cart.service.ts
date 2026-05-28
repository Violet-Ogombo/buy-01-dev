import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CartDTO, CartItemDTO, CheckoutRequest } from '../models/cart.model';
import { OrderDTO } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private apiUrl = `${environment.apiUrl}/v1/cart`;
  private cartSubject = new BehaviorSubject<CartDTO | null>(null);
  readonly cart$ = this.cartSubject.asObservable();

  constructor(private http: HttpClient) {}

  getCart(): Observable<CartDTO> {
    return this.http.get<CartDTO>(this.apiUrl).pipe(
      timeout(10000),
      tap(cart => this.cartSubject.next(cart)),
      catchError(err => {
        console.error('[CartService] Error loading cart:', err);
        throw err;
      })
    );
  }

  addToCart(productId: string, quantity: number): Observable<CartItemDTO> {
    return this.http.post<CartItemDTO>(`${this.apiUrl}/items`, { productId, quantity }).pipe(
      timeout(10000),
      tap(() => this.getCart().subscribe()),
      catchError(err => {
        console.error('[CartService] Error adding to cart:', err);
        throw err;
      })
    );
  }

  updateCartItem(itemId: string, quantity: number): Observable<CartItemDTO> {
    return this.http.put<CartItemDTO>(`${this.apiUrl}/items/${itemId}`, { quantity }).pipe(
      timeout(10000),
      tap(() => this.getCart().subscribe()),
      catchError(err => {
        console.error('[CartService] Error updating cart item:', err);
        throw err;
      })
    );
  }

  removeCartItem(itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${itemId}`).pipe(
      timeout(10000),
      tap(() => this.getCart().subscribe()),
      catchError(err => {
        console.error('[CartService] Error removing cart item:', err);
        throw err;
      })
    );
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.apiUrl).pipe(
      timeout(10000),
      tap(() => this.cartSubject.next(null)),
      catchError(err => {
        console.error('[CartService] Error clearing cart:', err);
        throw err;
      })
    );
  }

  checkout(request: CheckoutRequest): Observable<OrderDTO> {
    return this.http.post<OrderDTO>(`${this.apiUrl}/checkout`, request).pipe(
      timeout(10000),
      tap(() => this.cartSubject.next(null)),
      catchError(err => {
        console.error('[CartService] Error checking out:', err);
        throw err;
      })
    );
  }
}
