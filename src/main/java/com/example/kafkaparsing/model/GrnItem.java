package com.example.kafkaparsing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "item")
public class GrnItem {

    @JsonProperty("item_code")
    private String itemCode;

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("unit_price")
    private Double unitPrice;

    @JsonProperty("total_price")
    private Double totalPrice;

    @JsonProperty("batch_number")
    private String batchNumber;

    @JsonProperty("expiry_date")
    private String expiryDate;

    // Default constructor
    public GrnItem() {}

    // Constructor with parameters
    public GrnItem(String itemCode, String itemName, Integer quantity, Double unitPrice, 
                   Double totalPrice, String batchNumber, String expiryDate) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public String toString() {
        return "GrnItem{" +
                "itemCode='" + itemCode + '\'' +
                ", itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                ", batchNumber='" + batchNumber + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                '}';
    }
}
