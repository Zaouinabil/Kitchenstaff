package be.kitchenstaff.entity;

import java.time.LocalDate;
import java.time.LocalTime;

public class DailyMenu {
    private Long id;
    private Menu menu;
    private LocalDate serviceDate;
    private Integer guestCount;
    private LocalTime serviceTime;
}
