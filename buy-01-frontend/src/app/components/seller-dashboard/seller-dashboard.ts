import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe, CurrencyPipe } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductService } from '../../services/product.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Product, SellerProduct } from '../../models/product.model';
import { User } from '../../models/user.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface SalesTrackingItem {
  productId: string;
  productName: string;
  imageUrl?: string;
  unitPrice: number;
  salesCount: number;
  productRevenue: number;
  hasActiveSales: boolean;
}

@Component({
  selector: 'app-seller-dashboard',
  imports: [CommonModule, RouterModule, DecimalPipe, CurrencyPipe],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.scss'
})
export class SellerDashboardComponent implements OnInit, OnDestroy {
  products: Product[] = [];
  currentUser: User | null = null;
  salesTrackingData: SalesTrackingItem[] = [];
  topPerformingProducts: SellerProduct[] = [];
  totalSellerRevenue: number = 0;
  loadingPerformance: boolean = true;
  loading: boolean = true;
  error: string | null = null;

  currentPage: number = 1;
  pageSize: number = 10;
  private destroy$: Subject<void> = new Subject<void>();

  constructor(
    private productService: ProductService,
    private authService: AuthService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadMyProducts();
    if (this.isSellerUser()) {
      this.loadSellerPerformance();
    }
  }

  private isSellerUser(): boolean {
    return this.currentUser?.role === 'SELLER';
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.products.length / this.pageSize));
  }

  get paginatedProducts(): Product[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.products.slice(start, start + this.pageSize);
  }

  private loadMyProducts(): void {
    this.loading = true;
    this.error = null;
    this.cdr.markForCheck();
    const user = this.authService.getCurrentUser();
    
    this.productService.getAllProducts()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (products: Product[]) => {
          this.products = products.filter(p => p.userId === user?.id);
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          this.error = 'Failed to load products';
          this.loading = false;
          console.error('Error loading products:', err);
          this.cdr.markForCheck();
        }
      });
  }

  private loadSellerPerformance(): void {
    this.loadingPerformance = true;
    this.cdr.markForCheck();
    this.calculateSellerMetrics();
  }



  calculateSellerMetrics(): void {
    try {
      // Initialize sales tracking and performance data
      this.totalSellerRevenue = 0;
      const performanceMap = new Map<string, SellerProduct>();
      this.salesTrackingData = [];

      // Process each product for sales tracking
      this.products.forEach(product => {
        const salesCount: number = product.salesCount ?? 0;
        const unitPrice: number = product.price ?? 0;
        const productRevenue: number = salesCount * unitPrice;

        // Add to sales tracking
        this.salesTrackingData.push({
          productId: product.id || '',
          productName: product.name,
          imageUrl: product.imageUrls?.[0],
          unitPrice: unitPrice,
          salesCount: salesCount,
          productRevenue: productRevenue,
          hasActiveSales: salesCount > 0
        });

        // Accumulate total revenue
        this.totalSellerRevenue += productRevenue;

        // Track top performing products
        if (productRevenue > 0) {
          performanceMap.set(product.id || '', {
            productId: product.id || '',
            productName: product.name,
            salesCount: salesCount,
            revenue: productRevenue,
            imageUrl: product.imageUrls?.[0]
          });
        }
      });

      // Sort sales tracking by revenue descending
      this.salesTrackingData.sort((a, b) => b.productRevenue - a.productRevenue);

      // Sort and get top 5 performing products
      this.topPerformingProducts = Array.from(performanceMap.values())
        .sort((a, b) => b.revenue - a.revenue)
        .slice(0, 5);

      // Override with user profile total if available for consistency
      if (this.currentUser?.sellerRevenue !== undefined && this.currentUser.sellerRevenue > 0) {
        this.totalSellerRevenue = this.currentUser.sellerRevenue;
      }

      this.loadingPerformance = false;
      this.cdr.markForCheck();
    } catch (err) {
      console.error('Error calculating seller metrics:', err);
      this.loadingPerformance = false;
      this.cdr.markForCheck();
    }
  }

  createProduct() {
    this.router.navigate(['/seller/products/new']);
  }

  editProduct(id: string) {
    this.router.navigate(['/seller/products/edit', id]);
  }

  deleteProduct(id: string): void {
    if (confirm('Are you sure you want to delete this product?')) {
      this.productService.deleteProduct(id).subscribe({
        next: () => {
          this.loadMyProducts();
          if (this.isSellerUser()) {
            this.loadSellerPerformance();
          }
          this.toast.success('Product deleted');
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          this.error = 'Failed to delete product';
          this.toast.error('Failed to delete product');
          console.error('Error deleting product:', err);
          this.cdr.markForCheck();
        }
      });
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  getSalesStatusBadgeClass(hasActiveSales: boolean): string {
    return hasActiveSales ? 'badge-active-sales' : 'badge-no-sales';
  }

  getSalesStatusText(hasActiveSales: boolean): string {
    return hasActiveSales ? 'Active Sales' : 'No Sales Yet';
  }

  changePage(delta: number): void {
    const next = this.currentPage + delta;
    if (next < 1 || next > this.totalPages) return;
    this.currentPage = next;
  }

  onImageLoadError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.opacity = '0.5';
    img.alt = 'Image not available';
  }

  trackByProductId(index: number, product: Product | SalesTrackingItem): string {
    if ('productId' in product) {
      return product.productId || index.toString();
    }
    return product.id || index.toString();
  }

  trackBySalesItem(index: number, item: SalesTrackingItem): string {
    return item.productId || index.toString();
  }
}
