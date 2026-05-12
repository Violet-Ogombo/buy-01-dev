import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private session: SessionService
  ) {}

  login(credentials: { email: string; password: string }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/login`, credentials);
  }

  register(data: { name: string; email: string; password: string; role: string; avatar?: string }): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/auth/register`, data);
  }

  logout(): void {
    this.session.clear();
  }

  setToken(token: string, remember?: boolean): void {
    this.session.setToken(token, remember);
  }

  getToken(): string | null {
    return this.session.getToken();
  }

  setUser(user: User, remember?: boolean): void {
    this.session.setUser(user, remember);
  }

  getUser(): User | null {
    return this.session.getUser();
  }

  getCurrentUser(): User | null {
    return this.session.getCurrentUser();
  }

  isAuthenticated(): boolean {
    return this.session.isAuthenticated();
  }
}
