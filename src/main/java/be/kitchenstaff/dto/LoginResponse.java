package be.kitchenstaff.dto;

import be.kitchenstaff.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private Long userId;

    private String name;

    private String email;

    private Role role;

    private String token;

    private String tokenType;

    private String message;
}