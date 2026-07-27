package be.kitchenstaff.dto;
import jakarta.validation.constraints.PositiveOrZero;
import be.kitchenstaff.enums.TaskPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateTaskRequest {

    private LocalDate taskDate;

    @NotNull(message = "La préparation est obligatoire")
    private Long itemId;

    private Long assignedUserId;

    private TaskPriority priority = TaskPriority.NORMALE;

    @DecimalMin(value = "0.0", inclusive = false, message = "La quantité doit être supérieure à 0")
    @PositiveOrZero(message = "La quantité doit être positive ou égale à zéro")
    private BigDecimal quantity;

    @Size(max = 255, message = "Le commentaire ne peut pas dépasser 255 caractères")
    private String comment;
}