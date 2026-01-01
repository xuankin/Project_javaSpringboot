package com.motorental.service;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.entity.Feedback;
import com.motorental.entity.User;
import com.motorental.entity.Vehicle;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.UserRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    // Đã xóa RentalOrderRepository vì không cần check đơn hàng nữa
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public List<FeedbackDto> getFeedbacksByVehicleId(Long vehicleId) {
        return feedbackRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId, Pageable.unpaged())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<FeedbackDto> getAllFeedbacks() {
        return feedbackRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createFeedback(String userId, FeedbackDto dto) {
        // 1. Lấy thông tin User và Vehicle
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));

        // 2. (Tùy chọn) Check xem User này đã đánh giá xe này chưa nếu muốn chặn spam
        // if (feedbackRepository.existsByUserAndVehicle(user, vehicle)) { ... }

        // 3. Tạo Feedback mới mà KHÔNG cần check Order
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setVehicle(vehicle);
        feedback.setRating(dto.getRating());
        feedback.setContent(dto.getComment());

        feedbackRepository.save(feedback);
    }

    @Transactional
    public void deleteFeedback(Long id, String userId) {
        Feedback fb = feedbackRepository.findById(id).orElseThrow();
        // Chỉ cho phép xóa nếu là chủ sở hữu comment (hoặc admin xử lý riêng)
        if (userId != null && !fb.getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền xóa đánh giá này");
        }
        feedbackRepository.delete(fb);
    }

    private FeedbackDto mapToDto(Feedback fb) {
        return FeedbackDto.builder()
                .id(fb.getId())
                .vehicleId(fb.getVehicle().getId())
                .vehicleName(fb.getVehicle().getName())
                .userName(fb.getUser().getUsername())
                .rating(fb.getRating())
                .comment(fb.getContent())
                .createdAt(fb.getCreatedAt())
                .build();
    }
}