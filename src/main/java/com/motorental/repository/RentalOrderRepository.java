package com.motorental.repository;

import com.motorental.entity.RentalOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    // --- CÁC METHOD BỊ THIẾU GÂY LỖI ---

    // 1. Lấy danh sách đơn hàng của user, sắp xếp mới nhất trước
    List<RentalOrder> findByUserIdOrderByCreatedAtDesc(String userId);

    // 2. Lấy tất cả đơn hàng (có hỗ trợ phân trang/sắp xếp trong tên hàm)
    // Lưu ý: Spring Data JPA sẽ tự parse "OrderByCreatedAtDesc"
    Page<RentalOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ------------------------------------

    // Tìm kiếm đơn hàng (Fix lỗi cú pháp LIKE %:keyword%)
    @Query("SELECT o FROM RentalOrder o WHERE " +
            "lower(o.orderCode) LIKE lower(concat('%', :keyword, '%')) OR " +
            "lower(o.user.username) LIKE lower(concat('%', :keyword, '%')) OR " +
            "o.user.phoneNumber LIKE concat('%', :keyword, '%')")
    Page<RentalOrder> searchOrders(@Param("keyword") String keyword, Pageable pageable);

    // Các method phục vụ Dashboard
    long countByStatus(RentalOrder.OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM RentalOrder o WHERE o.status = 'COMPLETED'")
    Double getTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM RentalOrder o WHERE o.status = 'COMPLETED' " +
            "AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    Double getMonthlyRevenue(@Param("year") int year, @Param("month") int month);
}