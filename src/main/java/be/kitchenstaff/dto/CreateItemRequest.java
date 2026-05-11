package be.kitchenstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateItemRequest {
    @NotBlank(message = "Le nom de la préparation est obligatoire")

    @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")

    private String name;

    @NotBlank(message = "L'unité est obligatoire")

    @Size(max = 30, message = "L'unité ne peut pas dépasser 30 caractères")

    private String unit;

    @NotNull(message = "La catégorie est obligatoire")

    private Long categoryId;
}
