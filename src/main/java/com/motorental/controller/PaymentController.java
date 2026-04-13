package com.motorental.controller;

import com.motorental.dto.payment.CreatePaymentDto;
import com.motorental.entity.RentalOrder;
import com.motorental.repository.RentalOrderRepository;
import com.motorental.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RentalOrderRepository orderRepository;

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
            e.printStackTrace();
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
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
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
            boolean isSuccess = paymentService.processVNPayCallback(request);
            
            if (isSuccess) {
                return "redirect:/payments/success";
            } else {
                String responseCode = request.getParameter("vnp_ResponseCode");
                redirectAttributes.addFlashAttribute("error", "Giao dịch không thành công hoặc chữ ký không hợp lệ. (Mã: " + responseCode + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý thanh toán: " + e.getMessage());
        }
        return "redirect:/orders/my-orders";
    }

    @GetMapping("/payments/success")
    public String showSuccessPage() {
        return "payments/success";
    }
}