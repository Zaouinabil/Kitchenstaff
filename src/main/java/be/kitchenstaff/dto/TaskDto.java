package be.kitchenstaff.dto;

import be.kitchenstaff.enums.TaskPriority;
import be.kitchenstaff.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class TaskDto {

    private Long id;

    private LocalDate taskDate;

    private TaskStatus status;

    private TaskPriority priority;

    private BigDecimal quantity;

    private String comment;

    private Long itemId;

    private String itemName;

    private String itemUnit;

    private Long categoryId;

    private String categoryName;

    private Long assignedUserId;

    private String assignedUserName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}