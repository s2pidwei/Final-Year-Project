package com.example.finalyearproject;

public class DetectedProduct {
    private Product product;
    private float confidenceLvl;
    private String productId;

    public DetectedProduct(){
        this.product = new Product();
        this.confidenceLvl = 0;
        this.productId = "";
    }

    public DetectedProduct(Product product,float confidenceLvl, String productId){
        this.product = product;
        this.confidenceLvl = confidenceLvl;
        this.productId = productId;
    }


    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public float getConfidenceLvl() {
        return confidenceLvl;
    }

    public void setConfidenceLvl(float confidenceLvl) {
        this.confidenceLvl = confidenceLvl;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}

