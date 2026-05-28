import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { CartItem, ShoppingCart } from '../models/cart.model';
import { Order } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly userId = 'demo-user';

  private cart: ShoppingCart = {
    _id: 'cart-demo-user',
    userId: this.userId,
    items: [
      {
        productId: 'prod-1001',
        quantity: 2,
        priceAtTime: 49.99,
        addedAt: '2026-05-28T09:00:00.000Z',
      },
    ],
    totalAmount: 99.98,
    createdAt: '2026-05-27T12:00:00.000Z',
    updatedAt: '2026-05-28T09:00:00.000Z',
  };

  private readonly orderHistory: Order[] = [
    {
      _id: 'order-1001',
      userId: this.userId,
      orderNumber: 'ORD-2026-0001',
      items: [
        {
          productId: 'prod-1001',
          quantity: 1,
          unitPrice: 49.99,
          totalPrice: 49.99,
        },
        {
          productId: 'prod-2002',
          quantity: 1,
          unitPrice: 79.99,
          totalPrice: 79.99,
        },
      ],
      status: 'delivered',
      totalAmount: 129.98,
      paymentMethod: 'credit_card',
      shippingAddress: '123 Market Street, Lagos, Nigeria',
      trackingNumber: 'TRK-DELIVERED-001',
      createdAt: '2026-05-20T10:30:00.000Z',
      updatedAt: '2026-05-22T15:15:00.000Z',
      deliveredAt: '2026-05-22T15:15:00.000Z',
    },
    {
      _id: 'order-1002',
      userId: this.userId,
      orderNumber: 'ORD-2026-0002',
      items: [
        {
          productId: 'prod-3003',
          quantity: 3,
          unitPrice: 19.99,
          totalPrice: 59.97,
        },
      ],
      status: 'processing',
      totalAmount: 59.97,
      paymentMethod: 'pay_on_delivery',
      shippingAddress: '123 Market Street, Lagos, Nigeria',
      trackingNumber: 'TRK-PROCESS-002',
      createdAt: '2026-05-26T08:45:00.000Z',
      updatedAt: '2026-05-27T10:00:00.000Z',
    },
  ];

  getCart(): Observable<ShoppingCart> {
    return of(this.cloneCart());
  }

  addToCart(productId: string, quantity: number): Observable<ShoppingCart> {
    const safeQuantity = Math.max(1, Math.floor(quantity));
    const mockPrice = this.getMockPrice(productId);
    const now = new Date().toISOString();

    const existingItem = this.cart.items.find((item) => item.productId === productId);

    if (existingItem) {
      existingItem.quantity += safeQuantity;
      existingItem.addedAt = now;
    } else {
      const newItem: CartItem = {
        productId,
        quantity: safeQuantity,
        priceAtTime: mockPrice,
        addedAt: now,
      };

      this.cart.items = [...this.cart.items, newItem];
    }

    this.cart.totalAmount = this.cart.items.reduce(
      (sum, item) => sum + item.quantity * item.priceAtTime,
      0,
    );
    this.cart.updatedAt = now;

    return of(this.cloneCart());
  }

  getOrdersHistory(): Observable<Order[]> {
    return of(this.orderHistory.map((order) => this.cloneOrder(order)));
  }

  private cloneCart(): ShoppingCart {
    return {
      ...this.cart,
      items: this.cart.items.map((item) => ({ ...item })),
    };
  }

  private cloneOrder(order: Order): Order {
    return {
      ...order,
      items: order.items.map((item) => ({ ...item })),
    };
  }

  private getMockPrice(productId: string): number {
    const priceMap: Record<string, number> = {
      'prod-1001': 49.99,
      'prod-2002': 79.99,
      'prod-3003': 19.99,
      'prod-4004': 129.99,
    };

    return priceMap[productId] ?? 24.99;
  }
}