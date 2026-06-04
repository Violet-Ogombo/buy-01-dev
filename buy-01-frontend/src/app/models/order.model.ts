export type OrderStatus = 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItemDTO {
  productId: string;
  productName: string;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface OrderDTO {
  id: string;
  orderNumber: string;
  userId: string;
  items: OrderItemDTO[];
  total: number;
  status: OrderStatus;
  paymentMethod: string;
  shippingAddress: string;
  phoneNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
