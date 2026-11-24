package be.kitchenstaff.repository;

import be.kitchenstaff.entity.Task;
import be.kitchenstaff.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            select t from Task t
            where (:date is null or t.taskDate = :date)
              and (:status is null or t.status = :status)
              and (:assignedUserId is null or t.assignedUser.id = :assignedUserId)
              and (:categoryId is null or t.item.category.id = :categoryId)
            order by t.id desc
            """)
    List<Task> findFiltered(
            @Param("date") LocalDate date,
            @Param("status") TaskStatus status,
            @Param("assignedUserId") Long assignedUserId,
            @Param("categoryId") Long categoryId
    );

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
