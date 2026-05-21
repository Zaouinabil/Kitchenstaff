package be.kitchenstaff.service;

import be.kitchenstaff.dto.CreateUserRequest;
import be.kitchenstaff.dto.UserDto;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.exception.ResourceNotFoundException;
import be.kitchenstaff.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        user.setPassword(request.getPassword());
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
}