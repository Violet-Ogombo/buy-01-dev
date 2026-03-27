import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MediaService } from '../../services/media.service';
import { Media } from '../../models/media.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-seller-media',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-media.html',
  styleUrl: './seller-media.scss',
})
export class SellerMediaComponent implements OnInit {
  media: Media[] = [];
  filteredMedia: Media[] = [];
  filter: 'all' | 'linked' | 'unlinked' = 'all';
  loading = false;
  error: string | null = null;

  currentPage = 1;
  pageSize = 12;

  uploading = false;
  private pendingUploads = 0;

  isDragOver = false;
  previewUrl: string | null = null;
  showPreview = false;
  copyMessage: string | null = null;

  constructor(private mediaService: MediaService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadMedia();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredMedia.length / this.pageSize));
  }

  get paginatedMedia(): Media[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredMedia.slice(start, start + this.pageSize);
  }

  loadMedia(): void {
    this.loading = true;
    this.error = null;

    this.mediaService.getMyUploads().subscribe({
      next: (items) => {
        this.media = items;
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load media';
        this.loading = false;
      }
    });
  }

  setFilter(filter: 'all' | 'linked' | 'unlinked'): void {
    this.filter = filter;
    this.applyFilter();
  }

  private applyFilter(): void {
    if (this.filter === 'linked') {
      this.filteredMedia = this.media.filter(m => !!m.productId);
    } else if (this.filter === 'unlinked') {
      this.filteredMedia = this.media.filter(m => !m.productId);
    } else {
      this.filteredMedia = [...this.media];
    }

    this.currentPage = 1;
  }

  changePage(delta: number): void {
    const next = this.currentPage + delta;
    if (next < 1 || next > this.totalPages) return;
    this.currentPage = next;
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    this.uploadFiles(input.files);
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    if (!event.dataTransfer || !event.dataTransfer.files.length) return;
    this.uploadFiles(event.dataTransfer.files);
  }

  private uploadFiles(files: FileList): void {
    this.error = null;

    Array.from(files).forEach((file) => {
      const validationError = this.mediaService.validateFile(file);
      if (validationError) {
        this.error = validationError;
        this.toast.info(validationError);
        return;
      }

      this.pendingUploads++;
      this.uploading = true;

      this.mediaService.uploadImage(file, '').subscribe({
        next: (media) => {
          this.media.unshift(media);
          this.applyFilter();
          this.toast.success('Image uploaded');
        },
        error: () => {
          this.error = 'Failed to upload image';
          this.toast.error('Failed to upload image');
        },
        complete: () => {
          this.pendingUploads--;
          if (this.pendingUploads === 0) {
            this.uploading = false;
          }
        }
      });
    });
  }

  getImageUrl(media: Media): string {
    return this.mediaService.getImageUrl(media.id);
  }

  preview(media: Media): void {
    this.previewUrl = this.getImageUrl(media);
    this.showPreview = true;
  }

  closePreview(): void {
    this.showPreview = false;
    this.previewUrl = null;
  }

  copyUrl(media: Media): void {
    const url = this.getImageUrl(media);

    if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(() => {
        this.copyMessage = 'Copied URL to clipboard';
        setTimeout(() => (this.copyMessage = null), 2000);
      }).catch(() => {
        this.copyMessage = 'Failed to copy URL';
        setTimeout(() => (this.copyMessage = null), 2000);
      });
    } else {
      this.copyMessage = 'Copy not supported in this browser';
      setTimeout(() => (this.copyMessage = null), 2000);
    }
  }

  delete(media: Media): void {
    if (!confirm('Delete this image?')) return;

    this.mediaService.deleteImage(media.id).subscribe({
      next: () => {
        this.media = this.media.filter(m => m.id !== media.id);
        this.applyFilter();
        this.toast.success('Image deleted');
      },
      error: () => {
        this.error = 'Failed to delete image';
        this.toast.error('Failed to delete image');
      }
    });
  }
}
