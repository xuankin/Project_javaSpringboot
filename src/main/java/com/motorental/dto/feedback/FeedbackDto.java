package com.motorental.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Long id;

    @NotNull(message = "Thiếu mã xe")
    private Long vehicleId;

    // Đã xóa orderId vì cho phép đánh giá tự do

    private String vehicleName;
    private String userName;

    @NotNull(message = "Vui lòng chọn số điểm đánh giá")
    @Min(value = 1, message = "Tối thiểu 1 sao")
    @Max(value = 5, message = "Tối đa 5 sao")
    private Integer rating;

    // Tên biến là 'comment' => Bên HTML phải đặt name="comment"
    private String comment;

    private LocalDateTime createdAt;
}