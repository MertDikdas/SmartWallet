package com.smartwallet.financeservice.mapper;

import com.smartwallet.financeservice.dto.response.CategoryResponse;
import com.smartwallet.financeservice.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt()
        );
    }
}