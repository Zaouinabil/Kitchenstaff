package be.kitchenstaff;

import be.kitchenstaff.config.DataSeeder;
import be.kitchenstaff.dto.CreateTaskRequest;
import be.kitchenstaff.dto.TaskDto;
import be.kitchenstaff.dto.UpdateTaskRequest;
import be.kitchenstaff.entity.Category;
import be.kitchenstaff.entity.Item;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.Role;
import be.kitchenstaff.enums.TaskStatus;
import be.kitchenstaff.repository.CategoryRepository;
import be.kitchenstaff.repository.ItemRepository;
import be.kitchenstaff.repository.TaskRepository;
import be.kitchenstaff.repository.UserRepository;
import be.kitchenstaff.service.TaskService;
import be.kitchenstaff.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Deliberately no test-level transaction: DTO conversion must work inside each service call.
@SpringBootTest
@ActiveProfiles("test")
class TaskPersistenceIntegrationTest {
    @Autowired TaskService service;
    @Autowired UserService userService;
    @Autowired TaskRepository tasks;
    @Autowired ItemRepository items;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired ApplicationContext context;

    private Category category;
    private Item item;
    private User user;
    private Long taskId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        category = new Category();
        category.setName("Category " + suffix);
        category = categories.save(category);
        item = new Item();
        item.setName("Carrots");
        item.setUnit("kg");
        item.setCategory(category);
        item = items.save(item);
        user = new User();
        user.setName("Integration chef");
        user.setEmail(suffix + "@example.test");
        user.setPassword(UUID.randomUUID().toString());
        user.setRole(Role.CHEF);
        user = users.save(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_CHEF"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        if (taskId != null) tasks.deleteById(taskId);
        items.deleteById(item.getId());
        categories.deleteById(category.getId());
        users.deleteById(user.getId());
    }

    @Test
    void mutationsReturnCompleteDtosWithOpenInViewDisabled() {
        assertTrue(context.getBeansOfType(DataSeeder.class).isEmpty());
        CreateTaskRequest create = new CreateTaskRequest();
        create.setItemId(item.getId());
        create.setAssignedUserId(user.getId());
        create.setQuantity(new BigDecimal("2.50"));
        create.setTaskDate(LocalDate.of(2026, 11, 26));
        TaskDto created = service.create(create);
        taskId = created.getId();
        assertComplete(created);

        UpdateTaskRequest update = new UpdateTaskRequest();
        update.setItemId(item.getId());
        update.setAssignedUserId(user.getId());
        update.setTaskDate(create.getTaskDate());
        update.setQuantity(new BigDecimal("3.00"));
        assertComplete(service.update(taskId, update));
        TaskDto started = service.start(taskId);
        assertComplete(started);
        assertEquals(TaskStatus.EN_COURS, started.getStatus());
        TaskDto done = service.done(taskId);
        assertComplete(done);
        assertEquals(TaskStatus.TERMINEE, done.getStatus());
        TaskDto cancelled = service.cancel(taskId);
        assertComplete(cancelled);
        assertEquals(TaskStatus.ANNULEE, cancelled.getStatus());
        TaskDto unassigned = service.unassign(taskId);
        assertNull(unassigned.getAssignedUserId());
        assertEquals(category.getName(), unassigned.getCategoryName());
        assertEquals("kg", service.findById(taskId).getItemUnit());
        assertTrue(service.findAll(create.getTaskDate(), TaskStatus.ANNULEE, null, category.getId())
                .stream().anyMatch(dto -> dto.getId().equals(taskId)));
        assertTrue(service.findAll(create.getTaskDate(), TaskStatus.ANNULEE, user.getId(), category.getId()).isEmpty());
    }

    @Test
    void realQueryCombinesFiltersAndCommisCannotChooseAnotherOwner() {
        CreateTaskRequest create = new CreateTaskRequest();
        create.setItemId(item.getId());
        create.setAssignedUserId(user.getId());
        create.setQuantity(BigDecimal.ONE);
        create.setTaskDate(LocalDate.of(2026, 11, 26));
        taskId = service.create(create).getId();
        assertEquals(1, service.findAll(create.getTaskDate(), TaskStatus.A_FAIRE, user.getId(), category.getId()).size());
        assertTrue(service.findAll(create.getTaskDate().plusDays(1), TaskStatus.A_FAIRE, user.getId(), category.getId()).isEmpty());
        assertTrue(service.findAll(create.getTaskDate(), TaskStatus.TERMINEE, user.getId(), category.getId()).isEmpty());
        assertTrue(service.findAll(create.getTaskDate(), TaskStatus.A_FAIRE, user.getId(), -1L).isEmpty());
        assertTrue(service.findAll(create.getTaskDate(), TaskStatus.A_FAIRE, -1L, category.getId()).isEmpty());

        user.setRole(Role.COMMIS);
        users.save(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_COMMIS"))));
        assertEquals(List.of(taskId), service.findAll(create.getTaskDate(), TaskStatus.A_FAIRE, -1L, category.getId())
                .stream().map(TaskDto::getId).toList());
    }

    @Test
    void assignableListExcludesInactiveUsers() {
        assertTrue(userService.findAssignable().stream().anyMatch(dto -> dto.id().equals(user.getId())));
        user.setActive(false);
        users.save(user);
        assertTrue(userService.findAssignable().stream().noneMatch(dto -> dto.id().equals(user.getId())));
    }

    private void assertComplete(TaskDto dto) {
        assertEquals("Carrots", dto.getItemName());
        assertEquals("kg", dto.getItemUnit());
        assertEquals(category.getName(), dto.getCategoryName());
        assertEquals(user.getName(), dto.getAssignedUserName());
    }
}
