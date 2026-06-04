import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { OrderDTO, OrderStatus, PagedResponse } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = `${environment.apiUrl}/v1/orders`;

  constructor(private http: HttpClient) {}

  getOrderById(id: string): Observable<OrderDTO> {
    return this.http.get<OrderDTO>(`${this.apiUrl}/${id}`).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error fetching order:', err);
        throw err;
      })
    );
  }

  getUserOrders(page: number = 0, size: number = 10): Observable<PagedResponse<OrderDTO>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PagedResponse<OrderDTO>>(this.apiUrl, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error fetching user orders:', err);
        throw err;
      })
    );
  }

  getOrdersByStatus(status: OrderStatus, page: number = 0, size: number = 10): Observable<PagedResponse<OrderDTO>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PagedResponse<OrderDTO>>(`${this.apiUrl}/status/${status}`, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error fetching orders by status:', err);
        throw err;
      })
    );
  }

  searchOrders(orderNumber: string, page: number = 0, size: number = 10): Observable<PagedResponse<OrderDTO>> {
    const params = new HttpParams()
      .set('orderNumber', orderNumber)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<OrderDTO>>(`${this.apiUrl}/search`, { params }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error searching orders:', err);
        throw err;
      })
    );
  }

  updateOrderStatus(orderId: string, status: OrderStatus): Observable<OrderDTO> {
    return this.http.put<OrderDTO>(`${this.apiUrl}/${orderId}/status`, { status }).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error updating order status:', err);
        throw err;
      })
    );
  }

  cancelOrder(orderId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${orderId}`).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error cancelling order:', err);
        throw err;
      })
    );
  }

  redoOrder(orderId: string): Observable<OrderDTO> {
    return this.http.post<OrderDTO>(`${this.apiUrl}/${orderId}/redo`, {}).pipe(
      timeout(10000),
      catchError(err => {
        console.error('[OrderService] Error redoing order:', err);
        throw err;
      })
    );
  }
}
