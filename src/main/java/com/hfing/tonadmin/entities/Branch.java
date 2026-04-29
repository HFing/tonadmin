package com.hfing.tonadmin.entities;

import com.hfing.tonadmin.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "branch", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "branch", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Inventory> inventories = new HashSet<>();
}
