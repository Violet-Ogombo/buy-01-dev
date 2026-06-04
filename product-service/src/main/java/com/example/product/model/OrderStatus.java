package com.example.product.model;

public enum OrderStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(OrderStatus nextStatus) {
        switch (this) {
            case PENDING:
                return nextStatus == PROCESSING || nextStatus == CANCELLED;
            case PROCESSING:
                return nextStatus == SHIPPED || nextStatus == CANCELLED;
            case SHIPPED:
                return nextStatus == DELIVERED;
            case DELIVERED:
                return false;
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
}
