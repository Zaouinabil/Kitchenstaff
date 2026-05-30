package be.kitchenstaff.service;

import be.kitchenstaff.dto.CreateUserRequest;
import be.kitchenstaff.dto.UserDto;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.exception.ResourceNotFoundException;
import be.kitchenstaff.repository.UserRepository;
import org.springframework.stereotype.Service;
import be.kitchenstaff.dto.UpdateUserRoleRequest;
import be.kitchenstaff.dto.UpdateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public UserDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return toDto(user);
    }

    public UserDto create(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return toDto(savedUser);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setActive(user.getActive());

        return dto;
    }
    public UserDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setActive(false);

        User savedUser = userRepository.save(user);

        return toDto(savedUser);
    }
    public UserDto updateRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        return toDto(savedUser);
    }
    public UserDto reactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setActive(true);

        User savedUser = userRepository.save(user);

        return toDto(savedUser);
    }
    public UserDto update(Long id, UpdateUserRequest request) {

        User user = userRepository.findById(id)

                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setName(request.getName());

        user.setEmail(request.getEmail());

        User savedUser = userRepository.save(user);

        return toDto(savedUser);

    }
}