import { Routes } from '@angular/router';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { ProductListComponent } from './components/product-list/product-list';
import { ProductDetailComponent } from './components/product-detail/product-detail';
import { SellerDashboardComponent } from './components/seller-dashboard/seller-dashboard';
import { ProductFormComponent } from './components/product-form/product-form';
import { ProfileComponent } from './components/profile/profile';
import { SellerLayoutComponent } from './components/seller-layout/seller-layout';
import { SellerMediaComponent } from './components/seller-media/seller-media';

// New Features Components
import { CartViewComponent } from './components/cart-view/cart-view';
import { CartCheckoutComponent } from './components/cart-checkout/cart-checkout';
import { OrderList } from './components/order-list/order-list';
import { OrderDetailComponent } from './components/order-detail/order-detail';
import { WishlistViewComponent } from './components/wishlist-view/wishlist-view';
import { SearchResultsComponent } from './components/search-results/search-results';

import { authGuard, sellerGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/products', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'products', component: ProductListComponent },
  { path: 'products/:id', component: ProductDetailComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  
  // New features routes
  { path: 'cart', component: CartViewComponent, canActivate: [authGuard] },
  { path: 'cart/checkout', component: CartCheckoutComponent, canActivate: [authGuard] },
  { path: 'orders', component: OrderList, canActivate: [authGuard] },
  { path: 'orders/:id', component: OrderDetailComponent, canActivate: [authGuard] },
  { path: 'wishlist', component: WishlistViewComponent, canActivate: [authGuard] },
  { path: 'search', component: SearchResultsComponent }, // Public search

  {
    path: 'seller',
    component: SellerLayoutComponent,
    canActivate: [authGuard, sellerGuard],
    children: [
      { path: '', redirectTo: 'products', pathMatch: 'full' },
      { path: 'products', component: SellerDashboardComponent },
      { path: 'products/new', component: ProductFormComponent },
      { path: 'products/edit/:id', component: ProductFormComponent },
      { path: 'media', component: SellerMediaComponent },
      { path: 'profile', component: ProfileComponent },
    ],
  },
  { path: '**', redirectTo: '/products' }
];
