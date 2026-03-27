import { Injectable } from '@angular/core';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly TOKEN_KEY = 'token';
  private readonly USER_KEY = 'user';

  private getActiveStorage(): Storage {
    if (localStorage.getItem(this.TOKEN_KEY) || localStorage.getItem(this.USER_KEY)) {
      return localStorage;
    }
    if (sessionStorage.getItem(this.TOKEN_KEY) || sessionStorage.getItem(this.USER_KEY)) {
      return sessionStorage;
    }
    return localStorage;
  }

  setToken(token: string, remember?: boolean): void {
    const storage = remember === undefined
      ? this.getActiveStorage()
      : (remember ? localStorage : sessionStorage);

    localStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.TOKEN_KEY);
    storage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY) ?? sessionStorage.getItem(this.TOKEN_KEY);
  }

  setUser(user: User, remember?: boolean): void {
    const storage = remember === undefined
      ? this.getActiveStorage()
      : (remember ? localStorage : sessionStorage);

    localStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    storage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  getUser(): User | null {
    const raw = localStorage.getItem(this.USER_KEY) ?? sessionStorage.getItem(this.USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  }

  clear(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): User | null {
    return this.getUser();
  }

  getRole(): User['role'] | null {
    return this.getUser()?.role ?? null;
  }
}
