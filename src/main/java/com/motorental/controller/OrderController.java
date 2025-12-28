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

    // Trang thanh toán (Checkout)
    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String userId = getUserId(userDetails);
        CartDto cart = cartService.getCartByUserId(userId);

        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("orderRequest", new CreateOrderDto());
        return "orders/checkout";
    }

    // Tạo đơn hàng
    @PostMapping("/orders/create")
    public String createOrder(@AuthenticationPrincipal UserDetails userDetails,
                              @Valid @ModelAttribute("orderRequest") CreateOrderDto dto,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails);
            OrderDto order = orderService.createOrderFromCart(userId, dto);
            // Redirect sang trang thanh toán của đơn hàng vừa tạo
            return "redirect:/payments/create/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

    // Danh sách đơn hàng của tôi
    @GetMapping("/my-orders")
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String userId = getUserId(userDetails);
        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        model.addAttribute("orders", orders);
        return "orders/my-orders";
    }

    // Chi tiết đơn hàng
    @GetMapping("/orders/{id}")
    public String orderDetail(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long id,
                              Model model) {
        OrderDto order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "orders/detail";
    }

    // Hủy đơn hàng
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
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow().getId();
    }
}