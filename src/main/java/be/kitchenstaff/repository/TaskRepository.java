package be.kitchenstaff.repository;

import be.kitchenstaff.entity.Task;
import be.kitchenstaff.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatusOrderByIdDesc(TaskStatus status);

    List<Task> findByTaskDateAndStatus(LocalDate taskDate, TaskStatus status);

    List<Task> findByAssignedUserIdOrderByIdDesc(Long userId);

    List<Task> findByTaskDateOrderByIdDesc(LocalDate taskDate);

    List<Task> findAllByOrderByIdDesc();

    long countByTaskDateAndStatus(LocalDate taskDate, TaskStatus status);

    List<Task> findByTaskDateAndAssignedUserId(LocalDate taskDate, Long assignedUserId);

    List<Task> findByTaskDateAndStatusAndAssignedUserId(
            LocalDate taskDate,
            TaskStatus status,
            Long assignedUserId
    );

    List<Task> findByItemCategoryId(Long categoryId);

    List<Task> findByTaskDateAndItemCategoryId(LocalDate taskDate, Long categoryId);

    List<Task> findByStatusAndItemCategoryId(TaskStatus status, Long categoryId);

    List<Task> findByTaskDateAndStatusAndItemCategoryId(
            LocalDate taskDate,
            TaskStatus status,
            Long categoryId
    );

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByAssignedUserId(Long assignedUserId);

    boolean existsByTaskDateAndItemIdAndAssignedUserIdAndQuantityAndComment(
            LocalDate taskDate,
            Long itemId,
            Long assignedUserId,
            BigDecimal quantity,
            String comment
    );
}
