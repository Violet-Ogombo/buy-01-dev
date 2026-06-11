export interface CartItemDTO {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface CartDTO {
  id: string;
  userId: string;
  items: CartItemDTO[];
  totalPrice: number;
  itemCount: number;
}

export interface CheckoutRequest {
  paymentMethod: string;
  shippingAddress: string;
}
