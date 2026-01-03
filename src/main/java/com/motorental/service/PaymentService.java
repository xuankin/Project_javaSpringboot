package com.motorental.service;

import com.motorental.dto.payment.CreatePaymentDto;
import com.motorental.dto.payment.PaymentDto;
import com.motorental.entity.Payment;
import com.motorental.entity.RentalOrder;
import com.motorental.repository.PaymentRepository;
import com.motorental.repository.RentalOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalOrderRepository orderRepository;
    private final EmailService emailService;
    private final ModelMapper modelMapper;

    // Lấy thanh toán theo User
    public List<PaymentDto> getPaymentsByUserId(String userId) {
        return paymentRepository.findByUserId(userId, Pageable.unpaged()).stream()
                .map(p -> {
                    PaymentDto dto = modelMapper.map(p, PaymentDto.class);
                    if (p.getRentalOrder() != null) {
                        dto.setOrderCode(p.getRentalOrder().getOrderCode());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Lấy thanh toán theo Order ID
    public PaymentDto getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByRentalOrderId(orderId)
                .map(p -> modelMapper.map(p, PaymentDto.class))
                .orElse(null);
    }

    // --- HÀM BỊ THIẾU ĐÃ ĐƯỢC THÊM LẠI ---
    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> {
                    PaymentDto dto = modelMapper.map(p, PaymentDto.class);
                    if (p.getRentalOrder() != null) {
                        dto.setOrderCode(p.getRentalOrder().getOrderCode());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
    // --------------------------------------

    @Transactional
    public void processCashPayment(Long orderId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        createOrUpdatePayment(order, Payment.PaymentMethod.CASH, "CASH_" + System.currentTimeMillis());
    }

    // --- Logic VNPay (Giả lập để test luồng) ---
    public String createVNPayPaymentUrl(CreatePaymentDto dto, HttpServletRequest request) {
        // Trả về URL Callback nội bộ để test luồng
        return "/payments/vnpay/callback?vnp_TxnRef=" + dto.getOrderId() + "&vnp_ResponseCode=00";
    }

    @Transactional
    public void processVNPayCallback(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            Long orderId = Long.parseLong(txnRef);
            RentalOrder order = orderRepository.findById(orderId).orElseThrow();
            createOrUpdatePayment(order, Payment.PaymentMethod.VNPAY, "VNP_" + System.currentTimeMillis());
        } else {
            throw new RuntimeException("Thanh toán thất bại. Mã lỗi: " + responseCode);
        }
    }

    // Hàm chung để lưu/cập nhật thanh toán
    private void createOrUpdatePayment(RentalOrder order, Payment.PaymentMethod method, String txnId) {
        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            p.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            p.setMethod(method);
            p.setTransactionId(txnId);
            p.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(p);
        } else {
            Payment p = new Payment();
            p.setRentalOrder(order);
            p.setAmount(order.getTotalPrice());
            p.setMethod(method);
            p.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            p.setTransactionId(txnId);
            p.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(p);
        }

        order.setStatus(RentalOrder.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        try {
            emailService.sendPaymentConfirmationEmail(order.getUser().getEmail(), order.getPayment());
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
        }
    }
}