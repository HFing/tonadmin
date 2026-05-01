package com.hfing.tonadmin.mappers;

import com.hfing.tonadmin.dto.request.ProductCategoryRequest;
import com.hfing.tonadmin.entities.ProductCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductCategoryMapper {
    
    ProductCategory toProductCategory(ProductCategoryRequest request);

    ProductCategoryRequest toProductCategoryRequest(ProductCategory category);

    void updateProductCategoryFromRequest(ProductCategoryRequest request, @MappingTarget ProductCategory category);
}