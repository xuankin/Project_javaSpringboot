package com.motorental.service;

import com.motorental.dto.dashboard.DashboardStatsDto;
import com.motorental.entity.RentalOrder;
import com.motorental.entity.Vehicle;
import com.motorental.repository.PaymentRepository;
import com.motorental.repository.RentalOrderRepository;
import com.motorental.repository.UserRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalOrderRepository orderRepository;
    // private final PaymentRepository paymentRepository; // Chưa dùng có thể bỏ qua

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        // 1. Tổng quan số lượng
        stats.setTotalUsers(userRepository.count());
        stats.setTotalVehicles(vehicleRepository.count());
        stats.setTotalOrders(orderRepository.count());

        // 2. Doanh thu
        Double totalRev = orderRepository.getTotalRevenue();
        stats.setTotalRevenue(totalRev != null ? totalRev : 0.0);

        LocalDate now = LocalDate.now();
        // Lưu ý: Nếu method getMonthlyRevenue chưa có trong Repo thì cần xử lý null hoặc try-catch
        try {
            Double monthRev = orderRepository.getMonthlyRevenue(now.getYear(), now.getMonthValue());
            stats.setMonthlyRevenue(monthRev != null ? monthRev : 0.0);
        } catch (Exception e) {
            stats.setMonthlyRevenue(0.0);
        }

        // 3. Trạng thái đơn hàng
        stats.setPendingOrders(orderRepository.countByStatus(RentalOrder.OrderStatus.PENDING));
        stats.setConfirmedOrders(orderRepository.countByStatus(RentalOrder.OrderStatus.CONFIRMED));
        stats.setCompletedOrders(orderRepository.countByStatus(RentalOrder.OrderStatus.COMPLETED));
        stats.setCancelledOrders(orderRepository.countByStatus(RentalOrder.OrderStatus.CANCELLED));

        // 4. Trạng thái xe
        stats.setAvailableVehicles(vehicleRepository.countByStatus(Vehicle.VehicleStatus.AVAILABLE));
        stats.setRentedVehicles(vehicleRepository.countByStatus(Vehicle.VehicleStatus.RENTED));

        // 5. Lấy 5 đơn hàng mới nhất (Sắp xếp theo ID giảm dần)
        // Spring Data JPA hỗ trợ findAll(Pageable) mặc định
        stats.setRecentOrders(orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by("id").descending())
        ).getContent());

        return stats;
    }
}