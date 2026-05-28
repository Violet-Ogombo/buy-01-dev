import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { OrderDTO, OrderStatus } from '../../models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss'
})
export class OrderList implements OnInit, OnDestroy {
  orders: OrderDTO[] = [];
  loading = true;
  error: string | null = null;

  // Search & Filtering State
  searchTerm: string = '';
  selectedStatus: OrderStatus | 'ALL' = 'ALL';
  
  // Pagination State
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  lastPage = true;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  statusTabs: { value: OrderStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All Orders' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'SHIPPED', label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ];

  constructor(
    private orderService: OrderService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadOrders();
    });

    this.loadOrders();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchSubject.next(input.value.trim());
  }

  selectStatus(status: OrderStatus | 'ALL') {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadOrders();
  }

  loadOrders() {
    this.loading = true;
    this.error = null;

    if (this.searchTerm) {
      this.orderService.searchOrders(this.searchTerm, this.currentPage, this.pageSize)
        .subscribe({
          next: (res) => {
            this.orders = res.content;
            this.totalPages = res.totalPages;
            this.totalElements = res.totalElements;
            this.lastPage = res.last;
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.error = 'Failed to find orders matching that query.';
            this.cdr.markForCheck();
          }
        });
    } else if (this.selectedStatus !== 'ALL') {
      this.orderService.getOrdersByStatus(this.selectedStatus, this.currentPage, this.pageSize)
        .subscribe({
          next: (res) => {
            this.orders = res.content;
            this.totalPages = res.totalPages;
            this.totalElements = res.totalElements;
            this.lastPage = res.last;
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.error = `Failed to load ${this.selectedStatus.toLowerCase()} orders.`;
            this.cdr.markForCheck();
          }
        });
    } else {
      this.orderService.getUserOrders(this.currentPage, this.pageSize)
        .subscribe({
          next: (res) => {
            this.orders = res.content;
            this.totalPages = res.totalPages;
            this.totalElements = res.totalElements;
            this.lastPage = res.last;
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.error = 'Failed to retrieve your order list.';
            this.cdr.markForCheck();
          }
        });
    }
  }

  changePage(delta: number) {
    const next = this.currentPage + delta;
    if (next < 0 || next >= this.totalPages) return;
    this.currentPage = next;
    this.loadOrders();
  }

  cancelOrder(event: Event, orderId: string) {
    event.stopPropagation(); // Avoid triggering route navigation
    if (!confirm('Are you sure you want to cancel this order?')) return;

    this.orderService.cancelOrder(orderId).subscribe({
      next: () => {
        this.toast.success('Order cancelled successfully.');
        this.loadOrders();
      },
      error: () => {
        this.toast.error('Failed to cancel order.');
      }
    });
  }

  redoOrder(event: Event, orderId: string) {
    event.stopPropagation();
    if (!confirm('Would you like to duplicate this order and review it in your cart?')) return;

    this.orderService.redoOrder(orderId).subscribe({
      next: (order) => {
        this.toast.success('Items from previous order duplicated.');
        this.router.navigate(['/cart']);
      },
      error: () => {
        this.toast.error('Failed to redo order.');
      }
    });
  }

  trackByOrderId(index: number, order: OrderDTO): string {
    return order.id;
  }
}
