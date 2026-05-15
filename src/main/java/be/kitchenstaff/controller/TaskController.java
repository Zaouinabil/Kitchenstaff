package be.kitchenstaff.controller;

import be.kitchenstaff.dto.CreateTaskRequest;
import be.kitchenstaff.dto.TaskDto;
import be.kitchenstaff.dto.UpdateTaskRequest;
import be.kitchenstaff.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskDto> findAll(@RequestParam(required = false) LocalDate date) {
        return taskService.findAll(date);
    }

    @GetMapping("/{id}")
    public TaskDto findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/start")
    public TaskDto start(@PathVariable Long id) {
        return taskService.start(id);
    }

    @PatchMapping("/{id}/done")
    public TaskDto done(@PathVariable Long id) {
        return taskService.done(id);
    }

    @PatchMapping("/{id}/cancel")
    public TaskDto cancel(@PathVariable Long id) {
        return taskService.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}