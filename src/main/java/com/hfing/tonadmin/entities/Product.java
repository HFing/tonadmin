package com.hfing.tonadmin.entities;

import com.hfing.tonadmin.common.BaseEntity;
import com.hfing.tonadmin.common.UnitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType unit;

    private BigDecimal thickness;

    private BigDecimal width;

    private BigDecimal length;

    private String color;

    private String material;

    // Chỉ ADMIN được xem
    @Builder.Default
    private BigDecimal importPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal minStock = BigDecimal.ZERO;

    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Inventory> inventories = new HashSet<>();


}
