package com.motorental.repository;

import com.motorental.entity.RentalOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    // 1. Lấy danh sách đơn hàng của user, sắp xếp mới nhất trước
    List<RentalOrder> findByUserIdOrderByCreatedAtDesc(String userId);

    // 2. Lấy tất cả đơn hàng (có hỗ trợ phân trang/sắp xếp)
    Page<RentalOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 3. Tìm kiếm đơn hàng
    @Query("SELECT o FROM RentalOrder o WHERE " +
            "lower(o.orderCode) LIKE lower(concat('%', :keyword, '%')) OR " +
            "lower(o.user.username) LIKE lower(concat('%', :keyword, '%')) OR " +
            "o.user.phoneNumber LIKE concat('%', :keyword, '%')")
    Page<RentalOrder> searchOrders(@Param("keyword") String keyword, Pageable pageable);

    // --- CÁC QUERY DASHBOARD ---
    long countByStatus(RentalOrder.OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM RentalOrder o WHERE o.status = 'COMPLETED'")
    Double getTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM RentalOrder o WHERE o.status = 'COMPLETED' " +
            "AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    Double getMonthlyRevenue(@Param("year") int year, @Param("month") int month);

    // --- TÌM ĐƠN HÀNG TREO QUÁ HẠN ---
    @Query("SELECT DISTINCT o FROM RentalOrder o " +
            "JOIN o.orderDetails d " +
            "LEFT JOIN o.payment p " +
            "WHERE o.status = 'PENDING' " +
            "AND (p.method = 'CASH' OR p IS NULL) " +
            "AND d.startDate < :cutoffTime")
    List<RentalOrder> findOverduePendingOrders(@Param("cutoffTime") LocalDateTime cutoffTime);

    // --- [MỚI] KIỂM TRA NGƯỜI DÙNG ĐÃ THUÊ XE VÀ HOÀN THÀNH CHƯA ---
    // Logic: User đó + Xe đó + Đơn hàng trạng thái COMPLETED -> Trả về true/false
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM RentalOrder o JOIN o.orderDetails d " +
            "WHERE o.user.id = :userId " +
            "AND d.vehicle.id = :vehicleId " +
            "AND o.status = 'COMPLETED'")
    boolean hasUserRentedVehicle(@Param("userId") String userId, @Param("vehicleId") Long vehicleId);
}