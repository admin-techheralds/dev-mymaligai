package com.techheralds.mymaligai.customer;

public class inventory {
    String name;
    int quantity_type;
    String img;
    float price;
    int active;
    String attr1;
    String attr2;
    String attr3;
    String bulk_import_id;
    long created_date;
    String created_mode;
    int in_stock;
    String sku;

    public inventory() {
    }

    public inventory(String name, int quantity_type, String img, float price, int active, String attr1, String attr2, String attr3, String bulk_import_id, long created_date, String created_mode, int in_stock, String sku) {
        this.name = name;
        this.quantity_type = quantity_type;
        this.img = img;
        this.price = price;
        this.active = active;
        this.attr1 = attr1;
        this.attr2 = attr2;
        this.attr3 = attr3;
        this.bulk_import_id = bulk_import_id;
        this.created_date = created_date;
        this.created_mode = created_mode;
        this.in_stock = in_stock;
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity_type() {
        return quantity_type;
    }

    public void setQuantity_type(int quantity_type) {
        this.quantity_type = quantity_type;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public String getAttr1() {
        return attr1;
    }

    public void setAttr1(String attr1) {
        this.attr1 = attr1;
    }

    public String getAttr2() {
        return attr2;
    }

    public void setAttr2(String attr2) {
        this.attr2 = attr2;
    }

    public String getAttr3() {
        return attr3;
    }

    public void setAttr3(String attr3) {
        this.attr3 = attr3;
    }

    public String getBulk_import_id() {
        return bulk_import_id;
    }

    public void setBulk_import_id(String bulk_import_id) {
        this.bulk_import_id = bulk_import_id;
    }

    public long getCreated_date() {
        return created_date;
    }

    public void setCreated_date(long created_date) {
        this.created_date = created_date;
    }

    public String getCreated_mode() {
        return created_mode;
    }

    public void setCreated_mode(String created_mode) {
        this.created_mode = created_mode;
    }

    public int getIn_stock() {
        return in_stock;
    }

    public void setIn_stock(int in_stock) {
        this.in_stock = in_stock;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
