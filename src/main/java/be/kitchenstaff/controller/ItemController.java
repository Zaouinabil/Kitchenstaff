package be.kitchenstaff.controller;

import be.kitchenstaff.dto.CreateItemRequest;
import be.kitchenstaff.dto.ItemDto;
import be.kitchenstaff.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import be.kitchenstaff.dto.UpdateItemRequest;

import java.util.List;
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {

        this.itemService = itemService;

    }

    @GetMapping

    public List<ItemDto> findAll(@RequestParam(required = false) Long categoryId) {

        return itemService.findAll(categoryId);

    }

    @PostMapping

    @ResponseStatus(HttpStatus.CREATED)

    public ItemDto create(@Valid @RequestBody CreateItemRequest request) {

        return itemService.create(request);

    }
    @PutMapping("/{id}")
    public ItemDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        return itemService.update(id, request);
    }
}

