package com.inn.bakery.dao;

import com.inn.bakery.POJO.Product;
import com.inn.bakery.wrapper.ProductWrapper;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductDao extends JpaRepository<Product, Integer> {

    List<ProductWrapper> getAllProduct();

    @Modifying
    @Transactional
    Integer updateProductStatus(@Param("status") String status, @Param("id") Integer id);

    @Query("SELECT new com.inn.bakery.wrapper.ProductWrapper(p.id, p.name, p.flavor, p.price, p.status, p.category.id, p.category.name) " +
            "FROM Product p WHERE p.category.id = :id")
    List<ProductWrapper> getProductsByCategoryId(@Param("id") Integer id);

    ProductWrapper getProductById(@Param("id") Integer id);

}
