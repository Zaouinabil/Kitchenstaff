package be.kitchenstaff.controller;

import be.kitchenstaff.dto.DashboardDto;
import be.kitchenstaff.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/dashboard/today")
    public DashboardDto today() {
        return dashboardService.getTodayDashboard();
    }

    @GetMapping("/api/v1/dashboard")
    public DashboardDto byDate(@RequestParam LocalDate date) {
        return dashboardService.getDashboardByDate(date);
    }
}