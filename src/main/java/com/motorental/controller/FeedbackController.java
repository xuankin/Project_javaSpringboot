package com.motorental.controller;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.repository.UserRepository;
import com.motorental.service.FeedbackService;
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
                              @ModelAttribute FeedbackDto dto,
                              RedirectAttributes redirectAttributes) {
        // [Safety Check] Nếu xe không tồn tại hoặc ID null, về trang chủ
        if (dto.getVehicleId() == null) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: Không tìm thấy mã xe.");
            return "redirect:/vehicles";
        }

        try {
            // 1. Kiểm tra đăng nhập (Spring Security thường đã chặn, nhưng check thêm cho chắc)
            if (userDetails == null) {
                return "redirect:/login";
            }

            // 2. Validate rating
            if (dto.getRating() == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn số sao để đánh giá.");
                return "redirect:/vehicles/detail/" + dto.getVehicleId();
            }

            // 3. Lấy User ID từ Database (dựa trên username trong session)
            String userId = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"))
                    .getId();

            // 4. Gọi Service tạo đánh giá (Logic không cần OrderId)
            feedbackService.createFeedback(userId, dto);

            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã chia sẻ đánh giá!");

        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi ra console server
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        // 5. Chuyển hướng quay lại trang chi tiết chiếc xe vừa đánh giá
        return "redirect:/vehicles/detail/" + dto.getVehicleId();
    }
}