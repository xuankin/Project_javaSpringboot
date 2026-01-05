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

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // --- Các hàm cũ giữ nguyên ---
    @GetMapping("/my-payments")
    public String viewMyPayments(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("payments", paymentService.getPaymentsByUserId(principal.getName()));
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
            paymentService.createCashPayment(orderId);
            redirectAttributes.addFlashAttribute("success", "Đã gửi yêu cầu thanh toán tiền mặt.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/payments/create/" + orderId;
        }
        return "redirect:/orders/my-orders";
    }

    // --- [ĐÃ SỬA] Hàm xử lý VNPay: Hỗ trợ cả GET (từ redirect) và POST ---
    @RequestMapping(value = "/payments/vnpay/{orderId}", method = {RequestMethod.GET, RequestMethod.POST})
    public String payVnPay(@PathVariable Long orderId,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            System.out.println(">>> Đang tạo yêu cầu thanh toán VNPay cho Order ID: " + orderId);

            CreatePaymentDto dto = new CreatePaymentDto();
            dto.setOrderId(orderId);

            // Gọi Service tạo URL (Logic trong Service giữ nguyên)
            String paymentUrl = paymentService.createVNPayPaymentUrl(dto, request);

            System.out.println(">>> URL VNPay thành công, đang chuyển hướng: " + paymentUrl);
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            System.err.println("!!! LỖI TẠO URL VNPAY !!!");
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("error", "Lỗi tạo cổng thanh toán: " + e.getMessage());
            return "redirect:/orders/detail/" + orderId; // Quay về trang chi tiết nếu lỗi
        }
    }

    // --- Hàm Callback giữ nguyên ---
    @GetMapping("/payments/vnpay/callback")
    public String vnpayCallback(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            paymentService.processVNPayCallback(request);
            String responseCode = request.getParameter("vnp_ResponseCode");

            if ("00".equals(responseCode)) {
                redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Đơn hàng đã được xác nhận.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Giao dịch thất bại. Mã lỗi: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi xác thực: " + e.getMessage());
        }
        return "redirect:/orders/my-orders";
    }
}