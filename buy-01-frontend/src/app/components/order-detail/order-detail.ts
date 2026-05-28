import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { OrderDTO } from '../../models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss'
})
export class OrderDetailComponent implements OnInit, OnDestroy {
  order: OrderDTO | null = null;
  loading = true;
  error: string | null = null;

  // Timeline statuses
  statuses = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'];

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params.get('id');
        if (id) {
          this.loadOrderDetail(id);
        } else {
          this.error = 'Order ID is missing';
          this.loading = false;
          this.cdr.markForCheck();
        }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrderDetail(id: string) {
    this.loading = true;
    this.error = null;
    this.orderService.getOrderById(id)
      .subscribe({
        next: (order) => {
          this.order = order;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.loading = false;
          if (err.status === 404) {
            this.error = 'Order not found';
          } else {
            this.error = 'Failed to load order details';
          }
          this.cdr.markForCheck();
        }
      });
  }

  cancelOrder() {
    if (!this.order) return;
    if (!confirm('Are you sure you want to cancel this order?')) return;

    this.loading = true;
    this.orderService.cancelOrder(this.order.id)
      .subscribe({
        next: () => {
          this.toast.success('Order cancelled');
          this.loadOrderDetail(this.order!.id);
        },
        error: () => {
          this.loading = false;
          this.toast.error('Failed to cancel order');
          this.cdr.markForCheck();
        }
      });
  }

  redoOrder() {
    if (!this.order) return;
    if (!confirm('Duplicating items to your cart. Continue?')) return;

    this.orderService.redoOrder(this.order.id)
      .subscribe({
        next: () => {
          this.toast.success('Items added to cart');
          this.router.navigate(['/cart']);
        },
        error: () => {
          this.toast.error('Failed to reorder items');
        }
      });
  }

  getStatusStepIndex(status: string): number {
    if (status === 'CANCELLED') return -1;
    return this.statuses.indexOf(status);
  }

  isStepCompleted(stepIndex: number): boolean {
    if (!this.order || this.order.status === 'CANCELLED') return false;
    const currentIdx = this.getStatusStepIndex(this.order.status);
    return stepIndex <= currentIdx;
  }
}
