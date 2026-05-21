package be.kitchenstaff.config;

import be.kitchenstaff.entity.Category;
import be.kitchenstaff.entity.Item;
import be.kitchenstaff.repository.CategoryRepository;
import be.kitchenstaff.repository.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(
            CategoryRepository categoryRepository,
            ItemRepository itemRepository
    ) {
        return args -> {
            Category legumes = createCategoryIfNotExists(
                    categoryRepository,
                    "Légumes",
                    "Préparations liées aux légumes"
            );

            Category sauces = createCategoryIfNotExists(
                    categoryRepository,
                    "Sauces",
                    "Préparations des sauces froides"
            );

            Category salades = createCategoryIfNotExists(
                    categoryRepository,
                    "Salades",
                    "Préparations du salad bar"
            );

            createItemIfNotExists(itemRepository, "Tomates rondelles", "kg", legumes);
            createItemIfNotExists(itemRepository, "Oignons", "kg", legumes);
            createItemIfNotExists(itemRepository, "Choux blanc", "kg", legumes);
            createItemIfNotExists(itemRepository, "Choux rouge", "kg", legumes);
            createItemIfNotExists(itemRepository, "Carottes râpées", "kg", legumes);

            createItemIfNotExists(itemRepository, "Mayonnaise", "litre", sauces);
            createItemIfNotExists(itemRepository, "Sauce tartare", "litre", sauces);
            createItemIfNotExists(itemRepository, "Vinaigrette", "litre", sauces);

            createItemIfNotExists(itemRepository, "Œufs cuits", "pièce", salades);
            createItemIfNotExists(itemRepository, "Haricots cuits", "kg", salades);
        };
    }

    private Category createCategoryIfNotExists(
            CategoryRepository categoryRepository,
            String name,
            String description
    ) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(name);
                    category.setDescription(description);
                    return categoryRepository.save(category);
                });
    }

    private void createItemIfNotExists(
            ItemRepository itemRepository,
            String name,
            String unit,
            Category category
    ) {
        if (!itemRepository.existsByName(name)) {
            Item item = new Item();
            item.setName(name);
            item.setUnit(unit);
            item.setCategory(category);
            item.setActive(true);

            itemRepository.save(item);
        }
    }
}