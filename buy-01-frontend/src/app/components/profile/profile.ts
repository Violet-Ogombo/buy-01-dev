import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnInit {
  form: FormGroup;
  user: User | null = null;
  loading = true;
  saving = false;
  error: string | null = null;
  success: string | null = null;
  isEditMode = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      oldPassword: [''],
      newPassword: [''],
      confirmPassword: [''],
    }, { validators: this.passwordValidator.bind(this) });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.loading = true;
    this.error = null;

    this.userService.getProfile().subscribe({
      next: (user) => {
        this.user = user;
        this.form.patchValue({ 
          name: user.name,
          email: user.email
        });
        this.loading = false;
        this.isEditMode = false;
        this.cdr.markForCheck();

        const current = this.authService.getCurrentUser();
        if (current) {
          this.authService.setUser({ ...current, name: user.name });
        } else {
          this.authService.setUser(user);
        }
      },
      error: () => {
        this.error = 'Failed to load profile';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  toggleEditMode(): void {
    if (this.isEditMode) {
      // Cancel edit - reset form
      if (this.user) {
        this.form.patchValue({
          name: this.user.name,
          email: this.user.email,
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
      }
    }
    this.isEditMode = !this.isEditMode;
    this.error = null;
    this.success = null;
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid || !this.user) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.saving) return;

    this.saving = true;
    this.error = null;
    this.success = null;
    this.cdr.markForCheck();

    const formValue = this.form.value;
    const payload: any = {};

    // Check what changed
    if (formValue.name && formValue.name !== this.user.name) {
      payload.name = formValue.name;
    }

    if (formValue.email && formValue.email !== this.user.email) {
      payload.email = formValue.email;
    }

    // Handle password change
    if (formValue.newPassword && formValue.newPassword !== '') {
      if (!formValue.oldPassword || formValue.oldPassword === '') {
        this.error = 'Old password is required to change password';
        this.saving = false;
        this.cdr.markForCheck();
        return;
      }
      payload.oldPassword = formValue.oldPassword;
      payload.newPassword = formValue.newPassword;
    }

    if (Object.keys(payload).length === 0) {
      this.saving = false;
      this.success = 'No changes made';
      this.cdr.markForCheck();
      return;
    }

    this.userService.updateProfile(payload).subscribe({
      next: (updated) => {
        this.user = updated;
        const current = this.authService.getCurrentUser() ?? updated;
        this.authService.setUser({ ...current, ...updated });
        this.success = 'Profile updated successfully';
        this.toast.success('Profile updated successfully');
        this.saving = false;
        this.isEditMode = false;
        
        // Reset password fields
        this.form.patchValue({
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
        
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to update profile';
        this.saving = false;
        this.cdr.markForCheck();
      }
    });
  }

  private passwordValidator(group: AbstractControl): ValidationErrors | null {
    const oldPassword = group.get('oldPassword')?.value;
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    // If password change is being attempted
    if (newPassword && newPassword !== '') {
      // Old password is required
      if (!oldPassword || oldPassword === '') {
        return { passwordRequired: true };
      }

      // Passwords must match
      if (newPassword !== confirmPassword) {
        return { passwordMismatch: true };
      }

      // New password must be at least 8 characters
      if (newPassword.length < 8) {
        return { passwordTooShort: true };
      }
    }

    return null;
  }

  getPasswordErrorMessage(): string {
    const errors = this.form.errors;
    if (errors?.['passwordRequired']) return 'Old password is required';
    if (errors?.['passwordMismatch']) return 'New password and confirm password do not match';
    if (errors?.['passwordTooShort']) return 'New password must be at least 8 characters';
    return '';
  }
}
