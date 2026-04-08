package com.motorental.controller.admin;

import com.motorental.service.DashboardService;
import com.motorental.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final OrderService orderService;
    private final com.motorental.service.VehicleService vehicleService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getDashboardStats());
        model.addAttribute("recentOrders", orderService.getAllOrders());
        return "admin/dashboard";
    }

    @GetMapping("/map")
    public String vehicleMap(Model model) {
        model.addAttribute("vehicles", 
            vehicleService.searchVehiclesDetail("", null, null, org.springframework.data.domain.Pageable.unpaged()).getContent());
        return "admin/map";
    }
}