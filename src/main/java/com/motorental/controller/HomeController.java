package com.motorental.controller;

import com.motorental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final VehicleService vehicleService;

    // Inject token từ application.properties
    @Value("${mapbox.token}")
    private String mapboxToken;

    @GetMapping("/")
    public String index(Model model) {
        // FIX LỖI: Gọi đúng hàm getPopularVehicles() có trong Service
        // Đặt tên attribute là "popularVehicles" để khớp với index.html cũ của bạn
        model.addAttribute("popularVehicles", vehicleService.getPopularVehicles());

        // Thêm title để tránh lỗi layout
        model.addAttribute("title", "Trang chủ");
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "Giới thiệu");
        return "about";
    }

    @GetMapping("/branches")
    public String branches(Model model) {
        // Truyền token xuống để file branches.html sử dụng
        model.addAttribute("mapboxToken", mapboxToken);
        model.addAttribute("title", "Chi nhánh cửa hàng");
        return "branches";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Liên hệ");
        return "contact";
    }
}