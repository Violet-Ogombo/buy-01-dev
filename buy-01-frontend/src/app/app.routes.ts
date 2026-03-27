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
import { authGuard, sellerGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/products', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'products', component: ProductListComponent },
  { path: 'products/:id', component: ProductDetailComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
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
