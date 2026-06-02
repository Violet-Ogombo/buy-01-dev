export interface User {
  id: string;
  name: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  token: string;
  avatar?: string;
  isSeller?: boolean;
  sellerRevenue?: number;
  totalSpent?: number;
  mostBoughtProducts?: ProductHistoryItem[];
}

export interface ProductHistoryItem {
  productId: string;
  productName: string;
  quantity: number;
  price: number;
  imageUrl?: string;
  purchaseDate?: string;
}
