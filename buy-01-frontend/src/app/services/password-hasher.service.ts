import { Injectable } from '@angular/core';

/**
 * Password hashing service that uses Web Crypto API.
 * Hashes passwords with SHA-256 before sending to backend.
 * Ensures plaintext passwords never leave the browser.
 */
@Injectable({ providedIn: 'root' })
export class PasswordHasherService {

  /**
   * Hash a password using SHA-256
   * @param password plaintext password
   * @returns Promise<string> hex-encoded SHA-256 hash
   */
  async hashPassword(password: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(password);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  }
}
