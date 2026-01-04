package com.motorental.repository;

import com.motorental.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // [FIX] Thêm hàm này để sửa lỗi trong PaymentService
    List<Payment> findAllByOrderByPaymentDateDesc();

    Optional<Payment> findByRentalOrderId(Long rentalOrderId);

    @Query("SELECT p FROM Payment p WHERE p.rentalOrder.user.id = :userId ORDER BY p.paymentDate DESC") // [Optional] Sửa createdAt -> paymentDate nếu cần
    Page<Payment> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
            "lower(p.transactionId) LIKE lower(concat('%', :keyword, '%')) OR " +
            "lower(p.rentalOrder.orderCode) LIKE lower(concat('%', :keyword, '%'))")
    Page<Payment> searchPayments(@Param("keyword") String keyword, Pageable pageable);

    long countByMethod(Payment.PaymentMethod method);
}