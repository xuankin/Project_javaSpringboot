package com.motorental.controller;

import com.motorental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final VehicleService vehicleService;

    // Inject token từ application.properties
    @Value("${mapbox.token}")
    private String mapboxToken;

    @GetMapping("/")
    public String index(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        Model model) {
        int pageSize = 6;
        var vehiclePage = vehicleService.searchVehiclesDetail(keyword, null, null,
                PageRequest.of(page, pageSize));

        model.addAttribute("vehicles", vehiclePage.getContent());
        model.addAttribute("currentPage", vehiclePage.getNumber());
        model.addAttribute("totalPages", vehiclePage.getTotalPages());
        model.addAttribute("keyword", keyword);
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