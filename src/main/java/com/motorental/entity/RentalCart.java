package com.motorental.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rental_carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalCart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude // Ngắt vòng lặp
    private User user;

    @OneToMany(mappedBy = "rentalCart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude // Ngắt vòng lặp
    @EqualsAndHashCode.Exclude
    private Set<RentalCartItem> items = new HashSet<>();

    public void addItem(RentalCartItem item) {
        items.add(item);
        item.setRentalCart(this);
    }

    public void removeItem(RentalCartItem item) {
        items.remove(item);
        item.setRentalCart(null);
    }

    public void clearItems() {
        // Cần copy ra list mới để tránh ConcurrentModificationException khi loop xóa
        new HashSet<>(items).forEach(this::removeItem);
    }
}