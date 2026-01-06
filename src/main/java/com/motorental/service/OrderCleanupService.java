package com.motorental.service;

import com.motorental.entity.RentalOrder;
import com.motorental.repository.RentalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCleanupService.class);

    private final RentalOrderRepository orderRepository;
    private final OrderService orderService;

    // Chạy định kỳ mỗi 1 giờ (3600000 ms).
    // initialDelay = 5000: Đợi 5 giây sau khi server khởi động mới chạy lần đầu.
    @Scheduled(fixedRate = 3600000, initialDelay = 5000)
    public void scanAndCancelOverdueOrders() {
        logger.info("--- Bắt đầu quét các đơn hàng treo (Pending) quá hạn ---");

        // Logic: Nếu (Ngày bắt đầu thuê) < (Hiện tại - 24 giờ) => Quá hạn nhận xe 1 ngày
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        List<RentalOrder> overdueOrders = orderRepository.findOverduePendingOrders(cutoffTime);

        if (overdueOrders.isEmpty()) {
            logger.info("Không tìm thấy đơn hàng nào cần hủy.");
            return;
        }

        int count = 0;
        for (RentalOrder order : overdueOrders) {
            try {
                logger.info("Đang tự động hủy đơn hàng: {} (Khách: {})", order.getOrderCode(), order.getUser().getUsername());

                // Cập nhật ghi chú lý do hủy
                String autoCancelNote = " [Hệ thống hủy tự động: Quá 24h kể từ ngày hẹn nhận xe]";
                if (order.getNotes() == null) {
                    order.setNotes(autoCancelNote);
                } else {
                    order.setNotes(order.getNotes() + autoCancelNote);
                }
                orderRepository.save(order);

                // Gọi hàm hủy đơn. Truyền null vào userId để bỏ qua check quyền sở hữu
                orderService.cancelOrder(order.getId(), null);

                count++;
            } catch (Exception e) {
                // Catch lỗi để 1 đơn lỗi không làm dừng cả vòng lặp
                logger.error("Lỗi khi hủy đơn {}: {}", order.getOrderCode(), e.getMessage());
            }
        }

        logger.info("--- Hoàn tất. Đã hủy tự động {} đơn hàng. ---", count);
    }
}