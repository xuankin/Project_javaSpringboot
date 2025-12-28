package com.motorental.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {
    private Long id;
    private String name;
    private String licensePlate;
    private String description;
    private BigDecimal pricePerDay;
    private String status;          // Trả về chuỗi enum
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private String primaryImageUrl; // URL ảnh đại diện
    private Double averageRating;   // Điểm đánh giá trung bình
    private Integer rentalCount;    // Số lần đã thuê
}