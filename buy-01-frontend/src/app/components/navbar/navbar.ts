import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { CommonModule } from '@angular/common';
import { ShoppingCart } from '../../models/cart.model';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit, OnDestroy {
  user: any = null;
  cart: ShoppingCart | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.user = this.authService.getCurrentUser();
  }

  ngOnInit() {
    this.loadCart();

    // Listen to route changes to update user state
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.user = this.authService.getCurrentUser();
        this.loadCart();
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  logout() {
    this.authService.logout();
    this.user = null;
    this.cart = null;
    this.cdr.markForCheck();
    this.router.navigate(['/login']);
  }

  get cartItemCount(): number {
    return this.cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;
  }

  private loadCart(): void {
    this.cartService.getCart().pipe(takeUntil(this.destroy$)).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.cdr.markForCheck();
      }
    });
  }
}
