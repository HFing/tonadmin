package com.hfing.tonadmin.mappers;

import com.hfing.tonadmin.dto.request.ProductRequest;
import com.hfing.tonadmin.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    Product toProduct(ProductRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    ProductRequest toProductRequest(Product product);
    
    void updateProductFromRequest(ProductRequest request, @MappingTarget Product product);
}