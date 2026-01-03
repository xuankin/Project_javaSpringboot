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
                            @RequestParam(value = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails);

            // 2. Thêm xe vào giỏ
            cartService.addToCart(userId, dto);

            // 3. Nếu bấm "Thuê ngay" -> Sang trang Checkout
            if ("checkout".equals(redirect)) {
                return "redirect:/checkout";
            }

            // 4. Nếu bấm "Thêm vào giỏ" -> Quay lại trang chi tiết xe
            redirectAttributes.addFlashAttribute("success", "Đã thêm vào giỏ hàng!");

            // [SỬA LỖI TẠI ĐÂY] Thêm "/detail" vào đường dẫn redirect
            return "redirect:/vehicles/detail/" + dto.getVehicleId();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            // [SỬA LỖI TẠI ĐÂY] Cũng sửa đường dẫn khi có lỗi
            return "redirect:/vehicles/detail/" + dto.getVehicleId();
        }
    }

    @PostMapping("/remove/{itemId}")
    public String removeItem(@AuthenticationPrincipal UserDetails userDetails,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(userDetails);
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

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}