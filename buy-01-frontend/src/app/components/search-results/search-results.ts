import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SearchService } from '../../services/search.service';
import { WishlistService } from '../../services/wishlist.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { ProductSearchDTO } from '../../models/product.model';
import { ProductFilterComponent, FilterParams } from '../product-filter/product-filter';

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductFilterComponent],
  templateUrl: './search-results.html',
  styleUrl: './search-results.scss'
})
export class SearchResultsComponent implements OnInit, OnDestroy {
  products: ProductSearchDTO[] = [];
  loading = true;
  error: string | null = null;
  totalProductsCount: number = 0;

  // Search/Filter parameters
  keyword: string = '';
  filters: FilterParams = {
    minPrice: undefined,
    maxPrice: undefined,
    sortBy: 'popularity'
  };

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private searchService: SearchService,
    private wishlistService: WishlistService,
    private cartService: CartService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.keyword = params.get('keyword') || '';
        const category = params.get('category');
        const min = params.get('minPrice');
        const max = params.get('maxPrice');
        const sort = params.get('sortBy');

        this.filters = {
          keyword: this.keyword,
          category: category || undefined,
          minPrice: min ? Number(min) : undefined,
          maxPrice: max ? Number(max) : undefined,
          sortBy: sort || 'popularity'
        };

        this.loadSearchResults();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSearchResults() {
    this.loading = true;
    this.error = null;

    const hasFilters = this.filters.minPrice !== undefined || 
                       this.filters.maxPrice !== undefined || 
                       (this.filters.category !== undefined && this.filters.category !== 'all');

    // Determine which API endpoint to consume based on search parameters
    if (this.keyword) {
      if (hasFilters) {
        this.searchService.searchAndFilter(this.keyword, undefined, undefined, undefined)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (allKeywordProds) => {
              this.totalProductsCount = allKeywordProds.length;
              this.cdr.markForCheck();
            }
          });
      }

      this.searchService.searchAndFilter(this.keyword, this.filters.category, this.filters.minPrice, this.filters.maxPrice)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res) => {
            this.products = res;
            if (!hasFilters) {
              this.totalProductsCount = res.length;
            }
            this.sortProductsLocally();
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.error = 'Failed to fetch search results';
            this.cdr.markForCheck();
          }
        });
    } else {
      if (hasFilters) {
        this.searchService.getFilteredAndSorted(undefined, undefined, undefined, this.filters.sortBy)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (allCatalogProds) => {
              this.totalProductsCount = allCatalogProds.length;
              this.cdr.markForCheck();
            }
          });
      }

      this.searchService.getFilteredAndSorted(this.filters.category, this.filters.minPrice, this.filters.maxPrice, this.filters.sortBy)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res) => {
            this.products = res;
            if (!hasFilters) {
              this.totalProductsCount = res.length;
            }
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.error = 'Failed to fetch filtered catalog';
            this.cdr.markForCheck();
          }
        });
    }
  }

  onFilterChange(newFilters: FilterParams) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        keyword: newFilters.keyword !== undefined ? newFilters.keyword : null,
        category: newFilters.category !== undefined ? newFilters.category : null,
        minPrice: newFilters.minPrice !== undefined ? newFilters.minPrice : null,
        maxPrice: newFilters.maxPrice !== undefined ? newFilters.maxPrice : null,
        sortBy: newFilters.sortBy !== 'popularity' ? newFilters.sortBy : null
      },
      queryParamsHandling: 'merge'
    });
  }

  // Local helper sorting since search-and-filter endpoint does not support sorting natively
  sortProductsLocally() {
    if (!this.filters.sortBy || this.filters.sortBy === 'popularity') return;

    if (this.filters.sortBy === 'price_asc') {
      this.products.sort((a, b) => a.price - b.price);
    } else if (this.filters.sortBy === 'price_desc') {
      this.products.sort((a, b) => b.price - a.price);
    } else if (this.filters.sortBy === 'name') {
      this.products.sort((a, b) => a.name.localeCompare(b.name));
    }
  }

  addToCart(productId: string, quantityInput: string) {
    const qty = parseInt(quantityInput, 10) || 1;
    this.cartService.addToCart(productId, qty)
      .subscribe({
        next: () => {
          this.toast.success('Added to Cart');
        },
        error: () => {
          this.toast.error('Failed to add to Cart');
        }
      });
  }

  toggleWishlist(productId: string) {
    if (this.isInWishlist(productId)) {
      this.wishlistService.removeFromWishlist(productId).subscribe({
        next: () => this.toast.success('Removed from Wishlist'),
        error: () => this.toast.error('Failed to remove')
      });
    } else {
      this.wishlistService.addToWishlist(productId).subscribe({
        next: () => this.toast.success('Saved to Wishlist'),
        error: () => this.toast.error('Failed to add')
      });
    }
  }

  isInWishlist(productId: string): boolean {
    return this.wishlistService.isInWishlist(productId);
  }

  onImageLoadError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.style.opacity = '0.5';
    img.alt = 'Image unavailable';
  }

  getQtyRange(quantity: number): number[] {
    const max = Math.max(1, Math.min(quantity, 100));
    return Array.from({ length: max }, (_, i) => i + 1);
  }

  trackByProductId(index: number, product: ProductSearchDTO): string {
    return product.id;
  }
}
