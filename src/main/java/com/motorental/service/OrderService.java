package com.motorental.service;

import com.motorental.dto.order.CreateOrderDto;
import com.motorental.dto.order.OrderDto;
import com.motorental.dto.order.OrderDetailDto;
import com.motorental.dto.payment.PaymentDto;
import com.motorental.entity.*;
import com.motorental.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final RentalOrderRepository orderRepository;
    private final VehicleAvailabilityRepository availabilityRepository;
    private final CartService cartService;
    private final EmailService emailService;
    private final ModelMapper modelMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderDto createOrderFromCart(String userId, CreateOrderDto createOrderDto) {
        RentalCart cart = cartService.getCartEntity(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        List<VehicleAvailability.AvailabilityStatus> busyStatuses = List.of(
                VehicleAvailability.AvailabilityStatus.BOOKED,
                VehicleAvailability.AvailabilityStatus.COMPLETED
        );

        // 1. Kiểm tra tính khả dụng
        for (RentalCartItem item : cart.getItems()) {
            List<VehicleAvailability> conflicts = availabilityRepository.findConflictingAvailabilities(
                    item.getVehicle().getId(),
                    item.getStartDate(),
                    item.getEndDate(),
                    busyStatuses
            );

            if (!conflicts.isEmpty()) {
                throw new RuntimeException("Xe '" + item.getVehicle().getName() +
                        "' đã bị người khác đặt hoặc đang trong thời gian thuê.");
            }
        }

        // 2. Tạo đối tượng đơn hàng
        RentalOrder order = new RentalOrder();
        order.setUser(cart.getUser());
        order.setPickupLocation(createOrderDto.getPickupLocation());
        order.setNotes(createOrderDto.getNotes());
        order.setStatus(RentalOrder.OrderStatus.PENDING);

        BigDecimal totalPrice = BigDecimal.ZERO;

        // 3. Xử lý chi tiết đơn hàng
        for (RentalCartItem item : cart.getItems()) {
            OrderDetail detail = new OrderDetail();
            detail.setRentalOrder(order);
            detail.setVehicle(item.getVehicle());
            detail.setStartDate(item.getStartDate());
            detail.setEndDate(item.getEndDate());
            detail.setPricePerDay(item.getPricePerDay());
            detail.setRentalDays(item.getRentalDays());
            detail.setTotalPrice(item.getTotalPrice());

            order.addOrderDetail(detail);
            totalPrice = totalPrice.add(item.getTotalPrice());

            VehicleAvailability availability = new VehicleAvailability();
            availability.setVehicle(item.getVehicle());
            availability.setOrder(order);
            availability.setStartDate(item.getStartDate());
            availability.setEndDate(item.getEndDate());
            availability.setStatus(VehicleAvailability.AvailabilityStatus.BOOKED);

            order.getAvailabilities().add(availability);
            item.getVehicle().incrementRentalCount();
        }

        order.setTotalPrice(totalPrice);
        RentalOrder savedOrder = orderRepository.save(order);

        // 4. Xóa giỏ hàng
        cartService.clearCart(userId);

        // 5. Gửi email (Sử dụng try-catch để không làm lỗi đơn hàng nếu mail lỗi)
        try {
            emailService.sendOrderConfirmationEmail(cart.getUser().getEmail(), savedOrder);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail xác nhận: " + e.getMessage());
        }

        return mapToDto(savedOrder);
    }

    public List<OrderDto> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long orderId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        return mapToDto(order);
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn này");
        }

        if (order.getStatus() == RentalOrder.OrderStatus.COMPLETED ||
                order.getStatus() == RentalOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái này");
        }

        order.setStatus(RentalOrder.OrderStatus.CANCELLED);
        orderRepository.save(order);
        availabilityRepository.deleteByOrderId(orderId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String statusStr) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        RentalOrder.OrderStatus newStatus = RentalOrder.OrderStatus.valueOf(statusStr);

        if (newStatus == RentalOrder.OrderStatus.CANCELLED) {
            availabilityRepository.deleteByOrderId(orderId);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        // QUAN TRỌNG: Try-catch việc gửi mail để tránh lỗi Authentication Failed làm rollback transaction
        try {
            emailService.sendOrderStatusUpdateEmail(order.getUser().getEmail(), order, order.getStatus().name(), statusStr);
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không thể gửi email cập nhật trạng thái. Lỗi: " + e.getMessage());
            // Không throw exception ở đây để transaction vẫn thành công
        }
    }

    private OrderDto mapToDto(RentalOrder order) {
        OrderDto dto = modelMapper.map(order, OrderDto.class);
        dto.setUserName(order.getUser().getFullName());
        dto.setUserEmail(order.getUser().getEmail());
        dto.setStatus(order.getStatus());
        dto.setPickupLocation(order.getPickupLocation());

        List<OrderDetailDto> details = order.getOrderDetails().stream().map(d -> {
            OrderDetailDto dDto = modelMapper.map(d, OrderDetailDto.class);
            dDto.setVehicleName(d.getVehicle().getName());
            dDto.setVehicleId(d.getVehicle().getId());
            return dDto;
        }).collect(Collectors.toList());
        dto.setOrderDetails(details);

        if (!details.isEmpty()) {
            OrderDetailDto firstItem = details.get(0);
            dto.setVehicleId(firstItem.getVehicleId());
            dto.setVehicleName(firstItem.getVehicleName());
            dto.setStartDate(firstItem.getStartDate());
            dto.setEndDate(firstItem.getEndDate());
        }

        if (order.getPayment() != null) {
            dto.setPayment(modelMapper.map(order.getPayment(), PaymentDto.class));
        }

        return dto;
    }
}