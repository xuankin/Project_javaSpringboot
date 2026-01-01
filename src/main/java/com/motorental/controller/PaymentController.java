package com.motorental.controller;

import com.motorental.dto.payment.CreatePaymentDto;
import com.motorental.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/my-payments")
    public String viewMyPayments(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        // TODO: Thay thế emptyList bằng dữ liệu thực từ paymentService
        model.addAttribute("payments", Collections.emptyList());

        return "payments/my-payments";
    }

    @GetMapping("/payments/create/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "payments/create";
    }

    @PostMapping("/payments/cash/{orderId}")
    public String payCash(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            // TODO: Gọi service xử lý thanh toán tiền mặt tại đây

            redirectAttributes.addFlashAttribute("success", "Đã gửi yêu cầu thanh toán tiền mặt. Vui lòng đến cửa hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/my-orders";
    }

    @PostMapping("/payments/vnpay/{orderId}")
    public String payVnPay(@PathVariable Long orderId,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            CreatePaymentDto dto = new CreatePaymentDto();
            dto.setOrderId(orderId);

            String paymentUrl = paymentService.createVNPayPaymentUrl(dto, request);
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể tạo thanh toán VNPay: " + e.getMessage());
            return "redirect:/payments/create/" + orderId;
        }
    }

    @GetMapping("/payments/vnpay/callback")
    public String vnpayCallback(@RequestParam(value = "vnp_TxnRef", required = false) String txnRef,
                                @RequestParam(value = "vnp_ResponseCode", required = false) String responseCode,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        try {
            if (txnRef == null || responseCode == null) {
                return "redirect:/";
            }

            paymentService.processVNPayCallback(request);

            if ("00".equals(responseCode)) {
                redirectAttributes.addFlashAttribute("success", "Thanh toán VNPay thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại hoặc bị hủy. Mã lỗi: " + responseCode);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý kết quả: " + e.getMessage());
        }
        return "redirect:/my-orders";
    }
}