package com.example.order.dto;

public class CheckoutRequest {
    private String paymentMethod; // PAY_ON_DELIVERY, CREDIT_CARD, etc.
    private String shippingAddress;

    public CheckoutRequest() {}

    public CheckoutRequest(String paymentMethod, String shippingAddress) {
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
