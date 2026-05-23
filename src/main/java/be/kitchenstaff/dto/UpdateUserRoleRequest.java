package be.kitchenstaff.dto;

import be.kitchenstaff.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;
}