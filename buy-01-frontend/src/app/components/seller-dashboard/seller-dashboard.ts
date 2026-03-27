import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductService } from '../../services/product.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Product } from '../../models/product.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-seller-dashboard',
  imports: [CommonModule, RouterModule],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.scss'
})
export class SellerDashboardComponent implements OnInit, OnDestroy {
  products: Product[] = [];
  loading = true;
  error: string | null = null;

  currentPage = 1;
  pageSize = 10;
  private destroy$ = new Subject<void>();

  constructor(
    private productService: ProductService,
    private authService: AuthService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadMyProducts();
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

  loadMyProducts() {
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

  createProduct() {
    this.router.navigate(['/seller/products/new']);
  }

  editProduct(id: string) {
    this.router.navigate(['/seller/products/edit', id]);
  }

  deleteProduct(id: string) {
    if (confirm('Are you sure you want to delete this product?')) {
      this.productService.deleteProduct(id).subscribe({
        next: () => {
          this.loadMyProducts();
          this.toast.success('Product deleted');
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.error = 'Failed to delete product';
          this.toast.error('Failed to delete product');
          console.error('Error deleting product:', err);
          this.cdr.markForCheck();
        }
      });
    }
  }

  changePage(delta: number) {
    const next = this.currentPage + delta;
    if (next < 1 || next > this.totalPages) return;
    this.currentPage = next;
  }

  onImageLoadError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.style.opacity = '0.5';
    img.alt = 'Image not available';
  }

  trackByProductId(index: number, product: any): string {
    return product.id || product._id || index.toString();
  }
}
