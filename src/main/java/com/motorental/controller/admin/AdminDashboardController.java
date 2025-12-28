package com.motorental.controller.admin;

import com.motorental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
// SỬA LỖI 1: Đổi "/templates/admin" thành "/admin"
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getDashboardStats());

        // SỬA LỖI 2: View resolver mặc định tìm trong resources/templates/
        // Nên chỉ cần trả về "admin/dashboard", không cần prefix "templates/"
        return "admin/dashboard";
    }
}