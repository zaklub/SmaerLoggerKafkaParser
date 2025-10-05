package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.LocalDateTime;

@JacksonXmlRootElement(localName = "GrnCreateReceiptResponse")
public class GrnCreateReceiptResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("grn_id")
    private String grnId;

    @JsonProperty("grn_number")
    private String grnNumber;

    @JsonProperty("receipt_id")
    private String receiptId;

    @JsonProperty("processed_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedDate;

    @JsonProperty("total_items")
    private Integer totalItems;

    @JsonProperty("total_amount")
    private Double totalAmount;

    @JsonProperty("currency")
    private String currency;

    // Default constructor
    public GrnCreateReceiptResponse() {}

    // Constructor with parameters
    public GrnCreateReceiptResponse(String status, String message, String grnId, String grnNumber, 
                                  String receiptId, LocalDateTime processedDate, Integer totalItems, 
                                  Double totalAmount, String currency) {
        this.status = status;
        this.message = message;
        this.grnId = grnId;
        this.grnNumber = grnNumber;
        this.receiptId = receiptId;
        this.processedDate = processedDate;
        this.totalItems = totalItems;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGrnId() {
        return grnId;
    }

    public void setGrnId(String grnId) {
        this.grnId = grnId;
    }

    public String getGrnNumber() {
        return grnNumber;
    }

    public void setGrnNumber(String grnNumber) {
        this.grnNumber = grnNumber;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "GrnCreateReceiptResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", grnId='" + grnId + '\'' +
                ", grnNumber='" + grnNumber + '\'' +
                ", receiptId='" + receiptId + '\'' +
                ", processedDate=" + processedDate +
                ", totalItems=" + totalItems +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                '}';
    }
}
