package com.motorental.controller;

import com.motorental.dto.user.UserDto;
import com.motorental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        // Lấy thông tin user hiện tại
        UserDto userDto = userService.getUserProfile(principal.getName());
        model.addAttribute("userDto", userDto);
        return "user/profile";
    }

    @PostMapping("/user/profile/update")
    public String updateProfile(@ModelAttribute("userDto") UserDto userDto,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserProfile(principal.getName(), userDto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}