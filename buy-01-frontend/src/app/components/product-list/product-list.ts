import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { WishlistService } from '../../services/wishlist.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { ProductFilterComponent, FilterParams } from '../product-filter/product-filter';
import { Product } from '../../models/product.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ProductFilterComponent],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductListComponent implements OnInit, OnDestroy {
  allProducts: Product[] = [];
  filteredProducts: Product[] = [];
  error: string | null = null;
  currentFilters: FilterParams = {};

  currentPage = 1;
  pageSize = 12;
  skeletonItems: number[] = Array.from({ length: 6 }, (_, i) => i);

  private destroy$ = new Subject<void>();

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private authService: AuthService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadProducts();
    if (this.authService.isAuthenticated()) {
      this.wishlistService.getUserWishlist().subscribe();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredProducts.length / this.pageSize));
  }

  get paginatedProducts(): Product[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredProducts.slice(start, start + this.pageSize);
  }

  get displayedProductsCount(): number {
    return this.filteredProducts.length;
  }

  loadProducts() {
    this.error = null;
    this.productService.getAllProducts()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (products: Product[]) => {
          this.allProducts = products;
          this.applyFilters();
          this.currentPage = 1;
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          this.error = 'Failed to load products';
          console.error('Error loading products:', err);
          this.cdr.markForCheck();
        }
      });
  }

  applyFilters() {
    let filtered = [...this.allProducts];

    // Filter by keyword (search in name and description)
    if (this.currentFilters.keyword) {
      const keyword = this.currentFilters.keyword.toLowerCase();
      filtered = filtered.filter(p =>
        p.name.toLowerCase().includes(keyword) ||
        (p.description && p.description.toLowerCase().includes(keyword))
      );
    }

    // Filter by category
    if (this.currentFilters.category && this.currentFilters.category !== 'all') {
      // If you have a category field on products, filter by it
      // Otherwise, this is a placeholder for future category filtering
      // filtered = filtered.filter(p => p.category === this.currentFilters.category);
    }

    // Filter by price range
    if (this.currentFilters.minPrice !== undefined) {
      filtered = filtered.filter(p => p.price >= this.currentFilters.minPrice!);
    }
    if (this.currentFilters.maxPrice !== undefined) {
      filtered = filtered.filter(p => p.price <= this.currentFilters.maxPrice!);
    }

    this.filteredProducts = filtered;
    this.currentPage = 1;
  }

  onFilterChange(filters: FilterParams) {
    this.currentFilters = filters;
    this.applyFilters();
    this.cdr.markForCheck();
  }

  changePage(delta: number) {
    const next = this.currentPage + delta;
    if (next < 1 || next > this.totalPages) return;
    this.currentPage = next;
  }

  addToCart(product: Product, event: Event, quantityInput: string) {
    event.stopPropagation();
    if (!this.authService.isAuthenticated()) {
      this.toast.info('Please log in to add items to your cart.');
      return;
    }
    const qty = parseInt(quantityInput, 10) || 1;
    this.cartService.addToCart(product.id!, qty).subscribe({
      next: () => this.toast.success(`Added ${product.name} to Cart`),
      error: () => this.toast.error('Failed to add product to Cart')
    });
  }

  toggleWishlist(product: Product, event: Event) {
    event.stopPropagation();
    if (!this.authService.isAuthenticated()) {
      this.toast.info('Please log in to manage your wishlist.');
      return;
    }

    const productId = product.id!;
    if (this.isInWishlist(productId)) {
      this.wishlistService.removeFromWishlist(productId).subscribe({
        next: () => {
          this.toast.success('Removed from Wishlist');
          this.cdr.markForCheck();
        },
        error: () => this.toast.error('Failed to remove from Wishlist')
      });
    } else {
      this.wishlistService.addToWishlist(productId).subscribe({
        next: () => {
          this.toast.success('Saved to Wishlist');
          this.cdr.markForCheck();
        },
        error: () => this.toast.error('Failed to add to Wishlist')
      });
    }
  }

  isInWishlist(productId: string): boolean {
    return this.wishlistService.isInWishlist(productId);
  }

  isUserLoggedIn(): boolean {
    return this.authService.isAuthenticated();
  }

  onImageLoadError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.style.opacity = '0.5';
    img.alt = 'Image not available';
  }

  trackByProductId(index: number, product: any): string {
    return product.id || product._id || index.toString();
  }

  get products(): Product[] {
    return this.filteredProducts;
  }
}
