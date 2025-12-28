package com.motorental.repository;

import com.motorental.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // STT 2: Vehicle Details - Hiển thị list đánh giá của xe (phân trang)
    Page<Feedback> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId, Pageable pageable);

    // STT 2: Vehicle Details - Tính điểm trung bình sao
    @Query("SELECT COALESCE(AVG(f.rating), 0.0) FROM Feedback f WHERE f.vehicle.id = :vehicleId")
    Double getAverageRatingByVehicleId(@Param("vehicleId") Long vehicleId);

    // STT 11: Feedback Management - Xem các đánh giá của User đó
    List<Feedback> findByUserIdOrderByCreatedAtDesc(String userId);
}