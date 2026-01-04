package com.motorental.controller;

import com.motorental.dto.cart.CartDto;
import com.motorental.dto.order.CreateOrderDto;
import com.motorental.dto.order.OrderDto;
import com.motorental.dto.user.UserDto;
import com.motorental.service.CartService;
import com.motorental.service.OrderService;
import com.motorental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    // --- Endpoint hiển thị trang Checkout ---
    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        try {
            String userId = userService.findByUsername(principal.getName()).getId();
            CartDto cart = cartService.getCartByUserId(userId);

            if (cart.getItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống, vui lòng chọn xe trước.");
                return "redirect:/vehicles";
            }

            model.addAttribute("cart", cart);
            model.addAttribute("order", new CreateOrderDto()); // Object để bind form
            return "orders/checkout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    // --- [ĐÃ SỬA] Xử lý tạo đơn hàng và điều hướng thanh toán ---
    @PostMapping("/create")
    public String createOrder(@ModelAttribute("order") CreateOrderDto createOrderDto,
                              BindingResult result,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            String userId = userService.findByUsername(principal.getName()).getId();

            // 1. Tạo đơn hàng (Lưu xuống DB)
            OrderDto order = orderService.createOrderFromCart(userId, createOrderDto);

            // 2. [QUAN TRỌNG] Kiểm tra phương thức thanh toán để điều hướng
            // Nếu khách chọn VNPAY -> Chuyển sang PaymentController để tạo URL
            if ("VNPAY".equalsIgnoreCase(createOrderDto.getPaymentMethod())) {
                return "redirect:/payments/vnpay/" + order.getId();
            }

            // Nếu là Tiền mặt (hoặc mặc định) -> Về trang chi tiết đơn
            redirectAttributes.addFlashAttribute("success", "Đặt xe thành công! Mã đơn: " + order.getOrderCode());
            return "redirect:/orders/detail/" + order.getId();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    // --- Các hàm cũ giữ nguyên ---
    @GetMapping("/my-orders")
    public String myOrders(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String userId = userService.findByUsername(principal.getName()).getId();
        model.addAttribute("orders", orderService.getOrdersByUserId(userId));

        return "orders/my-orders";
    }

    @GetMapping("/detail/{id}")
    public String viewOrderDetail(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        try {
            OrderDto order = orderService.getOrderById(id);
            UserDto currentUser = userService.findByUsername(principal.getName());

            if (!order.getUserEmail().equals(currentUser.getEmail())) {
                return "redirect:/error/403";
            }

            model.addAttribute("order", order);
            return "orders/detail";
        } catch (Exception e) {
            return "redirect:/orders/my-orders";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            String userId = userService.findByUsername(principal.getName()).getId();
            orderService.cancelOrder(id, userId);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/orders/detail/" + id;
    }
}