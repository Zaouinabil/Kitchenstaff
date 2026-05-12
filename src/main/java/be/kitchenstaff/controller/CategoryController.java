package be.kitchenstaff.controller;

import be.kitchenstaff.dto.CategoryDto;
import be.kitchenstaff.dto.CreateCategoryRequest;
import be.kitchenstaff.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDto> finAll() {
        return categoryService.findAll();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CreateCategoryRequest request){
        return categoryService.create(request);
    }
}
