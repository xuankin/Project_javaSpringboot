package com.motorental.controller;

import com.motorental.dto.payment.CreatePaymentDto;
import com.motorental.entity.Payment;
import com.motorental.entity.RentalOrder;
import com.motorental.repository.PaymentRepository;
import com.motorental.repository.RentalOrderRepository;
import com.motorental.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RentalOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    // --- Các hàm cũ giữ nguyên ---
    @GetMapping("/my-payments")
    public String viewMyPayments(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("payments", paymentService.getPaymentsByUserId(principal.getName()));
        return "payments/my-payments";
    }

    @GetMapping("/payments/create/{orderId}")
    public String showPaymentPage(@PathVariable("orderId") Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "payments/create";
    }

    @PostMapping("/payments/cash/{orderId}")
    public String payCash(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            paymentService.createCashPayment(orderId);
            redirectAttributes.addFlashAttribute("success", "Đã gửi yêu cầu thanh toán tiền mặt.");
        } catch (Exception e) {
            log.error("[Cash Payment] Lỗi xử lý tiền mặt cho orderId={}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/payments/create/" + orderId;
        }
        return "redirect:/orders/my-orders";
    }

    // --- [MỚI] Trang xác nhận trước khi vào VNPay ---
    @GetMapping("/payments/vnpay/preview/{orderId}")
    public String showVnpayPreview(@PathVariable("orderId") Long orderId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            RentalOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            BigDecimal total = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;

            // Tạo mã giao dịch tạm (để hiển thị cho user, sẽ được tạo lại khi submit)
            String previewTxnRef = "VNP" + System.currentTimeMillis();

            // Format số tiền dạng tiếng Việt
            NumberFormat nf = NumberFormat.getInstance(Locale.of("vi", "VN"));
            String formattedAmount = nf.format(total) + " VNĐ";

            model.addAttribute("orderId", orderId);
            model.addAttribute("orderCode", order.getOrderCode());
            model.addAttribute("amount", total);
            model.addAttribute("formattedAmount", formattedAmount);
            model.addAttribute("previewTxnRef", previewTxnRef);
            model.addAttribute("orderInfo", "Thanh toan don hang " + order.getOrderCode());

            return "payments/vnpay-preview";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng: " + e.getMessage());
            return "redirect:/orders/my-orders";
        }
    }

    // --- [ĐÃ SỬA] Hàm xử lý VNPay: Hỗ trợ cả GET (từ redirect) và POST ---
    @RequestMapping(value = "/payments/vnpay/{orderId}", method = {RequestMethod.GET, RequestMethod.POST})
    public String payVnPay(@PathVariable("orderId") Long orderId,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            log.info("[VNPay] Tạo yêu cầu thanh toán cho orderId={}", orderId);

            CreatePaymentDto dto = new CreatePaymentDto();
            dto.setOrderId(orderId);

            String paymentUrl = paymentService.createVNPayPaymentUrl(dto, request);

            log.info("[VNPay] URL tạo thành công, redirect, orderId={}", orderId);
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            log.error("[VNPay] Lỗi tạo URL VNPay cho orderId={}: {}", orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo cổng thanh toán: " + e.getMessage());
            return "redirect:/orders/detail/" + orderId;
        }
    }

    // --- Callback (Return URL - phía client redirect về) ---
    @GetMapping("/payments/vnpay/callback")
    public String vnpayCallback(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        try {
            boolean isSuccess = paymentService.processVNPayCallback(request);
            String responseCode = request.getParameter("vnp_ResponseCode");
            String txnRef = request.getParameter("vnp_TxnRef");

            // Tự động tìm orderId để redirect hoặc hiển thị kết quả chi tiết
            Long orderId = paymentRepository.findByTransactionId(txnRef)
                    .map(p -> p.getRentalOrder() != null ? p.getRentalOrder().getId() : null)
                    .orElse(null);

            model.addAttribute("responseCode", responseCode);
            model.addAttribute("txnRef", txnRef);
            model.addAttribute("orderId", orderId);
            model.addAttribute("success", isSuccess);

            if (isSuccess) {
                log.info("[VNPay Callback] Payment success, txnRef={}", txnRef);
                return "redirect:/payments/success";
            } else {
                log.warn("[VNPay Callback] Payment failed, code={}, txnRef={}", responseCode, txnRef);
                redirectAttributes.addFlashAttribute("error", "Giao dịch thất bại (Mã: " + responseCode + ")");
                return "payments/vnpay-result";
            }

        } catch (Exception e) {
            log.error("[VNPay Callback] Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi xác thực: " + e.getMessage());
            return "redirect:/orders/my-orders";
        }
    }

    // --- [MỚI] IPN Endpoint - Server-to-Server từ VNPay ---
    @PostMapping("/payments/vnpay/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> vnpayIPN(HttpServletRequest request) {
        log.info("[VNPay IPN] Received IPN request from VNPay");
        Map<String, String> response = paymentService.processVNPayIPN(request);
        return ResponseEntity.ok(response);
    }

    // --- [MỚI] API kiểm tra trạng thái thanh toán (AJAX polling) ---
    @GetMapping("/api/payments/status/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    // --- [MỚI] Retry: cho phép user thử lại khi thanh toán thất bại ---
    @GetMapping("/payments/retry/{orderId}")
    public String retryPayment(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            RentalOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            Payment.PaymentStatus currentStatus = order.getPayment() != null
                    ? order.getPayment().getPaymentStatus()
                    : null;

            if (Payment.PaymentStatus.COMPLETED.equals(currentStatus)) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng đã thanh toán thành công, không cần thử lại.");
                return "redirect:/orders/my-orders";
            }

            log.info("[Retry Payment] Retrying VNPay for orderId={}", orderId);
            return "redirect:/payments/vnpay/preview/" + orderId;

        } catch (Exception e) {
            log.error("[Retry Payment] Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/orders/my-orders";
        }
    }

    @GetMapping("/payments/success")
    public String showSuccessPage() {
        return "payments/success";
    }
}