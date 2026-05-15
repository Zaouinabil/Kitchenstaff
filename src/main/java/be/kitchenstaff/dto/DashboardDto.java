package be.kitchenstaff.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DashboardDto {

    private LocalDate date;

    private long totalTasks;

    private long todoTasks;

    private long inProgressTasks;

    private long doneTasks;

    private long cancelledTasks;

    private int progressPercentage;
}