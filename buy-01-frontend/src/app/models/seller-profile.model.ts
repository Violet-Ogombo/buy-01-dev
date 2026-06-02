export interface BestSellingProduct {
  productId: string;
  name: string;
  price: number;
  salesCount: number;
  revenue: number;
}

export interface SellerProfile {
  sellerId: string;
  totalProducts: number;
  totalSales: number;
  totalRevenue: number;
  bestSellingProducts: BestSellingProduct[];
}
