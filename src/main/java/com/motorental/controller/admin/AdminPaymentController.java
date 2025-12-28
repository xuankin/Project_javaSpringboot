package com.motorental.controller.admin;

import com.motorental.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/payments") // SỬA: Bỏ "templates/"
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "admin/payments/list"; // SỬA: Bỏ "templates/"
    }

    @PostMapping("/{orderId}/confirm-cash")
    public String confirmCash(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            paymentService.processCashPayment(orderId);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận thanh toán tiền mặt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}