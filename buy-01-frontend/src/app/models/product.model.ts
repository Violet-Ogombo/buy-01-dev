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

