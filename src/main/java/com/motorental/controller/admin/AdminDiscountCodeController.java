package com.motorental.controller.admin;

import com.motorental.model.DiscountCode;
import com.motorental.service.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/discounts")
@RequiredArgsConstructor
public class AdminDiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @GetMapping
    public String showDiscountManagementPage(Model model) {
        model.addAttribute("discounts", discountCodeService.getAllDiscountCodes());
        model.addAttribute("newDiscount", new DiscountCode());
        return "admin/discounts";
    }

    @PostMapping("/add")
    public String addDiscountCode(@ModelAttribute("newDiscount") DiscountCode newDiscount, RedirectAttributes redirectAttributes) {
        if (discountCodeService.findByCode(newDiscount.getCode()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Mã giảm giá '" + newDiscount.getCode() + "' đã tồn tại!");
            return "redirect:/admin/discounts";
        }

        newDiscount.setActive(true); // By default, new codes are active
        discountCodeService.createDiscountCode(newDiscount);
        redirectAttributes.addFlashAttribute("success", "Thêm mã giảm giá thành công.");
        return "redirect:/admin/discounts";
    }

    @PostMapping("/delete/{id}")
    public String deleteDiscountCode(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            discountCodeService.deleteDiscountCode(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa mã giảm giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Không thể xóa mã (có thể mã đã được sử dụng trong hóa đơn).");
        }
        return "redirect:/admin/discounts";
    }

    @PostMapping("/edit")
    public String editDiscountCode(@ModelAttribute("editedDiscount") DiscountCode editedDiscount, RedirectAttributes redirectAttributes) {
        try {
            java.util.Optional<DiscountCode> existingOpt = discountCodeService.findByCode(editedDiscount.getCode());
            if (existingOpt.isPresent() && !existingOpt.get().getId().equals(editedDiscount.getId())) {
                 redirectAttributes.addFlashAttribute("error", "Lỗi: Mã giảm giá '" + editedDiscount.getCode() + "' đã tông tại cho ID khác!");
                 return "redirect:/admin/discounts";
            }
            
            java.util.Optional<DiscountCode> currentOpt = discountCodeService.findById(editedDiscount.getId());
            if (currentOpt.isPresent()) {
                DiscountCode current = currentOpt.get();
                current.setCode(editedDiscount.getCode());
                current.setDiscountPercentage(editedDiscount.getDiscountPercentage());
                current.setExpiryDate(editedDiscount.getExpiryDate());
                current.setActive(editedDiscount.isActive());
                discountCodeService.createDiscountCode(current);
                redirectAttributes.addFlashAttribute("success", "Cập nhật mã giảm giá thành công.");
            }
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: Không thể cập nhật mã.");
        }
        return "redirect:/admin/discounts";
    }
}
