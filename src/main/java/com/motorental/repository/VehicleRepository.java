package com.motorental.repository;

import com.motorental.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // STT 1: Index Page - Tìm kiếm và Lọc (Tên, Hãng, Trạng thái)
    @Query("SELECT v FROM Vehicle v WHERE " +
            "(:keyword IS NULL OR lower(v.name) LIKE lower(concat('%', :keyword, '%')) OR lower(v.brand) LIKE lower(concat('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR v.status = :status)")
    Page<Vehicle> searchVehicles(@Param("keyword") String keyword,
                                 @Param("status") Vehicle.VehicleStatus status,
                                 Pageable pageable);

    // STT 1: Best Seller - Lấy top xe có lượt thuê cao nhất
    @Query("SELECT v FROM Vehicle v ORDER BY v.rentalCount DESC")
    List<Vehicle> findTopPopularVehicles(Pageable pageable);

    // STT 12: Admin Dashboard - Thống kê số lượng xe theo trạng thái
    long countByStatus(Vehicle.VehicleStatus status);

    // STT 13: Vehicle Management - Kiểm tra trùng biển số khi thêm mới
    boolean existsByLicensePlate(String licensePlate);
}