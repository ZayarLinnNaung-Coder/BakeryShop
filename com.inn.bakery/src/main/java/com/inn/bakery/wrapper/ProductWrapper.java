package com.inn.bakery.wrapper;

import lombok.Data;

@Data
public class ProductWrapper {

    Integer id;

    String imagePath;

    String name;

    String flavor;

    Integer price;

    String status;

    Integer categoryId;

    String categoryName;

    public ProductWrapper() {

    }

    public ProductWrapper(Integer id, String name, String flavor, Integer price, String status, Integer categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.flavor = flavor;
        this.price = price;
        this.status = status;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public ProductWrapper(Integer id, String imagePath, String name, String flavor, Integer price, String status, Integer categoryId, String categoryName) {
        this.id = id;
        this.imagePath = imagePath;
        this.name = name;
        this.flavor = flavor;
        this.price = price;
        this.status = status;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public ProductWrapper(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public ProductWrapper(Integer id, String name, String flavor, Integer price) {
        this.id = id;
        this.name = name;
        this.flavor = flavor;
        this.price = price;
    }

}
