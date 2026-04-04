package com.motorental.repository;

import com.motorental.entity.UserLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserLocationHistoryRepository extends JpaRepository<UserLocationHistory, Long> {
    
    List<UserLocationHistory> findByUserIdOrderByTimestampDesc(String userId);

    @Query("SELECT h FROM UserLocationHistory h WHERE h.user.id = :userId " +
           "AND h.timestamp >= :startDate AND h.timestamp <= :endDate " +
           "ORDER BY h.timestamp ASC")
    List<UserLocationHistory> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
