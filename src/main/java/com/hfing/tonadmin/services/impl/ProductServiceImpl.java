package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.dto.request.ProductRequest;
import com.hfing.tonadmin.dto.request.ProductSearchRequest;
import com.hfing.tonadmin.entities.Product;
import com.hfing.tonadmin.entities.ProductCategory;
import com.hfing.tonadmin.mappers.ProductMapper;
import com.hfing.tonadmin.repositories.ProductCategoryRepository;
import com.hfing.tonadmin.repositories.ProductRepository;
import com.hfing.tonadmin.services.ProductService;
import com.hfing.tonadmin.specifications.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductMapper productMapper;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    @Override
    public Page<Product> getProducts(ProductSearchRequest search, Pageable pageable) {
        ProductSearchRequest safeSearch = search == null
                ? new ProductSearchRequest(null, null, null)
                : search;

        return productRepository.findAll(ProductSpecifications.bySearch(safeSearch), pageable);
    }

    @Override
    @Transactional
    public boolean createProduct(ProductRequest request, BindingResult bindingResult) {
        String code = normalizeCode(request.code());

        if (productRepository.existsByCode(code)) {
            bindingResult.rejectValue("code", "duplicate", "Mã sản phẩm đã tồn tại");
            return false;
        }

        ProductCategory category = getActiveCategory(request.categoryId(), bindingResult);
        if (category == null) {
            return false;
        }

        Product product = productMapper.toProduct(request);

        product.setCode(code);
        product.setName(trimToNull(request.name()));
        product.setDescription(trimToNull(request.description()));
        product.setCategory(category);
        product.setImportPrice(defaultZero(request.importPrice()));
        product.setSellingPrice(defaultZero(request.sellingPrice()));
        product.setMinStock(defaultZero(request.minStock()));
        product.setActive(true);

        productRepository.save(product);
        return true;
    }

    @Override
    @Transactional
    public boolean updateProduct(String id, ProductRequest request, BindingResult bindingResult) {
        Product product = getProductById(id);
        String code = normalizeCode(request.code());

        if (productRepository.existsByCodeAndIdNot(code, id)) {
            bindingResult.rejectValue("code", "duplicate", "Mã sản phẩm đã tồn tại");
            return false;
        }

        ProductCategory category = getActiveCategory(request.categoryId(), bindingResult);
        if (category == null) {
            return false;
        }

        productMapper.updateProductFromRequest(request, product);

        product.setCode(code);
        product.setName(trimToNull(request.name()));
        product.setDescription(trimToNull(request.description()));
        product.setCategory(category);
        product.setImportPrice(defaultZero(request.importPrice()));
        product.setSellingPrice(defaultZero(request.sellingPrice()));
        product.setMinStock(defaultZero(request.minStock()));

        productRepository.save(product);
        return true;
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        Product product = getProductById(id);
        product.setActive(!Boolean.TRUE.equals(product.getActive()));
        productRepository.save(product);
    }

    private ProductCategory getActiveCategory(String categoryId, BindingResult bindingResult) {
        if (categoryId == null || categoryId.isBlank()) {
            bindingResult.rejectValue("categoryId", "required", "Vui lòng chọn danh mục");
            return null;
        }

        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElse(null);

        if (category == null || !Boolean.TRUE.equals(category.getActive())) {
            bindingResult.rejectValue("categoryId", "invalid", "Danh mục không hợp lệ hoặc đã bị khóa");
            return null;
        }

        return category;
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

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
