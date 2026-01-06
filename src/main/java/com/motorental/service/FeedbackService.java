package com.motorental.service;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.entity.Feedback;
import com.motorental.entity.User;
import com.motorental.entity.Vehicle;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.RentalOrderRepository; // Đã thêm
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
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalOrderRepository rentalOrderRepository; // Cần thiết để check đơn hàng

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

        // 2. [QUAN TRỌNG] Kiểm tra xem User đã thuê xe này và hoàn thành chuyến đi chưa
        boolean hasRented = rentalOrderRepository.hasUserRentedVehicle(userId, dto.getVehicleId());

        if (!hasRented) {
            throw new RuntimeException("Bạn cần hoàn thành ít nhất một chuyến đi với xe này để có thể đánh giá.");
        }

        // 3. (Tùy chọn) Chặn spam: Nếu user đã đánh giá rồi thì không cho đánh giá thêm (hoặc cho phép sửa)
        // Hiện tại tạm thời cho phép đánh giá nhiều lần nếu thuê nhiều lần (hoặc comment logic này nếu muốn)
        /*
        if (feedbackRepository.existsByUserIdAndVehicleId(userId, dto.getVehicleId())) {
             throw new RuntimeException("Bạn đã đánh giá xe này rồi.");
        }
        */

        // 4. Tạo Feedback mới
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