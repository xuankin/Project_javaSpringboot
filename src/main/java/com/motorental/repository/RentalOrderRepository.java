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

    // STT 9: My Orders - Lấy đơn hàng của user, mới nhất lên đầu
    List<RentalOrder> findByUserIdOrderByCreatedAtDesc(String userId);

    // STT 14: Rental Order Management - Lấy tất cả đơn cho Admin
    Page<RentalOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // STT 14: Tìm kiếm đơn hàng (Mã đơn, User, SĐT)
    @Query("SELECT o FROM RentalOrder o WHERE " +
            "lower(o.orderCode) LIKE lower(concat('%', :keyword, '%')) OR " +
            "lower(o.user.username) LIKE lower(concat('%', :keyword, '%')) OR " +
            "o.user.phoneNumber LIKE %:keyword%")
    Page<RentalOrder> searchOrders(@Param("keyword") String keyword, Pageable pageable);

    // STT 12: Admin Dashboard - Thống kê số đơn theo trạng thái (Pending, Completed...)
    long countByStatus(RentalOrder.OrderStatus status);

    // STT 12: Admin Dashboard - Tổng doanh thu (Chỉ tính đơn Completed)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM RentalOrder o WHERE o.status = 'COMPLETED'")
    Double getTotalRevenue();

    // STT 12: Admin Dashboard - Doanh thu theo tháng
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM RentalOrder o WHERE o.status = 'COMPLETED' " +
            "AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    Double getMonthlyRevenue(@Param("year") int year, @Param("month") int month);
}