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

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            CategoryRepository categoryRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Category legumes = createCategoryIfNotExists(
                "Légumes",
                "Préparations liées aux légumes"
        );

        Category sauces = createCategoryIfNotExists(
                "Sauces",
                "Préparations liées aux sauces"
        );

        Category salades = createCategoryIfNotExists(
                "Salades",
                "Préparations liées aux salades"
        );

        Category fruitsEtCondiments = createCategoryIfNotExists(
                "Fruits et condiments",
                "Préparations liées aux fruits et condiments"
        );

        createItemIfNotExists("Tomates rondelles", "kg", legumes);
        createItemIfNotExists("Tomates au four pour tomate crevette", "kg", legumes);
        createItemIfNotExists("Oignons", "kg", legumes);
        createItemIfNotExists("Choux blanc", "kg", legumes);
        createItemIfNotExists("Choux rouge", "kg", legumes);
        createItemIfNotExists("Carottes râpées", "kg", legumes);
        createItemIfNotExists("Céleri coupé", "kg", legumes);
        createItemIfNotExists("Œufs cuits", "pièce", salades);
        createItemIfNotExists("Haricots cuits", "kg", salades);
        createItemIfNotExists("Moules oignon/céleri", "kg", salades);
        createItemIfNotExists("Mayonnaise", "litre", sauces);
        createItemIfNotExists("Sauce tartare", "litre", sauces);
        createItemIfNotExists("Vinaigrette", "litre", sauces);
        createItemIfNotExists("Citrons", "pièce", fruitsEtCondiments);

        createUserIfNotExists(
                "Admin Kitchenstaff",
                "admin@kitchenstaff.test",
                Role.ADMIN
        );

        createUserIfNotExists(
                "Chef Cuisine",
                "chef@kitchenstaff.test",
                Role.CHEF
        );

        createUserIfNotExists(
                "Commis Cuisine",
                "commis@kitchenstaff.test",
                Role.COMMIS
        );

        createDemoTasksIfNotExists();
    }

    private Category createCategoryIfNotExists(String name, String description) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(name);
                    category.setDescription(description);

                    return categoryRepository.save(category);
                });
    }

    private void createItemIfNotExists(String name, String unit, Category category) {
        if (itemRepository.existsByName(name)) {
            return;
        }

        Item item = new Item();
        item.setName(name);
        item.setUnit(unit);
        item.setCategory(category);
        item.setActive(true);

        itemRepository.save(item);
    }

    private void createUserIfNotExists(String name, String email, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
                existingUser -> {
                    existingUser.setName(name);
                    existingUser.setPassword(passwordEncoder.encode("password"));
                    existingUser.setRole(role);
                    existingUser.setActive(true);

                    userRepository.save(existingUser);
                },
                () -> {
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode("password"));
                    user.setRole(role);
                    user.setActive(true);

                    userRepository.save(user);
                }
        );
    }

    private void createDemoTasksIfNotExists() {
        LocalDate today = LocalDate.now();

        if (!taskRepository.findByTaskDateOrderByIdDesc(today).isEmpty()) {
            return;
        }

        User chef = userRepository.findByEmail("chef@kitchenstaff.test")
                .orElseThrow();

        User commis = userRepository.findByEmail("commis@kitchenstaff.test")
                .orElseThrow();

        createDemoTask(
                findItemByName("Tomates rondelles"),
                commis,
                new BigDecimal("5"),
                TaskPriority.NORMALE,
                TaskStatus.A_FAIRE,
                "Préparer les tomates rondelles pour le salad bar",
                today
        );

        createDemoTask(
                findItemByName("Oignons"),
                commis,
                new BigDecimal("3"),
                TaskPriority.HAUTE,
                TaskStatus.EN_COURS,
                "Préparer les oignons pour les moules",
                today
        );

        createDemoTask(
                findItemByName("Choux blanc"),
                commis,
                new BigDecimal("4"),
                TaskPriority.NORMALE,
                TaskStatus.A_FAIRE,
                "Émincer le chou blanc pour le salad bar",
                today
        );

        createDemoTask(
                findItemByName("Choux rouge"),
                commis,
                new BigDecimal("4"),
                TaskPriority.NORMALE,
                TaskStatus.TERMINEE,
                "Émincer le chou rouge pour le salad bar",
                today
        );

        createDemoTask(
                findItemByName("Carottes râpées"),
                commis,
                new BigDecimal("5"),
                TaskPriority.HAUTE,
                TaskStatus.EN_COURS,
                "Râper les carottes pour les salades",
                today
        );

        createDemoTask(
                findItemByName("Œufs cuits"),
                commis,
                new BigDecimal("40"),
                TaskPriority.NORMALE,
                TaskStatus.TERMINEE,
                "Cuire et écaler les œufs pour les salades",
                today
        );

        createDemoTask(
                findItemByName("Mayonnaise"),
                chef,
                new BigDecimal("20"),
                TaskPriority.HAUTE,
                TaskStatus.EN_COURS,
                "Préparer la mayonnaise pour le service",
                today
        );

        createDemoTask(
                findItemByName("Sauce tartare"),
                chef,
                new BigDecimal("5"),
                TaskPriority.URGENTE,
                TaskStatus.A_FAIRE,
                "Préparer la sauce tartare pour le service",
                today
        );

        createDemoTask(
                findItemByName("Vinaigrette"),
                chef,
                new BigDecimal("5"),
                TaskPriority.NORMALE,
                TaskStatus.TERMINEE,
                "Préparer la vinaigrette pour le salad bar",
                today
        );

        createDemoTask(
                findItemByName("Citrons"),
                commis,
                new BigDecimal("30"),
                TaskPriority.HAUTE,
                TaskStatus.TERMINEE,
                "Couper les citrons en quartiers pour le service",
                today
        );

        createDemoTask(
                findItemByName("Moules oignon/céleri"),
                commis,
                new BigDecimal("6"),
                TaskPriority.URGENTE,
                TaskStatus.EN_COURS,
                "Préparer l'oignon et le céleri pour les moules",
                today
        );

        createDemoTask(
                findItemByName("Tomates au four pour tomate crevette"),
                chef,
                new BigDecimal("30"),
                TaskPriority.URGENTE,
                TaskStatus.A_FAIRE,
                "Préparer les tomates au four pour les tomates crevettes",
                today
        );
    }

    private Item findItemByName(String name) {
        return itemRepository.findAll()
                .stream()
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void createDemoTask(
            Item item,
            User assignedUser,
            BigDecimal quantity,
            TaskPriority priority,
            TaskStatus status,
            String comment,
            LocalDate taskDate
    ) {
        Task task = new Task();
        task.setItem(item);
        task.setAssignedUser(assignedUser);
        task.setQuantity(quantity);
        task.setPriority(priority);
        task.setStatus(status);
        task.setComment(comment);
        task.setTaskDate(taskDate);

        taskRepository.save(task);
    }
}
