import { Component } from '@angular/core';
import { AsyncPipe, NgFor, NgClass, NgIf } from '@angular/common';
import { ToastService, Toast } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [AsyncPipe, NgFor, NgClass, NgIf],
  templateUrl: './toast-container.html',
  styleUrl: './toast-container.scss',
})
export class ToastContainerComponent {
  constructor(private toastService: ToastService) {}

  get toasts$() {
    return this.toastService.toasts$;
  }

  trackById(index: number, toast: Toast): number {
    return toast.id;
  }

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}
