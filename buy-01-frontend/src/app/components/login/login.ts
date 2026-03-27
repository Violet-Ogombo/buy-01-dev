import { Component, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  form: FormGroup;
  error: string | null = null;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      rememberMe: [true]
    });
  }

  submit() {
    if (this.form.invalid || this.loading) return;
    const { email, password, rememberMe } = this.form.value;
    this.loading = true;
    this.cdr.markForCheck();
    this.authService.login({ email, password }).subscribe({
      next: (user) => {
        this.authService.setToken(user.token, rememberMe);
        this.authService.setUser(user, rememberMe);
        this.cdr.markForCheck();

        if (user.role === 'SELLER') {
          this.router.navigate(['/seller']);
        } else {
          this.router.navigate(['/products']);
        }
        this.toast.success('Logged in successfully');
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'Login failed';
        if (this.error) {
          this.toast.error(this.error);
        }
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }
}
