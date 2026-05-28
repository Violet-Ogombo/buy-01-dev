export interface CartItem {
  productId: string;
  quantity: number;
  priceAtTime: number;
  addedAt: Date | string;
}

export interface ShoppingCart {
  _id: string;
  userId: string;
  items: CartItem[];
  totalAmount: number;
  createdAt: Date | string;
  updatedAt: Date | string;
}
