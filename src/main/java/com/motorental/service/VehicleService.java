package com.motorental.service;

import com.motorental.dto.vehicle.VehicleDetailDto;
import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.entity.Vehicle;
import com.motorental.entity.VehicleAvailability;
import com.motorental.entity.VehicleImage;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.VehicleAvailabilityRepository;
import com.motorental.repository.VehicleImageRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityRepository availabilityRepository;
    private final FeedbackRepository feedbackRepository;
    private final ModelMapper modelMapper;

    // Thư mục lưu ảnh upload (tương ứng với cấu hình ResourceHandler nếu có)
    private static final String UPLOAD_DIR = "uploads/";

    public Page<VehicleDetailDto> searchVehicles(String keyword, Double maxPrice, Pageable pageable) {
        return vehicleRepository.searchVehiclesDetail(keyword, maxPrice, pageable)
                .map(this::mapToDetailDto);
    }

    // --- Phương thức mới: Lấy danh sách xe phổ biến (gọi từ HomeController) ---
    public List<VehicleDto> getPopularVehicles() {
        // Lấy top 6 xe phổ biến nhất
        Pageable pageable = PageRequest.of(0, 6);
        return vehicleRepository.findTopPopularVehicles(pageable).stream()
                .map(vehicle -> {
                    VehicleDto dto = modelMapper.map(vehicle, VehicleDto.class);
                    // Set ảnh đại diện nếu có
                    if (!vehicle.getImages().isEmpty()) {
                        dto.setPrimaryImageUrl(vehicle.getImages().iterator().next().getImageUrl());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<VehicleAvailability> getFutureBookings(Long vehicleId) {
        return availabilityRepository.findFutureBookings(
                vehicleId,
                List.of(VehicleAvailability.AvailabilityStatus.BOOKED,
                        VehicleAvailability.AvailabilityStatus.COMPLETED)
        );
    }

    public VehicleDetailDto getVehicleDetail(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));
        return mapToDetailDto(vehicle);
    }

    // --- Phương thức mới: Tạo xe mới (gọi từ AdminVehicleController) ---
    @Transactional
    public void createVehicle(VehicleDto dto, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = modelMapper.map(dto, Vehicle.class);

        // Mặc định trạng thái là AVAILABLE nếu chưa set
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(Vehicle.VehicleStatus.AVAILABLE);
        }

        // Lưu xe trước để có ID (nếu cần dùng cho logic khác, dù cascade persist sẽ lo việc lưu image)
        vehicle = vehicleRepository.save(vehicle);

        // Xử lý upload ảnh
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String imageUrl = saveFile(file);
                    VehicleImage image = new VehicleImage();
                    image.setImageUrl(imageUrl);
                    image.setVehicle(vehicle);
                    // Ảnh đầu tiên set làm ảnh chính
                    if (vehicle.getImages().isEmpty()) {
                        image.setIsPrimary(true);
                    }
                    vehicle.addImage(image);
                }
            }
            // Lưu lại xe với danh sách ảnh đã thêm
            vehicleRepository.save(vehicle);
        }
    }

    // --- Phương thức mới: Cập nhật xe (gọi từ AdminVehicleController) ---
    @Transactional
    public void updateVehicle(Long id, VehicleDto dto, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));

        // Cập nhật thông tin cơ bản
        vehicle.setName(dto.getName());
        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setDescription(dto.getDescription());
        vehicle.setPricePerDay(dto.getPricePerDay());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        vehicle.setColor(dto.getColor());

        // Cập nhật trạng thái nếu có thay đổi
        if (dto.getStatus() != null) {
            try {
                vehicle.setStatus(Vehicle.VehicleStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // Giữ nguyên status cũ hoặc log lỗi
            }
        }

        // Xử lý ảnh mới thêm vào
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String imageUrl = saveFile(file);
                    VehicleImage image = new VehicleImage();
                    image.setImageUrl(imageUrl);
                    image.setVehicle(vehicle);
                    vehicle.addImage(image);
                }
            }
        }

        vehicleRepository.save(vehicle);
    }

    // --- Phương thức mới: Xóa xe (gọi từ AdminVehicleController) ---
    @Transactional
    public void deleteVehicle(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new RuntimeException("Xe không tồn tại");
        }
        vehicleRepository.deleteById(id);
    }

    // --- Helper: Map Entity sang Detail DTO ---
    private VehicleDetailDto mapToDetailDto(Vehicle vehicle) {
        VehicleDetailDto dto = modelMapper.map(vehicle, VehicleDetailDto.class);
        dto.setImageUrls(vehicle.getImages().stream()
                .map(VehicleImage::getImageUrl).collect(Collectors.toList()));

        Double avgRating = feedbackRepository.getAverageRatingByVehicleId(vehicle.getId());
        dto.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        return dto;
    }

    // --- Helper: Lưu file ---
    private String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Trả về đường dẫn tương đối để lưu vào DB (ví dụ: /uploads/abc.jpg)
        return "/" + UPLOAD_DIR + fileName;
    }
}