package com.kushal.uniassist.models;

import com.google.gson.annotations.SerializedName;

public class PaymentInitiateResponse {
    @SerializedName("payment_id")
    private int paymentId;

    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("amount")
    private String amount;

    @SerializedName("esewa_payment_url")
    private String esewaPaymentUrl;

    @SerializedName("esewa_form_data")
    private EsewaFormData esewaFormData;

    public int getPaymentId() { return paymentId; }
    public int getBookingId() { return bookingId; }
    public String getAmount() {
        return amount != null ? amount : "0";
    }
    public EsewaFormData getEsewaFormData() {
        return esewaFormData;
    }

    public static class EsewaFormData {
        @SerializedName("amount")
        private String amount;

        @SerializedName("tax_amount")
        private String taxAmount;

        @SerializedName("total_amount")
        private String totalAmount;

        @SerializedName("transaction_uuid")
        private String transactionUuid;

        @SerializedName("product_code")
        private String productCode;

        @SerializedName("product_service_charge")
        private String productServiceCharge;

        @SerializedName("product_delivery_charge")
        private String productDeliveryCharge;

        @SerializedName("success_url")
        private String successUrl;

        @SerializedName("failure_url")
        private String failureUrl;

        @SerializedName("signed_field_names")
        private String signedFieldNames;

        @SerializedName("signature")
        private String signature;

        public String getAmount() {
            return amount != null ? amount : "0";
        }
        public String getTotalAmount() {
            return totalAmount != null ? 
                totalAmount : "0";
        }
        public String getTransactionUuid() {
            return transactionUuid != null ? 
                transactionUuid : "";
        }
        public String getProductCode() {
            return productCode != null ? 
                productCode : "EPAYTEST";
        }
        public String getSuccessUrl() {
            return successUrl != null ? 
                successUrl : "";
        }
        public String getFailureUrl() {
            return failureUrl != null ? 
                failureUrl : "";
        }
        public String getSignedFieldNames() {
            return signedFieldNames != null ?
                signedFieldNames :
                "total_amount,transaction_uuid,product_code";
        }
        public String getSignature() {
            return signature != null ? 
                signature : "";
        }
        public String getTaxAmount() {
            return taxAmount != null ? taxAmount : "0";
        }
        public String getProductServiceCharge() {
            return productServiceCharge != null ?
                productServiceCharge : "0";
        }
        public String getProductDeliveryCharge() {
            return productDeliveryCharge != null ?
                productDeliveryCharge : "0";
        }
    }
}
