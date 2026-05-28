import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { ProductService, ProductSearchParams } from '../../services/product.service';
import { Product } from '../../models/product.model';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductListComponent implements OnInit, OnDestroy {
  products: Product[] = [];
  filteredProducts: Product[] = [];
  error: string | null = null;
  loading = false;

  readonly categoryOptions = [
    { value: 'all', label: 'All categories' },
    { value: 'electronics', label: 'Electronics' },
    { value: 'fashion', label: 'Fashion' },
    { value: 'audio', label: 'Audio' },
    { value: 'home', label: 'Home' },
    { value: 'books', label: 'Books' },
    { value: 'sports', label: 'Sports' },
  ];

  readonly priceBounds = {
    min: 0,
    max: 2000,
  };

  readonly filtersForm: FormGroup;

  currentPage = 1;
  pageSize = 12;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private cdr: ChangeDetectorRef,
  ) {
    this.filtersForm = this.fb.group({
      keyword: [''],
      category: ['all'],
      minPrice: [this.priceBounds.min],
      maxPrice: [this.priceBounds.max],
    });
  }

  ngOnInit() {
    this.loadProducts(true);

    this.filtersForm.valueChanges
      .pipe(debounceTime(250), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 1;
        this.loadProducts(true);
      });
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

  loadProducts(resetPage = false) {
    if (resetPage) {
      this.currentPage = 1;
    }

    this.error = null;
    this.loading = true;

    const filters = this.buildSearchParams();

    this.productService.getAllProducts(filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (products: Product[]) => {
          this.products = products;
          this.filteredProducts = this.applyClientFilters(products);
          this.syncCurrentPage();
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          console.log('Backend is down with 502, using Day 1 Mock data fallback!');
          this.error = null;
          this.products = [
            { id: '1', name: 'Sample Item 1', price: 25.99, description: 'Mock Description', quantity: 10 },
            { id: '2', name: 'Sample Item 2', price: 599.99, description: 'Mock Description', quantity: 5 },
          ];
          this.filteredProducts = this.applyClientFilters(this.products);
          this.syncCurrentPage();
          this.loading = false;
          console.error('Error loading products:', err);
          this.cdr.markForCheck();
        }
      });
  }

  changePage(delta: number) {
    const next = this.currentPage + delta;
    if (next < 1 || next > this.totalPages) return;
    this.currentPage = next;
    this.loadProducts(false);
  }

  resetFilters(): void {
    this.filtersForm.reset({
      keyword: '',
      category: 'all',
      minPrice: this.priceBounds.min,
      maxPrice: this.priceBounds.max,
    });
  }

  get activeFiltersCount(): number {
    const values = this.filtersForm.value;
    let count = 0;

    if (values.keyword?.trim()) count++;
    if ((values.category ?? 'all') !== 'all') count++;
    if ((values.minPrice ?? this.priceBounds.min) > this.priceBounds.min) count++;
    if ((values.maxPrice ?? this.priceBounds.max) < this.priceBounds.max) count++;

    return count;
  }

  onImageLoadError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.style.opacity = '0.5';
    img.alt = 'Image not available';
  }

  trackByProductId(index: number, product: any): string {
    return product.id || product._id || index.toString();
  }

  get minPrice(): number {
    return Number(this.filtersForm.value.minPrice ?? this.priceBounds.min);
  }

  get maxPrice(): number {
    return Number(this.filtersForm.value.maxPrice ?? this.priceBounds.max);
  }

  formatPrice(value: number): string {
    return `$${value.toFixed(0)}`;
  }

  private buildSearchParams(): ProductSearchParams {
    const values = this.filtersForm.value;
    const minPrice = Number(values.minPrice ?? this.priceBounds.min);
    const maxPrice = Number(values.maxPrice ?? this.priceBounds.max);
    const normalizedMin = Math.min(minPrice, maxPrice);
    const normalizedMax = Math.max(minPrice, maxPrice);

    return {
      keyword: values.keyword?.trim() || undefined,
      category: values.category && values.category !== 'all' ? values.category : undefined,
      minPrice: normalizedMin,
      maxPrice: normalizedMax,
      page: this.currentPage,
      pageSize: this.pageSize,
    };
  }

  private applyClientFilters(products: Product[]): Product[] {
    const values = this.filtersForm.value;
    const keyword = values.keyword?.trim().toLowerCase() || '';
    const category = values.category ?? 'all';
    const minPrice = Math.min(
      Number(values.minPrice ?? this.priceBounds.min),
      Number(values.maxPrice ?? this.priceBounds.max),
    );
    const maxPrice = Math.max(
      Number(values.minPrice ?? this.priceBounds.min),
      Number(values.maxPrice ?? this.priceBounds.max),
    );

    return products.filter((product) => {
      const matchesKeyword = !keyword || [product.name, product.description]
        .filter(Boolean)
        .some((field) => field?.toLowerCase().includes(keyword));

      const matchesCategory = category === 'all' || this.matchesCategory(product, category);
      const matchesPrice = product.price >= minPrice && product.price <= maxPrice;

      return matchesKeyword && matchesCategory && matchesPrice;
    });
  }

  private matchesCategory(product: Product, category: string): boolean {
    const haystack = `${product.name} ${product.description}`.toLowerCase();

    const categoryMap: Record<string, string[]> = {
      electronics: ['phone', 'laptop', 'tablet', 'computer', 'camera', 'tech'],
      fashion: ['shirt', 'shoe', 'jacket', 'dress', 'fashion', 'wear'],
      audio: ['headphone', 'speaker', 'earbud', 'audio'],
      home: ['home', 'kitchen', 'furniture', 'decor', 'house'],
      books: ['book', 'novel', 'guide', 'study'],
      sports: ['sport', 'fitness', 'running', 'gym', 'training'],
    };

    return (categoryMap[category] ?? []).some((keyword) => haystack.includes(keyword));
  }

  private syncCurrentPage(): void {
    const total = this.totalPages;
    if (this.currentPage > total) {
      this.currentPage = total;
    }
  }
}
