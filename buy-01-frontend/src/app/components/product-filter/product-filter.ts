import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface FilterParams {
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
  @Input() currentFilters: FilterParams = {
    minPrice: undefined,
    maxPrice: undefined,
    sortBy: 'popularity'
  };

  @Output() filterChange = new EventEmitter<FilterParams>();

  minPrice: number | undefined;
  maxPrice: number | undefined;
  sortBy: string = 'popularity';

  sortOptions = [
    { value: 'popularity', label: 'Popularity' },
    { value: 'price_asc', label: 'Price: Low to High' },
    { value: 'price_desc', label: 'Price: High to Low' },
    { value: 'name', label: 'Alphabetical (A-Z)' },
    { value: 'newest', label: 'Newest Arrivals' }
  ];

  ngOnInit() {
    this.minPrice = this.currentFilters.minPrice;
    this.maxPrice = this.currentFilters.maxPrice;
    this.sortBy = this.currentFilters.sortBy || 'popularity';
  }

  applyFilters() {
    this.filterChange.emit({
      minPrice: this.minPrice === null || this.minPrice === undefined ? undefined : this.minPrice,
      maxPrice: this.maxPrice === null || this.maxPrice === undefined ? undefined : this.maxPrice,
      sortBy: this.sortBy
    });
  }

  clearFilters() {
    this.minPrice = undefined;
    this.maxPrice = undefined;
    this.sortBy = 'popularity';
    this.applyFilters();
  }
}
