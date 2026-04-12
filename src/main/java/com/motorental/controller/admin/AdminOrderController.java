package com.motorental.controller.admin;

import com.motorental.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String list(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            Model model) {
        
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(page, 10);
        org.springframework.data.domain.Page<com.motorental.dto.order.OrderDto> orderPage = orderService.getAllOrdersPageable(status, pageable);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("currentStatus", status);
        
        return "admin/orders/list";
    }

    @GetMapping("/detail/{id}")
    public String viewDetail(@PathVariable("id") Long id, Model model) {
        try {
            model.addAttribute("order", orderService.getOrderById(id));
            return "admin/orders/detail";
        } catch (Exception e) {
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        // Redirect về lại trang chi tiết để admin thấy ngay thay đổi
        return "redirect:/admin/orders/detail/" + id;
    }
}