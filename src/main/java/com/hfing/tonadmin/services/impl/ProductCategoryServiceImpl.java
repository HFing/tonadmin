package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.dto.request.ProductCategoryRequest;
import com.hfing.tonadmin.entities.ProductCategory;
import com.hfing.tonadmin.mappers.ProductCategoryMapper;
import com.hfing.tonadmin.repositories.ProductCategoryRepository;
import com.hfing.tonadmin.services.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public List<ProductCategory> getAllCategories() {
        return productCategoryRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public ProductCategory getCategoryById(String id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục sản phẩm"));
    }

    @Override
    @Transactional
    public boolean createCategory(ProductCategoryRequest request, BindingResult bindingResult) {
        String code = normalizeCode(request.code());

        if (productCategoryRepository.existsByCode(code)) {
            bindingResult.rejectValue("code", "duplicate", "Mã danh mục đã tồn tại");
            return false;
        }

        ProductCategory category = productCategoryMapper.toProductCategory(request);
        category.setCode(code);
        category.setName(trimToNull(request.name()));
        category.setActive(true);

        productCategoryRepository.save(category);
        return true;
    }

    @Override
    @Transactional
    public boolean updateCategory(String id, ProductCategoryRequest request, BindingResult bindingResult) {
        ProductCategory category = getCategoryById(id);
        String code = normalizeCode(request.code());

        if (productCategoryRepository.existsByCodeAndIdNot(code, id)) {
            bindingResult.rejectValue("code", "duplicate", "Mã danh mục đã tồn tại");
            return false;
        }

        productCategoryMapper.updateProductCategoryFromRequest(request, category);

        category.setCode(code);
        category.setName(trimToNull(request.name()));

        productCategoryRepository.save(category);
        return true;
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        ProductCategory category = getCategoryById(id);
        category.setActive(!Boolean.TRUE.equals(category.getActive()));
        productCategoryRepository.save(category);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}