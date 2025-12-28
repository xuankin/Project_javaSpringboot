package com.motorental.controller;

import com.motorental.dto.cart.AddToCartDto;
import com.motorental.dto.cart.CartDto;
import com.motorental.repository.UserRepository;
import com.motorental.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String userId = getUserId(userDetails);
        CartDto cart = cartService.getCartByUserId(userId);
        model.addAttribute("cart", cart);
        return "cart/view";
    }

    @PostMapping("/add")
    public String addToCart(@AuthenticationPrincipal UserDetails userDetails,
                            @Valid @ModelAttribute AddToCartDto dto,
                            RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails);
            cartService.addToCart(userId, dto);
            redirectAttributes.addFlashAttribute("success", "Đã thêm vào giỏ hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles/" + dto.getVehicleId();
    }

    @PostMapping("/remove/{itemId}")
    public String removeItem(@AuthenticationPrincipal UserDetails userDetails,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails); // Lấy userId để check quyền nếu cần
            cartService.removeFromCart(userId, itemId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa xe khỏi giỏ.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        cartService.clearCart(userId);
        return "redirect:/cart";
    }

    // Helper lấy ID từ UserDetails
    private String getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}