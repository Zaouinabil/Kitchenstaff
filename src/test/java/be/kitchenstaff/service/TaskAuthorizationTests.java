package be.kitchenstaff.service;

import be.kitchenstaff.entity.Task;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.TaskStatus;
import be.kitchenstaff.repository.ItemRepository;
import be.kitchenstaff.repository.TaskRepository;
import be.kitchenstaff.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskAuthorizationTests {
    private TaskRepository tasks;
    private UserRepository users;
    private TaskService service;
    private User commis;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskRepository.class);
        users = mock(UserRepository.class);
        service = new TaskService(tasks, mock(ItemRepository.class), users);
        commis = new User();
        commis.setId(7L);
        commis.setEmail("commis@example.test");
        commis.setName("Commis");
        commis.setActive(true);
        when(users.findByEmail(commis.getEmail())).thenReturn(Optional.of(commis));
        authenticate("COMMIS");
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void commisCannotOverrideOwnIdAndAllFiltersRemainApplied() {
        LocalDate date = LocalDate.of(2026, 11, 26);
        when(tasks.findFiltered(date, TaskStatus.EN_COURS, 7L, 3L)).thenReturn(List.of());
        service.findAll(date, TaskStatus.EN_COURS, 99L, 3L);
        verify(tasks).findFiltered(date, TaskStatus.EN_COURS, 7L, 3L);
    }

    @Test
    void commisListWithoutFiltersStillRestrictsOwner() {
        when(tasks.findFiltered(null, null, 7L, null)).thenReturn(List.of());
        service.findAll(null, null, null, null);
        verify(tasks).findFiltered(null, null, 7L, null);
    }

    @Test
    void chefCanFilterAnotherUsersTasks() {
        authenticate("CHEF");
        LocalDate date = LocalDate.of(2026, 11, 26);
        when(tasks.findFiltered(date, TaskStatus.A_FAIRE, 99L, 3L)).thenReturn(List.of());
        service.findAll(date, TaskStatus.A_FAIRE, 99L, 3L);
        verify(tasks).findFiltered(date, TaskStatus.A_FAIRE, 99L, 3L);
    }

    @Test
    void commisCannotReadOrChangeAnotherUsersTask() {
        User other = new User();
        other.setId(99L);
        givenTask(other);
        assertThrows(AccessDeniedException.class, () -> service.findById(10L));
        assertThrows(AccessDeniedException.class, () -> service.start(10L));
        assertThrows(AccessDeniedException.class, () -> service.done(10L));
        verify(tasks, never()).save(any());
    }

    @Test
    void unassignedTasksAreNotAvailableToCommis() {
        givenTask(null);
        assertThrows(AccessDeniedException.class, () -> service.findById(10L));
        assertThrows(AccessDeniedException.class, () -> service.start(10L));
        assertThrows(AccessDeniedException.class, () -> service.done(10L));
        verify(tasks, never()).save(any());
    }

    @Test
    void commisCanReadStartAndCompleteOwnTask() {
        givenTask(commis);
        assertEquals(7L, service.findById(10L).getAssignedUserId());
        assertEquals(TaskStatus.EN_COURS, service.start(10L).getStatus());
        assertEquals(TaskStatus.TERMINEE, service.done(10L).getStatus());
        verify(tasks, times(2)).save(any());
    }

    @Test
    void administratorCanReadAndUpdateUnassignedTask() {
        authenticate("ADMIN");
        givenTask(null);
        assertEquals(10L, service.findById(10L).getId());
        assertEquals(TaskStatus.EN_COURS, service.start(10L).getStatus());
    }

    @Test
    void missingAuthenticationFailsClosed() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.findAll(null, null, null, null));
        verifyNoInteractions(tasks);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("commis@example.test", null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void givenTask(User assignedUser) {
        Task task = new Task();
        task.setId(10L);
        task.setAssignedUser(assignedUser);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(tasks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
