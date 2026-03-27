import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Media } from '../models/media.model';

@Injectable({ providedIn: 'root' })
export class MediaService {
  private apiUrl = `${environment.apiUrl}/media/images`;

  constructor(private http: HttpClient) {}

  uploadImage(file: File, productId: string): Observable<Media> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('productId', productId);
    return this.http.post<Media>(this.apiUrl, formData);
  }

  getImageUrl(id: string): string {
    return `${this.apiUrl}/${id}`;
  }

  getProductImages(productId: string): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/product/${productId}`);
  }

  deleteImage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getMyUploads(): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/my-uploads`);
  }

  validateFile(file: File): string | null {
    const maxSize = 2 * 1024 * 1024; // 2MB
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    
    if (file.size > maxSize) {
      return 'File size exceeds 2MB limit';
    }
    
    if (!allowedTypes.includes(file.type)) {
      return 'Invalid file type. Only JPEG, PNG, GIF, and WEBP images are allowed';
    }
    
    return null;
  }
}
