package com.motorental.controller;

import com.motorental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final VehicleService vehicleService;

    @GetMapping("/")
    public String home(Model model) {
        // Hiển thị top xe phổ biến ra trang chủ
        model.addAttribute("popularVehicles", vehicleService.getPopularVehicles());
        return "index";
    }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/contact")
    public String contact() { return "contact"; }
}