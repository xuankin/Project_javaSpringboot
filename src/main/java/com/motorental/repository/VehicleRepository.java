package com.motorental.repository;

import com.motorental.entity.Vehicle;
import jakarta.persistence.LockModeType; // [MỚI] Import cho Lock
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // [MỚI] Import cho Lock
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // --- [MỚI] HÀM KHÓA XE ĐỂ TRÁNH TRÙNG LỊCH (RACE CONDITION) ---
    // Khi gọi hàm này, Database sẽ khóa dòng dữ liệu của xe lại cho đến khi Transaction kết thúc.
    // Các request khác muốn access vào xe này sẽ phải chờ (Wait).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdWithLock(@Param("id") Long id);
    // -------------------------------------------------------------

    // 1. Lấy danh sách các hãng xe (Brand) duy nhất để hiển thị bộ lọc
    @Query("SELECT DISTINCT v.brand FROM Vehicle v WHERE v.brand IS NOT NULL AND v.brand <> ''")
    List<String> findAllBrands();

    // 2. Tìm kiếm chi tiết
    @Query("SELECT v FROM Vehicle v WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR lower(v.name) LIKE lower(concat('%', :keyword, '%')) OR lower(v.brand) LIKE lower(concat('%', :keyword, '%'))) " +
            "AND (:brand IS NULL OR :brand = '' OR v.brand = :brand) " +
            "AND (:maxPrice IS NULL OR v.pricePerDay <= :maxPrice) " +
            "AND v.status = 'AVAILABLE'")
    Page<Vehicle> searchVehiclesDetail(@Param("keyword") String keyword,
                                       @Param("brand") String brand,
                                       @Param("maxPrice") Double maxPrice,
                                       Pageable pageable);

    // --- CÁC HÀM CŨ ---

    @Query("SELECT v FROM Vehicle v WHERE " +
            "(:keyword IS NULL OR lower(v.name) LIKE lower(concat('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR v.status = :status)")
    Page<Vehicle> searchVehicles(@Param("keyword") String keyword,
                                 @Param("status") Vehicle.VehicleStatus status,
                                 Pageable pageable);

    @Query("SELECT v FROM Vehicle v ORDER BY v.rentalCount DESC")
    List<Vehicle> findTopPopularVehicles(Pageable pageable);

    long countByStatus(Vehicle.VehicleStatus status);

    boolean existsByLicensePlate(String licensePlate);
    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);
}