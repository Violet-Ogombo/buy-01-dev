import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { Product } from '../../models/product.model';
import { ShoppingCart } from '../../models/cart.model';
import { Subject } from 'rxjs';
import { takeUntil, switchMap, tap, filter } from 'rxjs/operators';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  product: Product | null = null;
  loading = true;
  error: string | null = null;
  notFound = false;
  cart: ShoppingCart | null = null;
  selectedQuantity = 1;
  cartMessage: string | null = null;

  selectedImageUrl: string | null = null;
  showImageFullscreen = false;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cartService: CartService,
    private cdr: ChangeDetectorRef,
  ) {}

  protected get routeRef() {
    return this.route;
  }

  ngOnInit(): void {
    console.log('[ProductDetail] ngOnInit - subscribing to route param changes');
    this.loadCart();
    
    this.route.paramMap
      .pipe(
        takeUntil(this.destroy$),
        tap(paramMap => {
          const id = paramMap.get('id');
          console.log('[ProductDetail] Route param changed - ID:', id);
          
          if (!id) {
            this.error = 'No product id provided';
            this.loading = false;
            console.error('[ProductDetail] No product ID provided');
          } else {
            // Set loading state BEFORE the request
            this.loading = true;
            this.error = null;
            this.notFound = false;
          }
          this.cdr.markForCheck();
        }),
        filter(paramMap => !!paramMap.get('id')),
        switchMap(paramMap => {
          const id = paramMap.get('id');
          console.log('[ProductDetail] Fetching product with ID:', id);
          return this.productService.getProduct(id as string);
        })
      )
      .subscribe({
        next: (product) => {
          console.log('[ProductDetail] Product loaded successfully:', product);
          this.product = product;
          const images = product.imageUrls || [];
          this.selectedImageUrl = images.length ? images[0] : null;
          this.loading = false;
          this.error = null;
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          console.error('[ProductDetail] Error loading product:', err);
          if (err?.status === 404) {
            this.notFound = true;
            this.error = 'Product not found';
          } else {
            this.error = 'Failed to load product: ' + (err?.message || 'Unknown error');
          }
          this.loading = false;
          this.cdr.markForCheck();
        },
        complete: () => {
          console.log('[ProductDetail] Observable completed');
        }
      });
  }

  ngOnDestroy(): void {
    console.log('[ProductDetail] ngOnDestroy - cleaning up');
    this.destroy$.next();
    this.destroy$.complete();
  }

  public loadProduct(id: string): void {
    console.log('[ProductDetail] loadProduct (retry) called with ID:', id);
    if (!id) {
      console.error('[ProductDetail] No ID provided for retry');
      return;
    }

    this.loading = true;
    this.error = null;
    this.notFound = false;

    this.productService.getProduct(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (product) => {
          console.log('[ProductDetail] Product loaded successfully (retry):', product);
          this.product = product;
          const images = product.imageUrls || [];
          this.selectedImageUrl = images.length ? images[0] : null;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          console.error('[ProductDetail] Error loading product (retry):', err);
          if (err?.status === 404) {
            this.notFound = true;
            this.error = 'Product not found';
          } else {
            this.error = 'Failed to load product: ' + (err?.message || 'Unknown error');
          }
          this.loading = false;
          this.cdr.markForCheck();
        }
      });
  }

  selectImage(url: string): void {
    this.selectedImageUrl = url;
  }

  openFullscreen(): void {
    if (!this.selectedImageUrl) return;
    this.showImageFullscreen = true;
  }

  closeFullscreen(): void {
    this.showImageFullscreen = false;
  }

  onImageLoadError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.style.opacity = '0.5';
    img.alt = 'Image not available';
  }

  addToCart(): void {
    if (!this.product || !this.product.id) {
      return;
    }

    const quantity = Math.max(1, Math.floor(this.selectedQuantity || 1));

    this.cartService.addToCart(this.product.id, quantity).pipe(takeUntil(this.destroy$)).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.cartMessage = `${quantity} item${quantity === 1 ? '' : 's'} added to cart`;
        this.cdr.markForCheck();
      }
    });
  }

  get cartItemCount(): number {
    return this.cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;
  }

  get cartTotal(): number {
    return this.cart?.totalAmount ?? 0;
  }

  private loadCart(): void {
    this.cartService.getCart().pipe(takeUntil(this.destroy$)).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.cdr.markForCheck();
      }
    });
  }

  trackByImageUrl(index: number, url: string): string {
    return url || index.toString();
  }
}
