package com.hfing.tonadmin.configs;

import com.hfing.tonadmin.entities.Role;
import com.hfing.tonadmin.entities.User;
import com.hfing.tonadmin.entities.UserHasRole;
import com.hfing.tonadmin.common.RoleType;
import com.hfing.tonadmin.common.UserStatus;
import com.hfing.tonadmin.repositories.RoleRepository;
import com.hfing.tonadmin.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = createRoleIfNotExists(RoleType.ADMIN, "Quản trị viên");
        createRoleIfNotExists(RoleType.BRANCH_STAFF, "Nhân viên chi nhánh");

        if (!userRepository.existsByEmail("admin@tonadmin.com")) {
            User admin = User.builder()
                    .email("admin@tonadmin.com")
                    .password(passwordEncoder.encode("tonadmin"))
                    .firstName("Admin")
                    .lastName("TonAdmin")
                    .userStatus(UserStatus.ACTIVE)
                    .userHasRoles(new HashSet<>())
                    .build();

            UserHasRole userHasRole = UserHasRole.builder()
                    .user(admin)
                    .role(adminRole)
                    .build();

            admin.getUserHasRoles().add(userHasRole);

            userRepository.save(admin);
        }
    }

    private Role createRoleIfNotExists(RoleType roleType, String name) {
        return roleRepository.findByRoleType(roleType)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleType(roleType)
                                .name(name)
                                .build()
                ));
    }
}