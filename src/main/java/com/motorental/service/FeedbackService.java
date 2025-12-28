package com.motorental.service;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.entity.Feedback;
import com.motorental.entity.User;
import com.motorental.entity.Vehicle;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.OrderDetailRepository;
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
    private final OrderDetailRepository orderDetailRepository;
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
        // Check điều kiện: Phải thuê xe đó và đơn hàng đã hoàn thành (COMPLETED)
        boolean canRate = orderDetailRepository.hasUserRentedVehicle(userId, dto.getVehicleId());

        if (!canRate) {
            throw new RuntimeException("Bạn phải hoàn thành chuyến đi với xe này mới được đánh giá.");
        }

        User user = userRepository.findById(userId).orElseThrow();
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId()).orElseThrow();

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setVehicle(vehicle);
        feedback.setRating(dto.getRating());
        feedback.setContent(dto.getContent());

        feedbackRepository.save(feedback);
    }

    @Transactional
    public void deleteFeedback(Long id, String userId) {
        Feedback fb = feedbackRepository.findById(id).orElseThrow();
        // Nếu có userId (user thường) -> Check quyền chính chủ
        if (userId != null && !fb.getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền xóa đánh giá này");
        }
        // Nếu userId null -> Admin -> Cho phép xóa
        feedbackRepository.delete(fb);
    }

    private FeedbackDto mapToDto(Feedback fb) {
        return FeedbackDto.builder()
                .id(fb.getId())
                .vehicleId(fb.getVehicle().getId())
                .vehicleName(fb.getVehicle().getName())
                .userName(fb.getUser().getUsername()) // hoặc fullName
                .rating(fb.getRating())
                .content(fb.getContent())
                .createdAt(fb.getCreatedAt())
                .build();
    }
}