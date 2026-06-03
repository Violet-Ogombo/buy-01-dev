export interface User {
  id: string;
  name: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  token: string;
  avatar?: string;
}

export interface MostBoughtProduct {
  productId: string;
  productName: string;
  totalQuantity: number;
  totalSpent: number;
  purchaseCount: number;
}

export interface BuyerAnalytics {
  userId: string;
  totalOrders: number;
  totalSpent: number;
  mostBoughtProducts: MostBoughtProduct[];
}
