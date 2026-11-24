package be.kitchenstaff.service;
import org.springframework.transaction.annotation.Transactional;
import be.kitchenstaff.dto.CreateTaskRequest;
import be.kitchenstaff.dto.TaskDto;
import be.kitchenstaff.dto.UpdateTaskRequest;
import be.kitchenstaff.entity.Item;
import be.kitchenstaff.entity.Task;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.TaskPriority;
import be.kitchenstaff.enums.TaskStatus;
import be.kitchenstaff.exception.ResourceNotFoundException;
import be.kitchenstaff.repository.ItemRepository;
import be.kitchenstaff.repository.TaskRepository;
import be.kitchenstaff.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            ItemRepository itemRepository,
            UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }
    @Transactional(readOnly = true)
    public List<TaskDto> findAll(
            LocalDate date,
            TaskStatus status,
            Long assignedUserId,
            Long categoryId
    ) {
        Long commisId = currentCommisId();
        Long effectiveAssignedUserId = commisId != null ? commisId : assignedUserId;
        List<Task> tasks = taskRepository.findFiltered(date, status, effectiveAssignedUserId, categoryId);

        return tasks.stream()
                .map(this::toDto)
                .toList();
    }
    @Transactional(readOnly = true)
    public TaskDto findById(Long id) {
        Task task = getTaskOrThrow(id);

        return toDto(task);
    }

    public TaskDto create(CreateTaskRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Préparation introuvable"));

        Task task = new Task();
        task.setItem(item);
        task.setTaskDate(request.getTaskDate() != null ? request.getTaskDate() : LocalDate.now());
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.NORMALE);
        task.setQuantity(request.getQuantity());
        task.setComment(request.getComment());
        task.setStatus(TaskStatus.A_FAIRE);

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

            task.setAssignedUser(user);
        }

        Task savedTask = taskRepository.save(task);

        return toDto(savedTask);
    }

    public TaskDto update(Long id, UpdateTaskRequest request) {
        Task task = getTaskOrThrow(id);

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Préparation introuvable"));

        task.setTaskDate(request.getTaskDate());
        task.setItem(item);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.NORMALE);
        task.setQuantity(request.getQuantity());
        task.setComment(request.getComment());

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

            task.setAssignedUser(user);
        } else {
            task.setAssignedUser(null);
        }

        Task savedTask = taskRepository.save(task);

        return toDto(savedTask);
    }
    public TaskDto unassign(Long id) {
        Task task = getTaskOrThrow(id);

        task.setAssignedUser(null);

        Task savedTask = taskRepository.save(task);

        return toDto(savedTask);
    }

    public TaskDto start(Long id) {
        Task task = getTaskOrThrow(id);

        task.setStatus(TaskStatus.EN_COURS);

        Task savedTask = taskRepository.save(task);

        return toDto(savedTask);
    }

    public TaskDto done(Long id) {
        Task task = getTaskOrThrow(id);

        task.setStatus(TaskStatus.TERMINEE);

        Task savedTask = taskRepository.save(task);

        return toDto(savedTask);
    }

    public TaskDto cancel(Long id) {
        Task task = getTaskOrThrow(id);

        task.setStatus(TaskStatus.ANNULEE);

        Task savedTask = taskRepository.save(task);

        return toDto(savedTask);
    }

    public void delete(Long id) {
        Task task = getTaskOrThrow(id);

        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));
        Long commisId = currentCommisId();
        if (commisId != null && (task.getAssignedUser() == null
                || !commisId.equals(task.getAssignedUser().getId()))) {
            throw new AccessDeniedException("Cette tâche ne vous est pas attribuée");
        }
        return task;
    }

    private Long currentCommisId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentification requise");
        }
        if (authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_CHEF"))) {
            return null;
        }
        if (authentication.getAuthorities().stream().noneMatch(authority ->
                authority.getAuthority().equals("ROLE_COMMIS"))) {
            throw new AccessDeniedException("Accès interdit");
        }
        return userRepository.findByEmail(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur actif introuvable"));
    }

    private TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();

        dto.setId(task.getId());
        dto.setTaskDate(task.getTaskDate());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setQuantity(task.getQuantity());
        dto.setComment(task.getComment());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        if (task.getItem() != null) {
            dto.setItemId(task.getItem().getId());
            dto.setItemName(task.getItem().getName());
            dto.setItemUnit(task.getItem().getUnit());

            if (task.getItem().getCategory() != null) {
                dto.setCategoryId(task.getItem().getCategory().getId());
                dto.setCategoryName(task.getItem().getCategory().getName());
            }
        }

        if (task.getAssignedUser() != null) {
            dto.setAssignedUserId(task.getAssignedUser().getId());
            dto.setAssignedUserName(task.getAssignedUser().getName());
        }

        return dto;
    }
}
