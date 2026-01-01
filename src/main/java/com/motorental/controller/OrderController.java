package com.motorental.controller;

import com.motorental.dto.cart.CartDto;
import com.motorental.dto.order.CreateOrderDto;
import com.motorental.dto.order.OrderDto;
import com.motorental.repository.UserRepository;
import com.motorental.service.CartService;
import com.motorental.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String userId = getUserId(userDetails);
        CartDto cart = cartService.getCartByUserId(userId);

        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("order", new CreateOrderDto());
        return "orders/checkout";
    }

    @PostMapping("/orders/create")
    public String createOrder(@AuthenticationPrincipal UserDetails userDetails,
                              @Valid @ModelAttribute("order") CreateOrderDto dto,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails);
            OrderDto order = orderService.createOrderFromCart(userId, dto);

            // --- ĐOẠN ĐÃ SỬA: Phân luồng thanh toán ---

            // 1. Nếu là Tiền mặt (CASH) -> Xong luôn, về trang chi tiết
            if ("CASH".equals(dto.getPaymentMethod())) {
                redirectAttributes.addFlashAttribute("success", "Đặt xe thành công! Vui lòng thanh toán khi nhận xe.");
                return "redirect:/orders/" + order.getId();
            }

            // 2. Nếu là VNPay (hoặc Online khác) -> Chuyển sang trang thanh toán
            return "redirect:/payments/create/" + order.getId();

            // ------------------------------------------

        } catch (Exception e) {
            // In lỗi ra log để debug (tùy chọn)
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo đơn hàng: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/my-orders")
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String userId = getUserId(userDetails);
        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        model.addAttribute("orders", orders);
        return "orders/my-orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long id,
                              Model model) {
        // Có thể thêm check quyền sở hữu đơn hàng ở đây nếu cần bảo mật kỹ hơn
        OrderDto order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "orders/detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails);
            orderService.cancelOrder(id, userId);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-orders";
    }

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}