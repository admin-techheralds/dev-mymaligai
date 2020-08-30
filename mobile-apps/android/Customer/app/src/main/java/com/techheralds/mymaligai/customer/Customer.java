package com.techheralds.mymaligai.customer;

import java.util.ArrayList;

class Customer {
    String Name;
    String Dp;
    String Uid;
    String PhoneNumber;
    String Address;

    public Customer() {
    }

    public Customer(String name, String dp, String uid, String phoneNumber, String address) {
        Name = name;
        Dp = dp;
        Uid = uid;
        PhoneNumber = phoneNumber;
        Address = address;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getDp() {
        return Dp;
    }

    public void setDp(String dp) {
        Dp = dp;
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        PhoneNumber = phoneNumber;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }
}
