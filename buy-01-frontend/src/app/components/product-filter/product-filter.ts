import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface FilterParams {
  keyword?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
}

@Component({
  selector: 'app-product-filter',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-filter.html',
  styleUrl: './product-filter.scss'
})
export class ProductFilterComponent implements OnInit {
  @Input() currentFilters: FilterParams = {};
  @Input() totalProducts: number = 0;
  @Input() filteredProducts: number = 0;

  @Output() filterChange = new EventEmitter<FilterParams>();

  keyword: string = '';
  category: string = 'all';
  minPrice: number = 0;
  maxPrice: number = 2000;

  categories = [
    { value: 'all', label: 'All categories' },
    { value: 'electronics', label: 'Electronics' },
    { value: 'fashion', label: 'Fashion' },
    { value: 'home', label: 'Home & Garden' },
    { value: 'sports', label: 'Sports' },
    { value: 'books', label: 'Books' }
  ];

  ngOnInit() {
    if (this.currentFilters) {
      this.keyword = this.currentFilters.keyword || '';
      this.category = this.currentFilters.category || 'all';
      this.minPrice = this.currentFilters.minPrice ?? 0;
      this.maxPrice = this.currentFilters.maxPrice ?? 2000;
    }
  }

  get activeFilterCount(): number {
    let count = 0;
    if (this.keyword) count++;
    if (this.category && this.category !== 'all') count++;
    if (this.minPrice > 0) count++;
    if (this.maxPrice < 2000) count++;
    return count;
  }

  applyFilters() {
    this.filterChange.emit({
      keyword: this.keyword || undefined,
      category: this.category !== 'all' ? this.category : undefined,
      minPrice: this.minPrice > 0 ? this.minPrice : undefined,
      maxPrice: this.maxPrice < 2000 ? this.maxPrice : undefined
    });
  }

  resetFilters() {
    this.keyword = '';
    this.category = 'all';
    this.minPrice = 0;
    this.maxPrice = 2000;
    this.applyFilters();
  }

  onFilterChange() {
    this.applyFilters();
  }
}
