package com.motorental.service;

import com.motorental.dto.order.CreateOrderDto;
import com.motorental.dto.order.OrderDto;
import com.motorental.dto.order.OrderDetailDto;
import com.motorental.dto.payment.PaymentDto;
import com.motorental.entity.*;
import com.motorental.model.DiscountCode; // Import the DiscountCode class
import com.motorental.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final RentalOrderRepository orderRepository;
    private final VehicleAvailabilityRepository availabilityRepository;
    private final VehicleRepository vehicleRepository;
    private final CartService cartService;
    private final EmailService emailService;
    private final DiscountCodeService discountCodeService; // Injected
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(rollbackFor = Exception.class)
    public OrderDto createOrderFromCart(String userId, CreateOrderDto createOrderDto) {
        RentalCart cart = cartService.getCartEntity(userId);

        if (Boolean.TRUE.equals(cart.getUser().getIsScammed())) {
            throw new RuntimeException("Tài khoản của bạn đang bị giới hạn do vi phạm chính sách (Scam). Vui lòng liên hệ quản trị viên.");
        }

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        // Chỉ kiểm tra BOOKED (xe đang được đặt/chưa trả)
        // COMPLETED = đã trả xe → cho phép thuê thời gian khác
        List<VehicleAvailability.AvailabilityStatus> busyStatuses = List.of(
                VehicleAvailability.AvailabilityStatus.BOOKED
        );

        for (RentalCartItem item : cart.getItems()) {
            vehicleRepository.findByIdWithLock(item.getVehicle().getId())
                    .orElseThrow(() -> new RuntimeException("Xe không tồn tại hoặc đang bận xử lý!"));

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

        RentalOrder order = new RentalOrder();
        order.setUser(cart.getUser());
        order.setPickupLocation(createOrderDto.getPickupLocation());
        order.setNotes(createOrderDto.getNotes());
        order.setStatus(RentalOrder.OrderStatus.PENDING);

        BigDecimal originalTotalPrice = cart.getItems().stream()
                .map(RentalCartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotalPrice = originalTotalPrice;

        // --- Discount Code Logic ---
        if (StringUtils.hasText(createOrderDto.getDiscountCode())) {
            String code = createOrderDto.getDiscountCode();
            Optional<DiscountCode> discountOpt = discountCodeService.findByCode(code);

            if (discountOpt.isPresent() && discountCodeService.isCodeValid(code)) {
                DiscountCode discount = discountOpt.get();
                // TÍNH TOÁN THEO %: (Tổng tiền * % giảm giá) / 100
                BigDecimal percentage = BigDecimal.valueOf(discount.getDiscountPercentage());
                BigDecimal discountAmount = originalTotalPrice.multiply(percentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Giới hạn số tiền giảm không vượt quá tổng đơn hàng
                if (discountAmount.compareTo(originalTotalPrice) > 0) {
                    discountAmount = originalTotalPrice;
                }
                finalTotalPrice = originalTotalPrice.subtract(discountAmount);

                order.setDiscountCode(discount.getCode());
                order.setDiscountAmount(discountAmount);
            } else {
                throw new RuntimeException("Mã giảm giá không hợp lệ hoặc đã hết hạn.");
            }
        }
        // -------------------------

        order.setTotalPrice(finalTotalPrice);

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

            VehicleAvailability availability = new VehicleAvailability();
            availability.setVehicle(item.getVehicle());
            availability.setOrder(order);
            availability.setStartDate(item.getStartDate());
            availability.setEndDate(item.getEndDate());
            availability.setStatus(VehicleAvailability.AvailabilityStatus.BOOKED);

            order.getAvailabilities().add(availability);
            item.getVehicle().incrementRentalCount();
        }

        RentalOrder savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        try {
            emailService.sendOrderConfirmationEmail(cart.getUser().getEmail(), savedOrder);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail xác nhận: " + e.getMessage());
        }

        return mapToDto(savedOrder);
    }

    // --- CÁC HÀM DƯỚI GIỮ NGUYÊN KHÔNG ĐỔI ---

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

    public org.springframework.data.domain.Page<OrderDto> getOrdersPageable(String userId, String status, Pageable pageable) {
        org.springframework.data.domain.Page<RentalOrder> orders;
        if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
            RentalOrder.OrderStatus orderStatus = RentalOrder.OrderStatus.valueOf(status.toUpperCase());
            orders = orderRepository.findByUserIdAndStatus(userId, orderStatus, pageable);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return orders.map(this::mapToDto);
    }

    public org.springframework.data.domain.Page<OrderDto> getAllOrdersPageable(String status, Pageable pageable) {
        org.springframework.data.domain.Page<RentalOrder> orders;
        if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
            RentalOrder.OrderStatus orderStatus = RentalOrder.OrderStatus.valueOf(status.toUpperCase());
            orders = orderRepository.findAllByStatus(orderStatus, pageable);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return orders.map(this::mapToDto);
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
        
        // [FIX] Giải phóng trạng thái xe khi khách hủy đơn
        order.getOrderDetails().forEach(detail -> {
            Vehicle v = detail.getVehicle();
            v.setStatus(Vehicle.VehicleStatus.AVAILABLE);
            vehicleRepository.save(v);
        });
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String statusStr) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        RentalOrder.OrderStatus newStatus = RentalOrder.OrderStatus.valueOf(statusStr);

        if (newStatus == RentalOrder.OrderStatus.CANCELLED) {
            availabilityRepository.deleteByOrderId(orderId);
        } else if (newStatus == RentalOrder.OrderStatus.CONFIRMED) {
            // Khi xác nhận (đã thanh toán hoặc duyệt): Chuyển xe sang RENTED để bắt đầu giả lập GPS
            order.getOrderDetails().forEach(detail -> {
                Vehicle v = detail.getVehicle();
                v.setStatus(Vehicle.VehicleStatus.RENTED);
                vehicleRepository.save(v);
            });
        } else if (newStatus == RentalOrder.OrderStatus.COMPLETED) {
            // Cập nhật trạng thái availability thành COMPLETED
            // để không block ngày mới khi xe đã được trả
            availabilityRepository.updateStatusByOrderId(orderId, VehicleAvailability.AvailabilityStatus.COMPLETED);
            
            // Giải phóng trạng thái xe
            order.getOrderDetails().forEach(detail -> {
                Vehicle v = detail.getVehicle();
                v.setStatus(Vehicle.VehicleStatus.AVAILABLE);
                vehicleRepository.save(v);
            });
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        try {
            emailService.sendOrderStatusUpdateEmail(order.getUser().getEmail(), order, order.getStatus().name(), statusStr);
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không thể gửi email cập nhật trạng thái. Lỗi: " + e.getMessage());
        }

        // WebSocket Push Notification
        try {
            String title = "Cập nhật đơn hàng";
            String contentMessage = "Đơn hàng " + order.getOrderCode() + " đã chuyển sang trạng thái " + newStatus.getDisplayName();
            messagingTemplate.convertAndSendToUser(
                    order.getUser().getUsername(),
                    "/queue/notifications",
                    java.util.Map.of(
                            "type", "NEW_MESSAGE",
                            "title", title,
                            "content", contentMessage
                    )
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi websocket notification: " + e.getMessage());
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
            dDto.setLatitude(d.getVehicle().getLatitude());
            dDto.setLongitude(d.getVehicle().getLongitude());
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