package com.motorental.controller;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.repository.UserRepository;
import com.motorental.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public String addFeedback(@AuthenticationPrincipal UserDetails userDetails,
                              @Valid @ModelAttribute FeedbackDto dto,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = userRepository.findByUsername(userDetails.getUsername()).orElseThrow().getId();
            feedbackService.createFeedback(userId, dto);
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles/" + dto.getVehicleId();
    }
}