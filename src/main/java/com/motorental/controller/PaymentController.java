package com.motorental.controller;

import com.motorental.dto.payment.CreatePaymentDto;
import com.motorental.dto.payment.PaymentDto;
import com.motorental.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Trang chọn phương thức thanh toán
    @GetMapping("/create/{orderId}")
    public String paymentPage(@PathVariable Long orderId, Model model) {
        PaymentDto existingPayment = paymentService.getPaymentByOrderId(orderId);
        if (existingPayment != null && "COMPLETED".equals(existingPayment.getPaymentStatus())) {
            return "redirect:/orders/" + orderId;
        }

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);

        model.addAttribute("orderId", orderId);
        model.addAttribute("paymentRequest", dto);
        return "payments/create";
    }

    // Xử lý chuyển hướng VNPay
    @PostMapping("/vnpay")
    public String payViaVNPay(@ModelAttribute CreatePaymentDto dto) {
        // Dùng service tạo URL thanh toán VNPay
        String paymentUrl = paymentService.createVNPayPaymentUrl(dto);
        return "redirect:" + paymentUrl;
    }

    // Callback từ VNPay trả về
    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam("vnp_TxnRef") String txnRef,
                                @RequestParam("vnp_ResponseCode") String responseCode,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.processVNPayCallback(txnRef, responseCode);
            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi thanh toán: " + e.getMessage());
        }
        return "redirect:/my-orders";
    }
}