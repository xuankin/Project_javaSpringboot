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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    // SỬA 1: Đổi tham số từ Double sang BigDecimal để khớp với Entity
    // SỬA 2: Thay new Locale(...) bằng Locale.of(...) cho Java 21
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
        return currencyFormatter.format(amount);
    }

    @Async
    public void sendOrderConfirmationEmail(String toEmail, RentalOrder order) {
        String subject = "🔔 Xác nhận đơn thuê xe - Mã: " + order.getOrderCode();

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #f9f9f9;">
                <div style="text-align: center; padding-bottom: 20px; border-bottom: 2px solid #4CAF50;">
                    <h2 style="color: #4CAF50; margin: 0;">MotoRental</h2>
                    <p style="color: #666; font-size: 14px;">Dịch vụ thuê xe máy uy tín</p>
                </div>
                
                <div style="padding: 20px 0;">
                    <h3 style="color: #333;">Cảm ơn bạn đã đặt xe!</h3>
                    <p>Xin chào, đơn hàng của bạn đã được ghi nhận. Dưới đây là thông tin chi tiết:</p>
                    
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 15px; background-color: #fff;">
                        <tr>
                            <td style="padding: 10px; border-bottom: 1px solid #ddd; font-weight: bold;">Mã đơn hàng:</td>
                            <td style="padding: 10px; border-bottom: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border-bottom: 1px solid #ddd; font-weight: bold;">Tổng tiền:</td>
                            <td style="padding: 10px; border-bottom: 1px solid #ddd; color: #d9534f; font-weight: bold;">%s</td>
                        </tr>
                    </table>
                    
                    <p style="margin-top: 20px; font-style: italic; color: #555;">
                        Vui lòng thanh toán sớm để chúng tôi có thể giữ xe cho bạn.
                    </p>
                    
                    <div style="text-align: center; margin-top: 25px;">
                        <a href="http://localhost:8080/my-orders" style="background-color: #4CAF50; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">Xem chi tiết đơn hàng</a>
                    </div>
                </div>
                
                <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #888; font-size: 12px;">
                    <p>&copy; 2025 MotoRental. Mọi quyền được bảo lưu.</p>
                    <p>Địa chỉ: TP. Hồ Chí Minh, Việt Nam</p>
                </div>
            </div>
            """,
                order.getOrderCode(),
                formatCurrency(order.getTotalPrice())
        );

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendOrderStatusUpdateEmail(String toEmail, RentalOrder order, String oldStatus, String newStatus) {
        String subject = "📢 Cập nhật trạng thái đơn hàng - " + order.getOrderCode();

        String statusColor = "#2196F3";

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #ffffff;">
                <div style="text-align: center; border-bottom: 2px solid #FF9800; padding-bottom: 15px;">
                    <h2 style="color: #FF9800; margin: 0;">Cập nhật đơn hàng</h2>
                </div>
                
                <div style="padding: 20px 0;">
                    <p>Xin chào,</p>
                    <p>Trạng thái đơn hàng <strong>%s</strong> của bạn đã thay đổi:</p>
                    
                    <div style="background-color: #f1f1f1; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center;">
                        <span style="color: #777; text-decoration: line-through;">%s</span>
                        <span style="margin: 0 10px;">➔</span>
                        <span style="color: %s; font-weight: bold; font-size: 18px;">%s</span>
                    </div>
                    
                    <p>Cảm ơn bạn đã sử dụng dịch vụ của MotoRental.</p>
                </div>
                
                <div style="text-align: center; margin-top: 20px; color: #999; font-size: 12px;">
                    <p>MotoRental System</p>
                </div>
            </div>
            """,
                order.getOrderCode(),
                oldStatus,
                statusColor,
                newStatus
        );

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendPaymentConfirmationEmail(String toEmail, Payment payment) {
        String subject = "✅ Xác nhận thanh toán thành công - " + payment.getTransactionId();

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #fdfdfd;">
                <div style="text-align: center; background-color: #4CAF50; padding: 20px; border-radius: 10px 10px 0 0;">
                    <h2 style="color: white; margin: 0;">Thanh toán thành công</h2>
                </div>
                
                <div style="padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 10px 10px;">
                    <p>Hệ thống đã nhận được thanh toán cho đơn hàng <strong>%s</strong>.</p>
                    
                    <table style="width: 100%%; margin-top: 15px;">
                        <tr style="background-color: #f9f9f9;">
                            <td style="padding: 10px; color: #555;">Mã giao dịch:</td>
                            <td style="padding: 10px; font-weight: bold; text-align: right;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; color: #555;">Số tiền:</td>
                            <td style="padding: 10px; font-weight: bold; color: #4CAF50; text-align: right;">%s</td>
                        </tr>
                        <tr style="background-color: #f9f9f9;">
                            <td style="padding: 10px; color: #555;">Phương thức:</td>
                            <td style="padding: 10px; font-weight: bold; text-align: right;">%s</td>
                        </tr>
                    </table>
                    
                    <p style="margin-top: 20px; text-align: center;">Cảm ơn quý khách!</p>
                </div>
            </div>
            """,
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