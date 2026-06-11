import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { CartDTO } from '../../models/cart.model';

@Component({
  selector: 'app-cart-checkout',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './cart-checkout.html',
  styleUrl: './cart-checkout.scss'
})
export class CartCheckoutComponent implements OnInit, OnDestroy {
  cart: CartDTO | null = null;
  checkoutForm!: FormGroup;
  loading = false;
  loadingCart = true;
  errorMessage: string | null = null;

  private destroy$ = new Subject<void>();

  paymentMethods = [
    { value: 'CREDIT_CARD', label: 'Credit Card' },
    { value: 'DEBIT_CARD', label: 'Debit Card' },
    { value: 'PAYPAL', label: 'PayPal' },
    { value: 'BANK_TRANSFER', label: 'Bank Transfer' }
  ];

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.cartService.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe(cart => {
        this.cart = cart;
        if (cart && cart.items.length === 0) {
          this.toast.info('Your cart is empty. Redirecting...');
          this.router.navigate(['/products']);
        }
        this.cdr.markForCheck();
      });

    this.loadCart();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  initForm() {
    this.checkoutForm = this.fb.group({
      payOnDelivery: [false],
      cardNumber: ['', [Validators.required, Validators.pattern('^\\d{16}$')]],
      expiryMonth: ['', [Validators.required, Validators.pattern('^(0[1-9]|1[0-2])$')]],
      expiryYear: ['', [Validators.required, Validators.pattern('^\\d{2}$')]],
      securityNumber: ['', [Validators.required, Validators.pattern('^\\d{3,4}$')]],
      shippingAddress: ['', [Validators.required, Validators.minLength(10)]]
    });

    this.checkoutForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.errorMessage) {
          this.errorMessage = null;
          this.cdr.markForCheck();
        }
      });
  }

  onPayOnDeliveryChange() {
    const payOnDelivery = this.checkoutForm.get('payOnDelivery')?.value;
    const cardNumberControl = this.checkoutForm.get('cardNumber');
    const expiryMonthControl = this.checkoutForm.get('expiryMonth');
    const expiryYearControl = this.checkoutForm.get('expiryYear');
    const securityNumberControl = this.checkoutForm.get('securityNumber');

    if (payOnDelivery) {
      cardNumberControl?.disable();
      expiryMonthControl?.disable();
      expiryYearControl?.disable();
      securityNumberControl?.disable();

      cardNumberControl?.setValue('');
      expiryMonthControl?.setValue('');
      expiryYearControl?.setValue('');
      securityNumberControl?.setValue('');
    } else {
      cardNumberControl?.enable();
      expiryMonthControl?.enable();
      expiryYearControl?.enable();
      securityNumberControl?.enable();
    }
  }

  onExpiryMonthInput(event: Event, nextInput: HTMLInputElement) {
    const input = event.target as HTMLInputElement;
    if (input.value.length === 2) {
      nextInput.focus();
    }
  }

  loadCart() {
    this.loadingCart = true;
    this.cartService.getCart()
      .subscribe({
        next: () => {
          this.loadingCart = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loadingCart = false;
          this.toast.error('Could not load cart details');
          this.router.navigate(['/cart']);
          this.cdr.markForCheck();
        }
      });
  }

  onSubmit() {
    this.errorMessage = null;

    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      this.toast.error('Please fix validation errors in the form.');
      return;
    }

    if (!this.cart || this.cart.items.length === 0) {
      this.toast.error('Cannot place order on an empty cart.');
      return;
    }

    this.loading = true;
    const formVal = this.checkoutForm.getRawValue();
    const request = {
      paymentMethod: formVal.payOnDelivery ? 'PAY_ON_DELIVERY' : 'CARD_PAYMENT',
      shippingAddress: formVal.shippingAddress
    };
    
    this.cartService.checkout(request)
      .subscribe({
        next: (order) => {
          this.loading = false;
          this.toast.success(`Order placed successfully! Order #${order.orderNumber}`);
          this.router.navigate(['/orders', order.id]);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.loading = false;
          const msg = err.error?.message || err.error?.error || 'Failed to place order. Please try again.';
          this.errorMessage = msg;
          this.toast.error(msg);
          this.cdr.markForCheck();
        }
      });
  }

  isFieldInvalid(field: string): boolean {
    const control = this.checkoutForm.get(field);
    return !!(control && control.invalid && (control.touched || control.dirty));
  }
}
