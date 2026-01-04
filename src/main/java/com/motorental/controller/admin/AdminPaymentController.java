package com.motorental.controller.admin;

import com.motorental.dto.payment.PaymentDto;
import com.motorental.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public String listPayments(Model model) {
        List<PaymentDto> payments = paymentService.getAllPayments();
        model.addAttribute("payments", payments);
        return "admin/payments/list";
    }

    // --- Endpoint xác nhận thanh toán ---
    @PostMapping("/confirm/{id}")
    public String confirmPayment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.confirmPayment(id);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận thanh toán thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}