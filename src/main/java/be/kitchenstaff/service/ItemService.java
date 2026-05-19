package be.kitchenstaff.service;

import be.kitchenstaff.dto.CreateItemRequest;
import be.kitchenstaff.dto.ItemDto;
import be.kitchenstaff.entity.Category;
import be.kitchenstaff.entity.Item;
import be.kitchenstaff.repository.CategoryRepository;
import be.kitchenstaff.repository.ItemRepository;
import org.springframework.stereotype.Service;
import be.kitchenstaff.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public ItemService(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ItemDto> findAll(Long categoryId) {
        List<Item> items;

        if (categoryId != null) {
            items = itemRepository.findByCategoryId(categoryId);
        } else {
            items = itemRepository.findAll();
        }

        return items.stream()
                .map(this::toDto)
                .toList();
    }

    public ItemDto create(CreateItemRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));

        Item item = new Item();
        item.setName(request.getName());
        item.setUnit(request.getUnit());
        item.setCategory(category);
        item.setActive(true);

        Item savedItem = itemRepository.save(item);

        return toDto(savedItem);
    }

    private ItemDto toDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setActive(item.getActive());

        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }

        return dto;
    }
}