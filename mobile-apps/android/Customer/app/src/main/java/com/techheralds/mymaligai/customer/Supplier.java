package com.techheralds.mymaligai.customer;

public class Supplier {
    String name;
    String uid;
    String phoneNumber;
    String address;
    String location;
    Boolean status;
    Long dateCreated;
    String photo;
    String smsTemplate;

    public Supplier() {
    }

    public Supplier(String name, String uid, String phoneNumber, String address, String location, Boolean status, Long dateCreated, String photo, String smsTemplate) {
        this.name = name;
        this.uid = uid;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.location = location;
        this.status = status;
        this.dateCreated = dateCreated;
        this.photo = photo;
        this.smsTemplate = smsTemplate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getSmsTemplate() {
        return smsTemplate;
    }

    public void setSmsTemplate(String smsTemplate) {
        this.smsTemplate = smsTemplate;
    }
}
