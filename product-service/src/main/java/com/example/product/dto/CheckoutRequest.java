package com.example.product.dto;

public class CheckoutRequest {
    private String paymentMethod; // PAY_ON_DELIVERY, CREDIT_CARD, etc.
    private String shippingAddress;
    private String phoneNumber;

    public CheckoutRequest() {}

    public CheckoutRequest(String paymentMethod, String shippingAddress, String phoneNumber) {
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
