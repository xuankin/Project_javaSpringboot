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
        return getAllPayments().stream()
                .filter(p -> {
                    RentalOrder order = orderRepository.findById(p.getOrderId()).orElse(null);
                    return order != null && order.getUser().getId().equals(userId);
                })
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

        String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
        payment.setTransactionId(vnp_TxnRef);
        payment.setPaymentDate(LocalDateTime.now());
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
    public void processVNPayCallback(HttpServletRequest request) {
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

            Payment payment = paymentRepository.findAllByOrderByPaymentDateDesc().stream()
                    .filter(p -> p.getTransactionId() != null && p.getTransactionId().equals(txnRef))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

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
            } else {
                payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                payment.setNotes("VNPay Failed. Code: " + responseCode);
                paymentRepository.save(payment);
            }
        } else {
            // Log lỗi chữ ký nhưng không throw exception để tránh crash trang callback
            System.err.println("Invalid Checksum from VNPay Callback!");
        }
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