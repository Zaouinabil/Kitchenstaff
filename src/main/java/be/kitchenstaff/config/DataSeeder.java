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

        createItemIfNotExists("Tomates rondelles", "kg", legumes);
        createItemIfNotExists("Oignons", "kg", legumes);
        createItemIfNotExists("Choux blanc", "kg", legumes);
        createItemIfNotExists("Choux rouge", "kg", legumes);
        createItemIfNotExists("Carottes râpées", "kg", legumes);
        createItemIfNotExists("Œufs cuits", "pièce", salades);
        createItemIfNotExists("Haricots cuits", "kg", salades);
        createItemIfNotExists("Mayonnaise", "litre", sauces);
        createItemIfNotExists("Sauce tartare", "litre", sauces);
        createItemIfNotExists("Vinaigrette", "litre", sauces);

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

        Item tomates = findItemByName("Tomates rondelles");
        Item mayonnaise = findItemByName("Mayonnaise");
        Item oeufs = findItemByName("Œufs cuits");

        createDemoTask(
                tomates,
                commis,
                new BigDecimal("5"),
                TaskPriority.NORMALE,
                TaskStatus.A_FAIRE,
                "Préparer les tomates rondelles pour le salad bar",
                today
        );

        createDemoTask(
                mayonnaise,
                chef,
                new BigDecimal("10"),
                TaskPriority.HAUTE,
                TaskStatus.EN_COURS,
                "Préparer la mayonnaise pour le service du midi",
                today
        );

        createDemoTask(
                oeufs,
                commis,
                new BigDecimal("30"),
                TaskPriority.NORMALE,
                TaskStatus.TERMINEE,
                "Cuire et écaler les œufs pour les salades",
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