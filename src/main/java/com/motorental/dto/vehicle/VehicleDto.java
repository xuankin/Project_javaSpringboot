package com.motorental.dto.vehicle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Tên xe không được để trống")
    private String name;

    @NotBlank(message = "Biển số xe không được để trống")
    private String licensePlate;

    private String description;

    @NotNull(message = "Giá thuê không được để trống")
    @Min(value = 0, message = "Giá thuê phải lớn hơn 0")
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