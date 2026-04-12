package com.motorental.service;

import com.motorental.dto.payment.CreatePaymentDto;
import com.motorental.dto.payment.PaymentDto;
import com.motorental.entity.Payment;
import com.motorental.entity.RentalOrder;
import com.motorental.repository.PaymentRepository;
import com.motorental.repository.RentalOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalOrderRepository orderRepository;
    private final ModelMapper modelMapper;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.payUrl}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    // --- LOGIC THANH TOÁN TIỀN MẶT ---
    @Transactional
    public void createCashPayment(Long orderId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setRentalOrder(order);
        } else if (payment.getPaymentStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order already paid.");
        }

        payment.setAmount(order.getTotalPrice());
        payment.setMethod(Payment.PaymentMethod.CASH);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId("CASH" + System.currentTimeMillis());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setNotes("Pay at counter");

        paymentRepository.save(payment);
    }

    // --- CÁC HÀM GET DỮ LIỆU ---
    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PaymentDto> getPaymentsByUserId(String userId) {
        // [FIX] Sử dụng Pageable.unpaged() hoặc tạo method mới trả về List trong Repo để optimize
        return paymentRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private PaymentDto convertToDto(Payment payment) {
        PaymentDto dto = modelMapper.map(payment, PaymentDto.class);
        if (payment.getRentalOrder() != null) {
            dto.setOrderId(payment.getRentalOrder().getId());
            dto.setOrderCode(payment.getRentalOrder().getOrderCode());
            if (payment.getRentalOrder().getUser() != null) {
                dto.setUserName(payment.getRentalOrder().getUser().getFullName());
            }
        }
        return dto;
    }

    @Transactional
    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        RentalOrder order = payment.getRentalOrder();
        if (order != null) {
            order.setStatus(RentalOrder.OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
    }

    // ==================================================================================
    // VNPay Integration Methods (FIXED & ROBUST)
    // ==================================================================================

    @Transactional
    public String createVNPayPaymentUrl(CreatePaymentDto dto, HttpServletRequest request) {
        // 1. Kiểm tra và làm sạch cấu hình (TRIM dấu cách)
        String tmnCode = (vnpTmnCode != null) ? vnpTmnCode.trim() : "";
        String hashSecret = (vnpHashSecret != null) ? vnpHashSecret.trim() : "";

        if (tmnCode.isEmpty() || hashSecret.isEmpty()) {
            throw new RuntimeException("Vui lòng cấu hình vnpay.tmnCode và vnpay.hashSecret trong application.properties");
        }

        RentalOrder order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setRentalOrder(order);
        }

        // 2. Tính tiền (Nhân 100 theo yêu cầu VNPay)
        BigDecimal total = order.getTotalPrice();
        if (total == null) total = BigDecimal.ZERO;
        long amount = total.multiply(BigDecimal.valueOf(100)).longValue();

        payment.setAmount(total);
        payment.setMethod(Payment.PaymentMethod.VNPAY);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        payment = paymentRepository.save(payment); // Lưu trước để có ID

        // Sử dụng ID để đảm bảo tính duy nhất
        String vnp_TxnRef = "PAY" + payment.getId() + "T" + System.currentTimeMillis();
        payment.setTransactionId(vnp_TxnRef);
        payment.setNotes("VNPay Pending: " + vnp_TxnRef);
        paymentRepository.save(payment);

        // 3. Tạo Parameters
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (dto.getBankCode() != null && !dto.getBankCode().isEmpty()) {
            vnp_Params.put("vnp_BankCode", dto.getBankCode());
        }

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        // FIX: Nội dung tiếng Anh/Không dấu để tránh lỗi Encoding
        vnp_Params.put("vnp_OrderInfo", "Pay Order " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        // FIX: Hardcode IP để tránh lỗi IPv6
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // 4. Build Query URL & Hash Data
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // FIX QUAN TRỌNG: Dùng US_ASCII để đảm bảo Hash khớp với VNPay Sandbox
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String queryUrl = query.toString();
        // Tính toán chữ ký bảo mật
        String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String finalUrl = vnpPayUrl + "?" + queryUrl;
        System.out.println("VNPay URL Generated: " + finalUrl); // In ra console để debug
        return finalUrl;
    }

    @Transactional
    public boolean processVNPayCallback(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) fields.remove("vnp_SecureHashType");
        if (fields.containsKey("vnp_SecureHash")) fields.remove("vnp_SecureHash");

        String signValue = hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            String txnRef = request.getParameter("vnp_TxnRef");
            String responseCode = request.getParameter("vnp_ResponseCode");

            // [FIX] Dùng findByTransactionId thay vì stream trên toàn bộ DB để tăng hiệu năng và tránh chậm hệ thống
            Payment payment = paymentRepository.findByTransactionId(txnRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found for Transaction ID: " + txnRef));

            if ("00".equals(responseCode)) {
                if (payment.getPaymentStatus() != Payment.PaymentStatus.COMPLETED) {
                    payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaymentDate(LocalDateTime.now());
                    payment.setNotes("VNPay Success. Ref: " + txnRef);
                    paymentRepository.save(payment);

                    RentalOrder order = payment.getRentalOrder();
                    if (order != null) {
                        order.setStatus(RentalOrder.OrderStatus.CONFIRMED);
                        orderRepository.save(order);
                    }
                }
                return true;
            } else {
                payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                payment.setNotes("VNPay Failed. Code: " + responseCode);
                paymentRepository.save(payment);
                return false;
            }
        } else {
            // Log lỗi chữ ký nhưng không throw exception để tránh crash trang callback
            log.warn("[VNPay Callback] Invalid checksum! Expected: {}, Got: {}", signValue, vnp_SecureHash);
            return false;
        }
    }

    // ==================================================================================
    // VNPay IPN - Instant Payment Notification (Server-to-Server)
    // ==================================================================================

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPay gửi server-to-server.
     * Trả về Map chứa RspCode và Message theo chuẩn VNPay.
     * VNPay yêu cầu response: {"RspCode":"00","Message":"Confirm Success"}
     */
    @Transactional
    public Map<String, String> processVNPayIPN(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            // 1. Thu thập tất cả params từ VNPay
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            // 2. Xác minh chữ ký bảo mật
            String signValue = hashAllFields(fields);
            if (!signValue.equals(vnp_SecureHash)) {
                log.warn("[VNPay IPN] Invalid signature! txnRef={}", request.getParameter("vnp_TxnRef"));
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            // 3. Lấy thông tin giao dịch
            String txnRef      = request.getParameter("vnp_TxnRef");
            String responseCode = request.getParameter("vnp_ResponseCode");
            String vnpAmountStr = request.getParameter("vnp_Amount");
            long vnpAmount = Long.parseLong(vnpAmountStr) / 100; // VNPay nhân 100

            log.info("[VNPay IPN] txnRef={}, responseCode={}, amount={}", txnRef, responseCode, vnpAmount);

            // 4. Tìm payment theo mã giao dịch (indexed query, không scan toàn bảng)
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(txnRef);

            if (paymentOpt.isEmpty()) {
                log.warn("[VNPay IPN] Order not found for txnRef={}", txnRef);
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }

            Payment payment = paymentOpt.get();

            // 5. Kiểm tra số tiền khớp không
            long orderAmount = payment.getAmount().longValue();
            if (orderAmount != vnpAmount) {
                log.warn("[VNPay IPN] Amount mismatch! DB={}, VNPay={}", orderAmount, vnpAmount);
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }

            // 6. Kiểm tra đơn đã xử lý chưa (tránh xử lý 2 lần)
            if (payment.getPaymentStatus() == Payment.PaymentStatus.COMPLETED) {
                log.info("[VNPay IPN] Order already confirmed, txnRef={}", txnRef);
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // 7. Cập nhật trạng thái
            if ("00".equals(responseCode)) {
                payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setNotes("VNPay IPN Success. Ref: " + txnRef);
                paymentRepository.save(payment);

                RentalOrder order = payment.getRentalOrder();
                if (order != null) {
                    order.setStatus(RentalOrder.OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                    log.info("[VNPay IPN] Order {} CONFIRMED via IPN", order.getOrderCode());
                }
            } else {
                payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                payment.setNotes("VNPay IPN Failed. Code: " + responseCode);
                paymentRepository.save(payment);
                log.info("[VNPay IPN] Payment FAILED, code={}, txnRef={}", responseCode, txnRef);
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;

        } catch (Exception e) {
            log.error("[VNPay IPN] Unexpected error: {}", e.getMessage(), e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return response;
        }
    }

    /**
     * Lấy trạng thái payment theo orderId.
     * Dùng cho AJAX polling hoặc trang kết quả.
     */
    public Map<String, Object> getPaymentStatus(Long orderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            RentalOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Payment payment = order.getPayment();
            if (payment == null) {
                result.put("status", "NO_PAYMENT");
                result.put("message", "Chưa có thông tin thanh toán");
            } else {
                result.put("status", payment.getPaymentStatus().name());
                result.put("method", payment.getMethod() != null ? payment.getMethod().name() : "N/A");
                result.put("amount", payment.getAmount());
                result.put("transactionId", payment.getTransactionId());
                result.put("paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null);
                result.put("orderCode", order.getOrderCode());
                result.put("orderStatus", order.getStatus().name());
            }
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        return result;
    }

    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // Dùng US_ASCII đồng nhất
                    sb.append(fieldName);
                    sb.append('=');
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (itr.hasNext()) {
                sb.append('&');
            }
        }
        // Trim hash secret khi verify
        return hmacSHA512(vnpHashSecret.trim(), sb.toString());
    }

    private String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");

            // QUAN TRỌNG: Dùng UTF-8 cho Key (Java String -> Bytes)
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);

            // QUAN TRỌNG: Dùng UTF-8 cho Data
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
}