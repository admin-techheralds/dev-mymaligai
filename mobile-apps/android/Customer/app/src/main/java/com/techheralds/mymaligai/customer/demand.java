package com.techheralds.mymaligai.customer;

import java.util.ArrayList;
import java.util.Map;

public class demand {
    String consumer;
    String supplier;
    String deliveryTime;
    String status;
    String timeCreated;
    ArrayList<Map<String, Object>> demandList;
    String key;
    String address;
    double price;
    String payment_mode;

    public demand() {
    }

    public demand(String consumer, String supplier, String deliveryTime, String status, String timeCreated, ArrayList<Map<String, Object>> demandList, String key, String address, double price, String payment_mode) {
        this.consumer = consumer;
        this.supplier = supplier;
        this.deliveryTime = deliveryTime;
        this.status = status;
        this.timeCreated = timeCreated;
        this.demandList = demandList;
        this.key = key;
        this.address = address;
        this.price = price;
        this.payment_mode = payment_mode;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public ArrayList<Map<String, Object>> getDemandList() {
        return demandList;
    }

    public void setDemandList(ArrayList<Map<String, Object>> demandList) {
        this.demandList = demandList;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPayment_mode() {
        return payment_mode;
    }

    public void setPayment_mode(String payment_mode) {
        this.payment_mode = payment_mode;
    }
}
