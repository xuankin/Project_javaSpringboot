package com.motorental.controller.admin;

import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/vehicles") // SỬA: Bỏ "templates/"
@RequiredArgsConstructor
public class AdminVehicleController {

    private final VehicleService vehicleService;

    // Danh sách xe (Admin view)
    @GetMapping
    public String list(Model model) {
        model.addAttribute("vehicles", vehicleService.searchVehicles("", null, Pageable.unpaged()).getContent());
        return "admin/vehicles/list"; // SỬA: Bỏ "templates/"
    }

    // Form tạo mới
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("vehicle", new VehicleDto());
        return "admin/vehicles/form"; // SỬA: Bỏ "templates/"
    }

    // Xử lý tạo mới
    @PostMapping("/create")
    public String save(@Valid @ModelAttribute("vehicle") VehicleDto dto,
                       BindingResult result,
                       @RequestParam("imageFiles") List<MultipartFile> imageFiles,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (result.hasErrors()) return "admin/vehicles/form"; // SỬA

        try {
            vehicleService.createVehicle(dto, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Thêm xe thành công");
            return "redirect:/admin/vehicles";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/vehicles/form"; // SỬA
        }
    }

    // Form sửa xe
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicleDetail(id));
        return "admin/vehicles/form"; // SỬA
    }

    // Xử lý cập nhật
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("vehicle") VehicleDto dto,
                         @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                         RedirectAttributes redirectAttributes) {
        try {
            vehicleService.updateVehicle(id, dto, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    // Xóa xe
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.deleteVehicle(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa xe.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }
}