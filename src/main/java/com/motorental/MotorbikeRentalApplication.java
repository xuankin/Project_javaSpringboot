package com.motorental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MotorbikeRentalApplication {

	public static void main(String[] args) {
		SpringApplication.run(MotorbikeRentalApplication.class, args);
	}

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner dataMigrationRunner(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Phục hồi data từ cột TEXT sang cột NVARCHAR(MAX)
                jdbcTemplate.execute("UPDATE feedbacks SET content_nv = CAST(content AS NVARCHAR(MAX)) WHERE content_nv IS NULL AND content IS NOT NULL;");
                jdbcTemplate.execute("UPDATE vehicles SET description_nv = CAST(description AS NVARCHAR(MAX)) WHERE description_nv IS NULL AND description IS NOT NULL;");
                jdbcTemplate.execute("UPDATE rental_orders SET notes_nv = CAST(notes AS NVARCHAR(MAX)) WHERE notes_nv IS NULL AND notes IS NOT NULL;");
                jdbcTemplate.execute("UPDATE payments SET notes_nv = CAST(notes AS NVARCHAR(MAX)) WHERE notes_nv IS NULL AND notes IS NOT NULL;");
                System.out.println("Đã phục hồi dữ liệu tiếng việt cũ thành công!");
            } catch (Exception e) {
                System.out.println("Bỏ qua phục hồi dữ liệu: " + e.getMessage());
            }
        };
    }
}
