package be.kitchenstaff.dto;

import be.kitchenstaff.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {

    private Long id;

    private String name;

    private String email;

    private Role role;

    private Boolean active;
}