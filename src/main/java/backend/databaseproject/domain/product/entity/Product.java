package backend.databaseproject.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상품 엔티티
 */
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "unit_weight_kg", nullable = false, precision = 6, scale = 3)
    private BigDecimal unitWeightKg;

    @Column(name = "requires_verification", nullable = false)
    private Boolean requiresVerification = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Product(String name, String category, BigDecimal unitWeightKg,
                   Boolean requiresVerification, Boolean isActive) {
        this.name = name;
        this.category = category;
        this.unitWeightKg = unitWeightKg;
        this.requiresVerification = requiresVerification != null ? requiresVerification : false;
        this.isActive = isActive != null ? isActive : true;
    }
}
