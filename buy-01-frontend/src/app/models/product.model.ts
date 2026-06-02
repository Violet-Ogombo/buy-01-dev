export interface Product {
  id?: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId?: string;
  imageUrls?: string[];
  salesCount?: number;
  revenue?: number;
}

export interface ProductSearchDTO {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  imageUrls: string[];
  salesCount?: number;
  rating?: number;
}

export interface SellerProduct {
  id?: string;
  sellerId?: string;
  productId: string;
  productName: string;
  salesCount: number;
  revenue: number;
  imageUrl?: string;
}

export interface TrendingProduct {
  id: string;
  name: string;
  description: string;
  price: number;
  salesCount: number;
  rating?: number;
  imageUrls?: string[];
}

