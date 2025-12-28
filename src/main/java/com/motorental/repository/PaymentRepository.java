package com.motorental.repository;

import com.motorental.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // STT 10: Xem chi tiết thanh toán theo đơn hàng
    Optional<Payment> findByRentalOrderId(Long rentalOrderId);

    // STT 10: Payment Management (User) - Lịch sử thanh toán của user
    @Query("SELECT p FROM Payment p WHERE p.rentalOrder.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(@Param("userId") String userId, Pageable pageable);

    // STT 15: Payment Management (Admin) - Tìm kiếm thanh toán
    @Query("SELECT p FROM Payment p WHERE " +
            "lower(p.transactionId) LIKE lower(concat('%', :keyword, '%')) OR " +
            "lower(p.rentalOrder.orderCode) LIKE lower(concat('%', :keyword, '%'))")
    Page<Payment> searchPayments(@Param("keyword") String keyword, Pageable pageable);

    // STT 12: Admin Dashboard - Thống kê phương thức thanh toán
    long countByMethod(Payment.PaymentMethod method);
}