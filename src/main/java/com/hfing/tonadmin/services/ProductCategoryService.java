package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.ProductCategoryRequest;
import com.hfing.tonadmin.entities.ProductCategory;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface ProductCategoryService {

    List<ProductCategory> getAllCategories();

    ProductCategory getCategoryById(String id);

    boolean createCategory(ProductCategoryRequest request, BindingResult bindingResult);

    boolean updateCategory(String id, ProductCategoryRequest request, BindingResult bindingResult);

    void toggleActive(String id);
}
