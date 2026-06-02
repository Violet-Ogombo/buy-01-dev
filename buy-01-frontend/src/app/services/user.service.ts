import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';
import { SellerProfile } from '../models/seller-profile.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  updateProfile(payload: { 
    name?: string; 
    email?: string; 
    oldPassword?: string; 
    newPassword?: string 
  }): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/me`, payload);
  }

  getSellerProfile(sellerId: string): Observable<SellerProfile> {
    return this.http.get<SellerProfile>(`/api/v1/sellers/${sellerId}/profile`);
  }
}
