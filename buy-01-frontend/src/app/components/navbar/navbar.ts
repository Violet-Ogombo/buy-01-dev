import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { WishlistService } from '../../services/wishlist.service';
import { CommonModule } from '@angular/common';
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
  cartCount = 0;
  wishlistCount = 0;

  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.user = this.authService.getCurrentUser();
  }

  ngOnInit() {
    // Listen to route changes to update user state
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.user = this.authService.getCurrentUser();
        this.loadNavbarCounts();
        this.cdr.markForCheck();
      });

    // Subscribe to Cart changes
    this.cartService.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe(cart => {
        this.cartCount = cart ? cart.itemCount : 0;
        this.cdr.markForCheck();
      });

    // Subscribe to Wishlist changes
    this.wishlistService.wishlist$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.wishlistCount = items ? items.length : 0;
        this.cdr.markForCheck();
      });

    this.loadNavbarCounts();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadNavbarCounts() {
    if (this.authService.isAuthenticated()) {
      this.cartService.getCart().subscribe();
      this.wishlistService.getUserWishlist().subscribe();
    }
  }

  searchProducts(keyword: string) {
    const trimmed = keyword.trim();
    this.router.navigate(['/search'], { queryParams: { keyword: trimmed || null } });
  }

  logout() {
    this.authService.logout();
    this.user = null;
    this.cartCount = 0;
    this.wishlistCount = 0;
    this.cdr.markForCheck();
    this.router.navigate(['/login']);
  }
}
