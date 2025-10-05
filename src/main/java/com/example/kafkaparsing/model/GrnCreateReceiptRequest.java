package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.time.LocalDateTime;
import java.util.List;

@JacksonXmlRootElement(localName = "GrnCreateReceiptRequest")
public class GrnCreateReceiptRequest {

    @JsonProperty("grn_number")
    private String grnNumber;

    @JsonProperty("supplier_code")
    private String supplierCode;

    @JsonProperty("supplier_name")
    private String supplierName;

    @JsonProperty("po_number")
    private String poNumber;

    @JsonProperty("receipt_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime receiptDate;

    @JsonProperty("items")
    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    private List<GrnItem> items;

    @JsonProperty("total_amount")
    private Double totalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("remarks")
    private String remarks;

    // Default constructor
    public GrnCreateReceiptRequest() {}

    // Constructor with parameters
    public GrnCreateReceiptRequest(String grnNumber, String supplierCode, String supplierName, 
                                 String poNumber, LocalDateTime receiptDate, List<GrnItem> items, 
                                 Double totalAmount, String currency, String remarks) {
        this.grnNumber = grnNumber;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.poNumber = poNumber;
        this.receiptDate = receiptDate;
        this.items = items;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.remarks = remarks;
    }

    // Getters and Setters
    public String getGrnNumber() {
        return grnNumber;
    }

    public void setGrnNumber(String grnNumber) {
        this.grnNumber = grnNumber;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public LocalDateTime getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDateTime receiptDate) {
        this.receiptDate = receiptDate;
    }

    public List<GrnItem> getItems() {
        return items;
    }

    public void setItems(List<GrnItem> items) {
        this.items = items;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return "GrnCreateReceiptRequest{" +
                "grnNumber='" + grnNumber + '\'' +
                ", supplierCode='" + supplierCode + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", poNumber='" + poNumber + '\'' +
                ", receiptDate=" + receiptDate +
                ", items=" + items +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", remarks='" + remarks + '\'' +
                '}';
    }
}
