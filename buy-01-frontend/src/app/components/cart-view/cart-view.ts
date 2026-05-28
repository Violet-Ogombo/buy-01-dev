import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { CartDTO } from '../../models/cart.model';

@Component({
  selector: 'app-cart-view',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './cart-view.html',
  styleUrl: './cart-view.scss'
})
export class CartViewComponent implements OnInit, OnDestroy {
  cart: CartDTO | null = null;
  loading = true;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private cartService: CartService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.cartService.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe(cart => {
        this.cart = cart;
        this.cdr.markForCheck();
      });

    this.loadCart();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCart() {
    this.loading = true;
    this.error = null;
    this.cartService.getCart()
      .subscribe({
        next: () => {
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.loading = false;
          this.error = 'Failed to load cart';
          this.toast.error('Could not load cart items');
          this.cdr.markForCheck();
        }
      });
  }

  updateQuantity(itemId: string, currentQty: number, delta: number) {
    const newQty = currentQty + delta;
    if (newQty < 1) {
      this.removeItem(itemId);
      return;
    }
    
    this.loading = true;
    this.cartService.updateCartItem(itemId, newQty)
      .subscribe({
        next: () => {
          this.loading = false;
          this.toast.success('Cart updated');
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.toast.error('Failed to update quantity');
          this.cdr.markForCheck();
        }
      });
  }

  removeItem(itemId: string) {
    if (!confirm('Are you sure you want to remove this item?')) return;
    
    this.loading = true;
    this.cartService.removeCartItem(itemId)
      .subscribe({
        next: () => {
          this.loading = false;
          this.toast.success('Item removed from cart');
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.toast.error('Failed to remove item');
          this.cdr.markForCheck();
        }
      });
  }

  clearCart() {
    if (!confirm('Are you sure you want to clear your cart?')) return;

    this.loading = true;
    this.cartService.clearCart()
      .subscribe({
        next: () => {
          this.loading = false;
          this.toast.success('Cart cleared');
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.toast.error('Failed to clear cart');
          this.cdr.markForCheck();
        }
      });
  }

  checkout() {
    if (!this.cart || this.cart.items.length === 0) {
      this.toast.error('Your cart is empty');
      return;
    }
    this.router.navigate(['/cart/checkout']);
  }
}
