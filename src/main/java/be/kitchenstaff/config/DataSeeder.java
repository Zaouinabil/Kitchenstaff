package be.kitchenstaff.config;

import be.kitchenstaff.entity.Category;
import be.kitchenstaff.entity.Item;
import be.kitchenstaff.entity.Task;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.Role;
import be.kitchenstaff.enums.TaskPriority;
import be.kitchenstaff.enums.TaskStatus;
import be.kitchenstaff.repository.CategoryRepository;
import be.kitchenstaff.repository.ItemRepository;
import be.kitchenstaff.repository.TaskRepository;
import be.kitchenstaff.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final String DEMO_PASSWORD = "password";
    private static final int DEMO_TASK_COUNT = 200;
    private static final LocalDate TASK_PERIOD_START = LocalDate.of(2026, 7, 27);

    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed("Légumes", "Préparations liées aux légumes"),
            new CategorySeed("Sauces", "Sauces froides préparées sur place"),
            new CategorySeed("Salades", "Préparations destinées au salad bar"),
            new CategorySeed("Fruits et condiments", "Fruits et condiments pour le service"),
            new CategorySeed("Garnitures", "Accompagnements et garnitures froides"),
            new CategorySeed("Préparations froides", "Préparations réalisées avant le service"),
            new CategorySeed("Condiments", "Herbes et condiments prêts à l'emploi")
    );

    private static final List<ItemSeed> ITEM_SEEDS = List.of(
            new ItemSeed("Tomates rondelles", "kg", "Légumes"),
            new ItemSeed("Tomates au four pour tomate crevette", "kg", "Légumes"),
            new ItemSeed("Oignons", "kg", "Légumes"),
            new ItemSeed("Choux blanc", "kg", "Légumes"),
            new ItemSeed("Choux rouge", "kg", "Légumes"),
            new ItemSeed("Carottes râpées", "kg", "Légumes"),
            new ItemSeed("Céleri coupé", "kg", "Légumes"),
            new ItemSeed("Œufs cuits", "pièce", "Salades"),
            new ItemSeed("Haricots cuits", "kg", "Salades"),
            new ItemSeed("Moules oignon/céleri", "kg", "Salades"),
            new ItemSeed("Mayonnaise", "litre", "Sauces"),
            new ItemSeed("Sauce tartare", "litre", "Sauces"),
            new ItemSeed("Vinaigrette", "litre", "Sauces"),
            new ItemSeed("Citrons", "pièce", "Fruits et condiments"),
            new ItemSeed("Tomates au four", "kg", "Préparations froides"),
            new ItemSeed("Oignons émincés", "kg", "Légumes"),
            new ItemSeed("Oignons pour moules", "kg", "Préparations froides"),
            new ItemSeed("Citrons coupés", "kg", "Condiments"),
            new ItemSeed("Céleri", "kg", "Légumes"),
            new ItemSeed("Concombres rondelles", "kg", "Légumes"),
            new ItemSeed("Poivrons émincés", "kg", "Légumes"),
            new ItemSeed("Betteraves rouges", "kg", "Salades"),
            new ItemSeed("Salade mixte", "kg", "Salades"),
            new ItemSeed("Pommes de terre cuites", "kg", "Garnitures"),
            new ItemSeed("Riz froid", "kg", "Garnitures"),
            new ItemSeed("Pâtes froides", "kg", "Garnitures"),
            new ItemSeed("Cornichons émincés", "kg", "Condiments"),
            new ItemSeed("Persil haché", "kg", "Condiments"),
            new ItemSeed("Ciboulette ciselée", "kg", "Condiments"),
            new ItemSeed("Sauce cocktail", "litre", "Sauces"),
            new ItemSeed("Sauce à l'ail", "litre", "Sauces"),
            new ItemSeed("Taboulé", "kg", "Préparations froides")
    );

    private static final List<UserSeed> USER_SEEDS = List.of(
            new UserSeed("Admin Kitchenstaff", "admin@kitchenstaff.test", Role.ADMIN),
            new UserSeed("Chef Cuisine", "chef@kitchenstaff.test", Role.CHEF),
            new UserSeed("Commis Cuisine", "commis@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Sophie Lambert", "sophie.lambert@kitchenstaff.test", Role.CHEF),
            new UserSeed("Thomas Dubois", "thomas.dubois@kitchenstaff.test", Role.CHEF),
            new UserSeed("Amélie Martin", "amelie.martin@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Lucas Bernard", "lucas.bernard@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Emma Petit", "emma.petit@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Hugo Leroy", "hugo.leroy@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Chloé Simon", "chloe.simon@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Nathan Michel", "nathan.michel@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Julie Leclercq", "julie.leclercq@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Maxime Laurent", "maxime.laurent@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Camille Renard", "camille.renard@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Louis Fontaine", "louis.fontaine@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Manon Gérard", "manon.gerard@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Arthur Denis", "arthur.denis@kitchenstaff.test", Role.COMMIS),
            new UserSeed("Sarah François", "sarah.francois@kitchenstaff.test", Role.COMMIS)
    );

    private static final List<String> COMMENTS = List.of(
            "Préparation pour le salad bar", "Prévoir pour le service du midi",
            "Préparation pour tomate crevette", "Préparation pour les moules",
            "Stock faible, préparer en priorité", "Compléter le bac avant le service",
            "Préparation du matin", "Réserve pour le service du soir",
            "Mettre en bac gastronorme après préparation",
            "Contrôler la quantité avant l'envoi"
    );

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoryRepository categoryRepository, ItemRepository itemRepository,
                      UserRepository userRepository, TaskRepository taskRepository,
                      PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Map<String, Category> categories = seedCategories();
        List<Item> items = seedItems(categories);
        List<User> users = seedUsers();
        seedTasks(items, users);
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> categories = new LinkedHashMap<>();
        for (CategorySeed seed : CATEGORY_SEEDS) {
            Category category = categoryRepository.findByName(seed.name()).orElseGet(() -> {
                Category created = new Category();
                created.setName(seed.name());
                created.setDescription(seed.description());
                return categoryRepository.save(created);
            });
            categories.put(category.getName(), category);
        }
        return categories;
    }

    private List<Item> seedItems(Map<String, Category> categories) {
        Map<String, Item> existing = new LinkedHashMap<>();
        itemRepository.findAll().forEach(item -> existing.putIfAbsent(item.getName(), item));
        List<Item> items = new ArrayList<>();
        for (ItemSeed seed : ITEM_SEEDS) {
            Item item = existing.get(seed.name());
            if (item == null) {
                item = new Item();
                item.setName(seed.name());
                item.setUnit(seed.unit());
                item.setCategory(categories.get(seed.categoryName()));
                item.setActive(true);
                item = itemRepository.save(item);
                existing.put(item.getName(), item);
            }
            items.add(item);
        }
        return items;
    }

    private List<User> seedUsers() {
        List<User> users = new ArrayList<>();
        for (UserSeed seed : USER_SEEDS) {
            User user = userRepository.findByEmail(seed.email()).orElseGet(() -> {
                User created = new User();
                created.setName(seed.name());
                created.setEmail(seed.email());
                created.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
                created.setRole(seed.role());
                created.setActive(true);
                return userRepository.save(created);
            });
            users.add(user);
        }
        return users;
    }

    private void seedTasks(List<Item> items, List<User> users) {
        Random random = new Random(42L);
        for (int index = 0; index < DEMO_TASK_COUNT; index++) {
            Item item = items.get(random.nextInt(items.size()));
            User user = users.get(1 + random.nextInt(users.size() - 1));
            LocalDate date = TASK_PERIOD_START.plusDays(random.nextInt(28));
            BigDecimal quantity = quantityFor(item, random);
            String comment = COMMENTS.get(random.nextInt(COMMENTS.size()));
            TaskStatus status = statusFor(index);
            TaskPriority priority = priorityFor(random.nextInt(100));

            if (!taskRepository.existsByTaskDateAndItemIdAndAssignedUserIdAndQuantityAndComment(
                    date, item.getId(), user.getId(), quantity, comment)) {
                createTask(item, user, quantity, priority, status, comment, date);
            }
        }
    }

    private BigDecimal quantityFor(Item item, Random random) {
        if ("pièce".equals(item.getUnit())) return BigDecimal.valueOf(20L + random.nextInt(81));
        if ("litre".equals(item.getUnit())) return BigDecimal.valueOf(4L + random.nextInt(17));
        return BigDecimal.valueOf(2L + random.nextInt(7));
    }

    private TaskStatus statusFor(int index) {
        int position = index % 100;
        if (position < 60) return TaskStatus.TERMINEE;
        if (position < 85) return TaskStatus.A_FAIRE;
        if (position < 97) return TaskStatus.EN_COURS;
        return TaskStatus.ANNULEE;
    }

    private TaskPriority priorityFor(int value) {
        if (value < 15) return TaskPriority.BASSE;
        if (value < 70) return TaskPriority.NORMALE;
        if (value < 92) return TaskPriority.HAUTE;
        return TaskPriority.URGENTE;
    }

    private void createTask(Item item, User user, BigDecimal quantity, TaskPriority priority,
                            TaskStatus status, String comment, LocalDate date) {
        Task task = new Task();
        task.setItem(item);
        task.setAssignedUser(user);
        task.setQuantity(quantity);
        task.setPriority(priority);
        task.setStatus(status);
        task.setComment(comment);
        task.setTaskDate(date);
        taskRepository.save(task);
    }

    private record CategorySeed(String name, String description) { }
    private record ItemSeed(String name, String unit, String categoryName) { }
    private record UserSeed(String name, String email, Role role) { }
}
