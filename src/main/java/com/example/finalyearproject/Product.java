package com.example.finalyearproject;

public class Product {
    private String id;
    private String name;
    private double price;
    private double priceBefore;
    private String type;

    public Product(){
        this.id = null;
        this.name = "null";
        this.price = 0.00;
        this.priceBefore = 0.00;
        this.type = "null";
    }

    public Product(String name, double price, String type){
        this.name = name;
        this.price = price;
        this.type = type;
    }

    public Product(String name, double price, double priceBefore ,String type){
        this.name = name;
        this.price = price;
        this.priceBefore = priceBefore;
        this.type = type;
    }

    //Product name getter and setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    //Product price getter and setter
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    //Product type getter and setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getPriceBefore() {
        return priceBefore;
    }

    public void setPriceBefore(double priceBefore) {
        this.priceBefore = priceBefore;
    }
}
