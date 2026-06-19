package be.kitchenstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateItemRequest {

    @NotBlank(message = "Le nom de la préparation est obligatoire")
    private String name;

    @NotBlank(message = "L'unité est obligatoire")
    private String unit;

    @NotNull(message = "La catégorie est obligatoire")
    private Long categoryId;
}