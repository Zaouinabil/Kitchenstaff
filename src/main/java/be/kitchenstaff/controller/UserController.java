package be.kitchenstaff.controller;

import be.kitchenstaff.dto.CreateUserRequest;
import be.kitchenstaff.dto.UpdateUserRoleRequest;
import be.kitchenstaff.dto.UserDto;
import be.kitchenstaff.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import be.kitchenstaff.dto.UpdateUserRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserDto findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
    @PatchMapping("/{id}/role")
    public UserDto updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return userService.updateRole(id, request);
    }
    @PatchMapping("/{id}/deactivate")
    public UserDto deactivate(@PathVariable Long id) {
        return userService.deactivate(id);
    }
    @PatchMapping("/{id}/reactivate")
    public UserDto reactivate(@PathVariable Long id) {
        return userService.reactivate(id);
    }
    @PutMapping("/{id}")
    public UserDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(id, request);
    }

}