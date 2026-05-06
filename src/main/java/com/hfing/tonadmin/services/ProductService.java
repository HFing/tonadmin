package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.ProductRequest;
import com.hfing.tonadmin.dto.request.ProductSearchRequest;
import com.hfing.tonadmin.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface ProductService {

    Page<Product> getProducts(ProductSearchRequest search, Pageable pageable);

    List<Product> getAllProducts();

    Product getProductById(String id);

    boolean createProduct(ProductRequest request, BindingResult bindingResult);

    boolean updateProduct(String id, ProductRequest request, BindingResult bindingResult);

    void toggleActive(String id);
}
