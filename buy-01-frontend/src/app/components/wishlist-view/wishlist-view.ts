import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { WishlistService } from '../../services/wishlist.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { WishlistItemDTO } from '../../models/wishlist.model';

@Component({
  selector: 'app-wishlist-view',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './wishlist-view.html',
  styleUrl: './wishlist-view.scss'
})
export class WishlistViewComponent implements OnInit, OnDestroy {
  items: WishlistItemDTO[] = [];
  loading = true;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private wishlistService: WishlistService,
    private cartService: CartService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.wishlistService.wishlist$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.items = items;
        this.cdr.markForCheck();
      });

    this.loadWishlist();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadWishlist() {
    this.loading = true;
    this.error = null;
    this.wishlistService.getUserWishlist()
      .subscribe({
        next: () => {
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.error = 'Failed to load wishlist items';
          this.cdr.markForCheck();
        }
      });
  }

  removeFromWishlist(productId: string) {
    this.wishlistService.removeFromWishlist(productId)
      .subscribe({
        next: () => {
          this.toast.success('Removed from Wishlist');
        },
        error: () => {
          this.toast.error('Failed to remove item');
        }
      });
  }

  addToCart(productId: string) {
    this.cartService.addToCart(productId, 1)
      .subscribe({
        next: () => {
          this.toast.success('Product added to Cart!');
          // Optionally remove from wishlist
          this.wishlistService.removeFromWishlist(productId).subscribe();
        },
        error: () => {
          this.toast.error('Failed to add product to Cart');
        }
      });
  }


  trackByItemId(index: number, item: WishlistItemDTO): string {
    return item.id;
  }
}
