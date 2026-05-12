package be.kitchenstaff.service;

import be.kitchenstaff.dto.CategoryDto;
import be.kitchenstaff.dto.CreateCategoryRequest;
import be.kitchenstaff.entity.Category;
import be.kitchenstaff.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CategoryDto create(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category savedCategory = categoryRepository.save(category);

        return toDto(savedCategory);
    }

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }
}