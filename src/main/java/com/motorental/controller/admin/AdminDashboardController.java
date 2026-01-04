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
    private final OrderService orderService; // Dùng để lấy đơn hàng gần đây

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Truyền DTO stats vào model để view (dashboard.html) sử dụng
        model.addAttribute("stats", dashboardService.getDashboardStats());

        // Truyền danh sách đơn hàng (Sử dụng hàm có sẵn của OrderService)
        // Nếu bạn chưa có hàm getRecentOrders, có thể dùng getAllOrders tạm thời
        model.addAttribute("recentOrders", orderService.getAllOrders());

        return "admin/dashboard";
    }
}