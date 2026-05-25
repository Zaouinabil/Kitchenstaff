package be.kitchenstaff.repository;

import be.kitchenstaff.entity.Task;
import be.kitchenstaff.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByTaskDate(LocalDate taskDate);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByTaskDateAndStatus(LocalDate taskDate, TaskStatus status);

    List<Task> findByAssignedUserId(Long userId);

    List<Task> findByTaskDateOrderByIdDesc(LocalDate taskDate);

    List<Task> findAllByOrderByIdDesc();

    long countByTaskDateAndStatus(LocalDate taskDate, TaskStatus status);

    List<Task> findByTaskDateAndAssignedUserId(LocalDate taskDate, Long assignedUserId);

    List<Task> findByTaskDateAndStatusAndAssignedUserId(
            LocalDate taskDate,
            TaskStatus status,
            Long assignedUserId
    );
}