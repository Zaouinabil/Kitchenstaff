package be.kitchenstaff.service;

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
import be.kitchenstaff.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;

@Service
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

    public List<TaskDto> findAll(
            LocalDate date,
            TaskStatus status,
            Long assignedUserId,
            Long categoryId
    ) {
        List<Task> tasks;

        if (date != null && status != null && assignedUserId != null) {
            tasks = taskRepository.findByTaskDateAndStatusAndAssignedUserId(date, status, assignedUserId);

        } else if (date != null && status != null && categoryId != null) {
            tasks = taskRepository.findByTaskDateAndStatusAndItemCategoryId(date, status, categoryId);

        } else if (date != null && status != null) {
            tasks = taskRepository.findByTaskDateAndStatus(date, status);

        } else if (date != null && assignedUserId != null) {
            tasks = taskRepository.findByTaskDateAndAssignedUserId(date, assignedUserId);

        } else if (date != null && categoryId != null) {
            tasks = taskRepository.findByTaskDateAndItemCategoryId(date, categoryId);

        } else if (status != null && categoryId != null) {
            tasks = taskRepository.findByStatusAndItemCategoryId(status, categoryId);

        } else if (status != null) {
            tasks = taskRepository.findByStatus(status);

        } else if (assignedUserId != null) {
            tasks = taskRepository.findByAssignedUserId(assignedUserId);

        } else if (categoryId != null) {
            tasks = taskRepository.findByItemCategoryId(categoryId);

        } else if (date != null) {
            tasks = taskRepository.findByTaskDateOrderByIdDesc(date);

        } else {
            tasks = taskRepository.findAllByOrderByIdDesc();
        }

        return tasks.stream()
                .map(this::toDto)
                .toList();
    }

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
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));
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