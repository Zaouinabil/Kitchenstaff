package be.kitchenstaff.service;

import be.kitchenstaff.dto.DashboardDto;
import be.kitchenstaff.enums.TaskStatus;
import be.kitchenstaff.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DashboardService {

    private final TaskRepository taskRepository;

    public DashboardService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public DashboardDto getDashboardByDate(LocalDate date) {
        long todoTasks = taskRepository.countByTaskDateAndStatus(date, TaskStatus.A_FAIRE);
        long inProgressTasks = taskRepository.countByTaskDateAndStatus(date, TaskStatus.EN_COURS);
        long doneTasks = taskRepository.countByTaskDateAndStatus(date, TaskStatus.TERMINEE);
        long cancelledTasks = taskRepository.countByTaskDateAndStatus(date, TaskStatus.ANNULEE);

        long totalTasks = todoTasks + inProgressTasks + doneTasks + cancelledTasks;

        int progressPercentage = calculateProgress(totalTasks, doneTasks);

        DashboardDto dto = new DashboardDto();
        dto.setDate(date);
        dto.setTotalTasks(totalTasks);
        dto.setTodoTasks(todoTasks);
        dto.setInProgressTasks(inProgressTasks);
        dto.setDoneTasks(doneTasks);
        dto.setCancelledTasks(cancelledTasks);
        dto.setProgressPercentage(progressPercentage);

        return dto;
    }

    public DashboardDto getTodayDashboard() {
        return getDashboardByDate(LocalDate.now());
    }

    private int calculateProgress(long totalTasks, long doneTasks) {
        if (totalTasks == 0) {
            return 0;
        }

        return (int) Math.round((doneTasks * 100.0) / totalTasks);
    }
}