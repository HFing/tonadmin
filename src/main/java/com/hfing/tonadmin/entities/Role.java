package com.hfing.tonadmin.entities;

import com.hfing.tonadmin.common.BaseEntity;
import com.hfing.tonadmin.common.RoleType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleType roleType;

    private String name;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserHasRole> userHasRoles = new HashSet<>();

    public SimpleGrantedAuthority getAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + roleType.name());
    }
}