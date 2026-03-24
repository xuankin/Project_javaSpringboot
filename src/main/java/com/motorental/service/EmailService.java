package com.motorental.service;

import com.motorental.entity.Payment;
import com.motorental.entity.RentalOrder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.base-url:http://localhost:8088}")
    private String baseUrl;

    // SỬA 1: Đổi tham số từ Double sang BigDecimal để khớp với Entity
    // SỬA 2: Thay new Locale(...) bằng Locale.of(...) cho Java 21
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        Locale localeVN = Locale.of("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
        return currencyFormatter.format(amount);
    }

    @Async
    public void sendOrderConfirmationEmail(String toEmail, RentalOrder order) {
        String subject = "🔔 Xác nhận đơn thuê xe - Mã: " + order.getOrderCode();

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            </head>
            <body class="bg-light py-4" style="background-color: #f8f9fa; padding: 20px; font-family: Arial, sans-serif;">
                <div class="container" style="max-width: 600px; margin: 0 auto;">
                    <div class="card shadow-sm border-0 rounded-4" style="background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                        <div class="card-header bg-success text-white text-center py-4" style="background-color: #198754; padding: 20px;">
                            <h2 class="mb-0 fw-bold" style="color: white; margin: 0;">MotoRental</h2>
                            <p class="mb-0 opacity-75" style="color: #e9ecef; margin: 0;">Dịch vụ thuê xe máy uy tín</p>
                        </div>
                        <div class="card-body p-4 p-md-5" style="padding: 30px;">
                            <h4 class="card-title text-success mb-4 fw-bold" style="color: #198754;">Cảm ơn bạn đã đặt xe!</h4>
                            <p class="card-text text-muted mb-4" style="color: #6c757d;">Xin chào, đơn hàng của bạn đã được hệ thống ghi nhận thành công. Dưới đây là thông tin chi tiết đơn hàng:</p>
                            
                            <div class="border rounded-3 p-3 bg-light mb-4" style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 10px; padding: 15px;">
                                <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
                                    <span class="text-muted fw-semibold" style="color: #6c757d;">Mã đơn hàng:</span>
                                    <span class="fw-bold text-dark" style="color: #212529; font-weight: bold;">%s</span>
                                </div>
                                <hr class="my-2" style="border-top: 1px solid #dee2e6;">
                                <div style="display: flex; justify-content: space-between;">
                                    <span class="text-muted fw-semibold" style="color: #6c757d;">Tổng tiền tính toán:</span>
                                    <span class="fw-bold text-danger fs-5" style="color: #dc3545; font-weight: bold; font-size: 1.25rem;">%s</span>
                                </div>
                            </div>
                            
                            <div class="alert alert-warning text-center border-start border-warning border-4" style="background-color: #fff3cd; color: #856404; padding: 15px; border-left: 4px solid #ffc107; border-radius: 5px; text-align: center;">
                                Vui lòng thanh toán sớm để chúng tôi có thể giữ xe cho bạn.
                            </div>
                            
                            <div class="text-center mt-5" style="text-align: center; margin-top: 30px;">
                                <a href="%s/my-orders" class="btn btn-success btn-lg rounded-pill fw-bold" style="background-color: #198754; color: white; padding: 12px 30px; text-decoration: none; border-radius: 50px; display: inline-block;">Xem chi tiết đơn hàng</a>
                            </div>
                        </div>
                        <div class="card-footer bg-white text-center py-4 border-0" style="background-color: white; padding: 20px; text-align: center; border-top: 1px solid #eee;">
                            <p class="text-muted small mb-0" style="color: #6c757d; font-size: 12px; margin: 0;">&copy; 2026 MotoRental. Mọi quyền được bảo lưu.</p>
                            <p class="text-muted small mb-0" style="color: #6c757d; font-size: 12px; margin: 0;">Địa chỉ: TP. Hồ Chí Minh, Việt Nam</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                baseUrl,
                order.getOrderCode(),
                formatCurrency(order.getTotalPrice())
        );

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendOrderStatusUpdateEmail(String toEmail, RentalOrder order, String oldStatus, String newStatus) {
        String subject = "📢 Cập nhật trạng thái đơn hàng - " + order.getOrderCode();

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            </head>
            <body class="bg-light py-4" style="background-color: #f8f9fa; padding: 20px;">
                <div class="container" style="max-width: 600px; margin: 0 auto;">
                    <div class="card shadow-sm border-0 rounded-4" style="background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                        <div class="card-header bg-warning text-dark text-center py-4" style="background-color: #ffc107; padding: 20px;">
                            <h3 class="mb-0 fw-bold" style="color: #212529; margin: 0;">📢 Cập nhật đơn hàng</h3>
                        </div>
                        <div class="card-body p-4 p-md-5" style="padding: 30px;">
                            <p class="card-text text-muted fs-5" style="font-size: 18px; color: #6c757d;">Xin chào,</p>
                            <p class="card-text text-muted" style="color: #6c757d;">Trạng thái đơn hàng <strong class="text-dark">%s</strong> của bạn đã được thay đổi:</p>
                            
                            <div class="d-flex align-items-center justify-content-center bg-light rounded-3 p-4 my-4 border" style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 10px; padding: 20px; text-align: center;">
                                <span class="badge bg-secondary text-decoration-line-through fs-6 px-3 py-2" style="background-color: #6c757d; color: white; padding: 8px 15px; border-radius: 5px; text-decoration: line-through;">%s</span>
                                <span class="mx-4 text-muted fs-4" style="margin: 0 20px; font-size: 24px; color: #6c757d;">➔</span>
                                <span class="badge bg-primary fs-5 px-4 py-2 shadow-sm" style="background-color: #0d6efd; color: white; padding: 10px 20px; border-radius: 5px; font-weight: bold; font-size: 18px;">%s</span>
                            </div>
                            
                            <p class="text-center text-muted mt-4" style="text-align: center; color: #6c757d; margin-top: 20px;">Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của MotoRental.</p>
                        </div>
                        <div class="card-footer bg-white text-center py-3 border-0" style="background-color: white; padding: 15px; text-align: center; border-top: 1px solid #eee;">
                            <p class="text-muted small mb-0" style="color: #6c757d; font-size: 12px; margin: 0;">MotoRental System Auto-Notification</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                order.getOrderCode(),
                oldStatus,
                newStatus
        );

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendPaymentConfirmationEmail(String toEmail, Payment payment) {
        String subject = "✅ Xác nhận thanh toán thành công - " + payment.getTransactionId();

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            </head>
            <body class="bg-light py-4" style="background-color: #f8f9fa; padding: 20px;">
                <div class="container" style="max-width: 600px; margin: 0 auto;">
                    <div class="card shadow-sm border-0 rounded-4 overflow-hidden" style="background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                        <div class="bg-primary text-white text-center py-4" style="background-color: #0d6efd; padding: 30px; text-align: center;">
                            <div class="display-4 mb-2" style="font-size: 40px; margin-bottom: 10px;">✅</div>
                            <h3 class="mb-0 fw-bold" style="color: white; margin: 0;">Thanh toán thành công</h3>
                        </div>
                        <div class="card-body p-4 p-md-5" style="padding: 30px;">
                            <p class="card-text text-center text-muted mb-4 fs-5" style="color: #6c757d; text-align: center; margin-bottom: 20px;">Hệ thống đã nhận được khoản thanh toán cho đơn hàng <strong class="text-dark">%s</strong>.</p>
                            
                            <div class="card border-0 bg-light shadow-sm mb-4" style="background-color: #f8f9fa; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
                                <ul class="list-group list-group-flush border-0 bg-transparent" style="list-style: none; padding: 0; margin: 0;">
                                    <li class="list-group-item bg-transparent d-flex justify-content-between align-items-center px-0 border-bottom border-light" style="display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #dee2e6;">
                                        <span class="text-muted" style="color: #6c757d;">Mã giao dịch</span>
                                        <span class="fw-bold text-dark" style="font-weight: bold;">%s</span>
                                    </li>
                                    <li class="list-group-item bg-transparent d-flex justify-content-between align-items-center px-0 border-bottom border-light py-3" style="display: flex; justify-content: space-between; padding: 15px 0; border-bottom: 1px solid #dee2e6;">
                                        <span class="text-muted" style="color: #6c757d;">Số tiền thanh toán</span>
                                        <span class="fw-bold text-success fs-5" style="color: #198754; font-weight: bold; font-size: 1.2rem;">%s</span>
                                    </li>
                                    <li class="list-group-item bg-transparent d-flex justify-content-between align-items-center px-0 pt-3" style="display: flex; justify-content: space-between; padding: 10px 0 0 0;">
                                        <span class="text-muted" style="color: #6c757d;">Phương thức</span>
                                        <span class="badge bg-info text-dark rounded-pill px-3" style="background-color: #0dcaf0; color: #000; padding: 5px 15px; border-radius: 50px;">%s</span>
                                    </li>
                                </ul>
                            </div>
                            
                            <p class="text-center text-muted mb-0" style="text-align: center; color: #6c757d;">Rất hân hạnh được phục vụ quý khách!</p>
                        </div>
                        <div class="card-footer bg-white text-center py-4 border-0" style="background-color: white; padding: 20px; text-align: center; border-top: 1px solid #eee;">
                            <a href="%s/" class="btn btn-outline-primary rounded-pill px-4" style="display: inline-block; padding: 10px 20px; color: #0d6efd; border: 1px solid #0d6efd; border-radius: 50px; text-decoration: none;">Quay về trang chủ</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                baseUrl,
                payment.getRentalOrder().getOrderCode(),
                payment.getTransactionId(),
                formatCurrency(payment.getAmount()),
                payment.getMethod()
        );

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}