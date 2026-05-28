export interface OrderItem {
  productId: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export type OrderStatus = 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled';

export type PaymentMethod = 'pay_on_delivery' | 'credit_card' | string;

export interface Order {
  _id: string;
  userId: string;
  orderNumber: string;
  items: OrderItem[];
  status: OrderStatus;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  shippingAddress: string;
  trackingNumber: string;
  createdAt: Date | string;
  updatedAt: Date | string;
  deliveredAt?: Date | string;
}