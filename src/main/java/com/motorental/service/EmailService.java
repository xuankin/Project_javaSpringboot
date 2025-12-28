package com.motorental.service;

import com.motorental.entity.Payment;
import com.motorental.entity.RentalOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Async
    public void sendOrderConfirmationEmail(String toEmail, RentalOrder order) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác nhận đơn thuê xe - Mã: " + order.getOrderCode());
        message.setText("Cảm ơn bạn đã đặt xe tại MotoRental.\n" +
                "Mã đơn hàng: " + order.getOrderCode() + "\n" +
                "Tổng tiền: " + order.getTotalPrice() + " VNĐ\n" +
                "Vui lòng thanh toán để xác nhận đơn hàng.");

        javaMailSender.send(message);
    }

    @Async
    public void sendOrderStatusUpdateEmail(String toEmail, RentalOrder order, String oldStatus, String newStatus) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Cập nhật trạng thái đơn hàng - " + order.getOrderCode());
        message.setText("Đơn hàng " + order.getOrderCode() + " đã chuyển từ trạng thái " + oldStatus + " sang " + newStatus);

        javaMailSender.send(message);
    }

    @Async
    public void sendPaymentConfirmationEmail(String toEmail, Payment payment) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác nhận thanh toán thành công - " + payment.getTransactionId());
        message.setText("Hệ thống đã nhận được thanh toán cho đơn hàng " + payment.getRentalOrder().getOrderCode() + ".\n" +
                "Số tiền: " + payment.getAmount() + "\n" +
                "Phương thức: " + payment.getMethod());

        javaMailSender.send(message);
    }
}