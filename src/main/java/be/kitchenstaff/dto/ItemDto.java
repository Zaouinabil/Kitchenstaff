package be.kitchenstaff.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ItemDto {
    private Long id;
    private String name;
    private String unit;
    private Boolean active;
    private Long categoryId;
    private String categoryName;

}
