import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductService } from '../../services/product.service';
import { MediaService } from '../../services/media.service';
import { Media } from '../../models/media.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-product-form',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss'
})
export class ProductFormComponent implements OnInit {
  productForm: FormGroup;
  isEditMode = false;
  productId: string | null = null;
  loading = false;
  error: string | null = null;
  uploadedImages: Media[] = [];
  primaryImageId: string | null = null;
  isDragOver = false;
  uploading = false;
  private pendingUploads = 0;
  pendingFiles: File[] = [];
  previewUrls: string[] = [];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private mediaService: MediaService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
      price: [0, [Validators.required, Validators.min(0.01)]],
      quantity: [0, [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit() {
    this.productId = this.route.snapshot.paramMap.get('id');
    if (this.productId) {
      this.isEditMode = true;
      this.loadProduct(this.productId);
    }
  }

  loadProduct(id: string) {
    this.loading = true;
    this.productService.getProduct(id).subscribe({
      next: (product: any) => {
        this.productForm.patchValue({
          name: product.name,
          description: product.description,
          price: product.price,
          quantity: product.quantity
        });
        this.loadProductImages();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.error = 'Failed to load product';
        this.loading = false;
        console.error('Error loading product:', err);
        this.cdr.markForCheck();
      }
    });
  }

  onSubmit() {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = null;
    const productData = this.productForm.value;

    const operation = this.isEditMode && this.productId
      ? this.productService.updateProduct(this.productId, {
          ...productData,
          ...(this.uploadedImages.length
            ? {
                imageUrls: this.getOrderedImageUrls(),
              }
            : {}),
        })
      : this.productService.createProduct(productData);

    operation.subscribe({
      next: (createdProduct: any) => {
        if (!this.isEditMode && this.pendingFiles.length > 0) {
          // For new products, upload pending files
          this.productId = createdProduct.id || null;
          this.uploadPendingFiles(() => {
            this.toast.success('Product created with images successfully');
            this.loading = false;
            this.cdr.markForCheck();
            this.router.navigate(['/seller/dashboard']);
          });
        } else {
          this.toast.success(this.isEditMode ? 'Product updated successfully' : 'Product created successfully');
          this.loading = false;
          this.cdr.markForCheck();
          this.router.navigate(['/seller/dashboard']);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.isEditMode ? 'Failed to update product' : 'Failed to create product';
        this.toast.error(this.error);
        this.loading = false;
        console.error('Error saving product:', err);
        this.cdr.markForCheck();
      }
    });
  }

  cancel() {
    this.router.navigate(['/seller/dashboard']);
  }

  getFieldError(fieldName: string): string {
    const field = this.productForm.get(fieldName);
    if (field?.touched && field?.errors) {
      if (field.errors['required']) return `${fieldName} is required`;
      if (field.errors['minlength']) return `${fieldName} is too short`;
      if (field.errors['maxlength']) return `${fieldName} is too long`;
      if (field.errors['min']) return `${fieldName} must be greater than ${field.errors['min'].min}`;
    }
    return '';
  }

  onImageUpload(event: Event) {
    const files = (event.target as HTMLInputElement).files;
    if (!files) return;
    this.handleFiles(files);
  }

  getImageUrl(imageId: string): string {
    return this.mediaService.getImageUrl(imageId);
  }

  deleteImage(imageId: string) {
    // Optimistically remove from UI immediately
    this.uploadedImages = this.uploadedImages.filter(img => img.id !== imageId);
    if (this.primaryImageId === imageId) {
      this.primaryImageId = this.uploadedImages.length ? this.uploadedImages[0].id : null;
    }
    this.cdr.markForCheck();

    // Delete from backend
    this.mediaService.deleteImage(imageId).subscribe({
      next: () => {
        // If in edit mode, also update the product to remove the image URL
        if (this.isEditMode && this.productId) {
          const updatedProduct = {
            ...this.productForm.value,
            imageUrls: this.getOrderedImageUrls()
          };
          this.productService.updateProduct(this.productId, updatedProduct).subscribe({
            next: () => {
              this.toast.success('Image deleted successfully');
              this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
              console.error('Failed to update product after image deletion:', err);
              this.toast.error('Image deleted but failed to save product changes');
            }
          });
        } else {
          this.toast.success('Image deleted');
        }
      },
      error: (err: HttpErrorResponse) => {
        // Revert the optimistic removal on error
        this.uploadedImages.push({ id: imageId } as Media);
        this.cdr.markForCheck();
        this.error = 'Failed to delete image';
        this.toast.error('Failed to delete image');
        console.error('Delete error:', err);
      }
    });
  }

  private loadProductImages() {
    if (this.productId) {
      this.mediaService.getProductImages(this.productId).subscribe({
        next: (images: Media[]) => {
          this.uploadedImages = images;
          this.primaryImageId = images.length ? images[0].id : null;
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error loading images:', err);
          this.cdr.markForCheck();
        }
      });
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    if (!event.dataTransfer || !event.dataTransfer.files.length) return;
    this.handleFiles(event.dataTransfer.files);
  }

  setPrimaryImage(id: string) {
    this.primaryImageId = id;
  }

  trackByImageId(index: number, img: Media): string {
    return img.id;
  }

  onImageLoadError(event: Event): void {
    const img = event.target as HTMLImageElement;
    console.warn('Failed to load image:', img.src);
    // Optionally set a placeholder or hide the broken image
    img.style.opacity = '0.5';
    img.alt = 'Image not available';
  }

  private handleFiles(files: FileList) {
    this.error = null;

    Array.from(files).forEach((file) => {
      const validationError = this.mediaService.validateFile(file);
      if (validationError) {
        this.error = validationError;
        this.toast.info(validationError);
        return;
      }

      if (this.productId) {
        // Editing existing product - upload immediately
        this.pendingUploads++;
        this.uploading = true;

        this.mediaService.uploadImage(file, this.productId!).subscribe({
          next: (media: Media) => {
            this.uploadedImages.push(media);
            if (!this.primaryImageId) {
              this.primaryImageId = media.id;
            }
            this.toast.success('Image uploaded');
            this.cdr.markForCheck();
          },
          error: (err: HttpErrorResponse) => {
            this.error = 'Failed to upload image';
            this.toast.error('Failed to upload image');
            console.error('Upload error:', err);
            this.cdr.markForCheck();
          },
          complete: () => {
            this.pendingUploads--;
            if (this.pendingUploads === 0) {
              this.uploading = false;
              this.cdr.markForCheck();
            }
          }
        });
      } else {
        // Creating new product - store file for later upload
        this.pendingFiles.push(file);
        this.generatePreview(file);
        this.toast.success('Image selected (will be uploaded when you save)');
        this.cdr.markForCheck();
      }
    });
  }

  private generatePreview(file: File) {
    const reader = new FileReader();
    reader.onload = (e: ProgressEvent<FileReader>) => {
      if (e.target?.result) {
        this.previewUrls.push(e.target.result as string);
        this.cdr.markForCheck();
      }
    };
    reader.readAsDataURL(file);
  }

  private uploadPendingFiles(onComplete: () => void) {
    if (this.pendingFiles.length === 0) {
      onComplete();
      return;
    }

    let uploadCount = 0;
    this.pendingFiles.forEach((file) => {
      this.mediaService.uploadImage(file, this.productId!).subscribe({
        next: (media: Media) => {
          this.uploadedImages.push(media);
          uploadCount++;
          if (uploadCount === this.pendingFiles.length) {
            this.pendingFiles = [];
            this.previewUrls = [];
            onComplete();
          }
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error uploading file:', err);
          uploadCount++;
          if (uploadCount === this.pendingFiles.length) {
            this.pendingFiles = [];
            this.previewUrls = [];
            onComplete();
          }
        }
      });
    });
  }

  removePendingFile(index: number) {
    this.pendingFiles.splice(index, 1);
    this.previewUrls.splice(index, 1);
    this.cdr.markForCheck();
  }

  private getOrderedImageUrls(): string[] {
    if (!this.uploadedImages.length) return [];

    const images = [...this.uploadedImages];
    if (this.primaryImageId) {
      images.sort((a, b) => {
        if (a.id === this.primaryImageId) return -1;
        if (b.id === this.primaryImageId) return 1;
        return 0;
      });
    }

    return images.map((img) => this.mediaService.getImageUrl(img.id));
  }
}
