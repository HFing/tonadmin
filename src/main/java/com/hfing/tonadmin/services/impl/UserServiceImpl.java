package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.common.RoleType;
import com.hfing.tonadmin.common.UserStatus;
import com.hfing.tonadmin.dto.request.CreateUserRequest;
import com.hfing.tonadmin.dto.request.UpdateUserRequest;
import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.Role;
import com.hfing.tonadmin.entities.User;
import com.hfing.tonadmin.entities.UserHasRole;
import com.hfing.tonadmin.mappers.UserMapper;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.repositories.RoleRepository;
import com.hfing.tonadmin.repositories.UserRepository;
import com.hfing.tonadmin.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    @Override
    @Transactional
    public boolean createUser(CreateUserRequest request, BindingResult bindingResult) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            bindingResult.rejectValue("email", "duplicate", "Email đã tồn tại");
            return false;
        }

        if (request.password() == null || request.password().isBlank()) {
            bindingResult.rejectValue("password", "required", "Mật khẩu không được để trống");
            return false;
        }

        if (request.roleType() == RoleType.BRANCH_STAFF &&
                (request.branchId() == null || request.branchId().isBlank())) {
            bindingResult.rejectValue("branchId", "required", "Nhân viên chi nhánh phải chọn chi nhánh");
            return false;
        }

        User user = userMapper.toUser(request);

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(trimToNull(request.firstName()));
        user.setLastName(trimToNull(request.lastName()));
        user.setPhone(trimToNull(request.phone()));
        user.setUserStatus(request.userStatus() == null ? UserStatus.ACTIVE : request.userStatus());

        setBranch(user, request.branchId(), request.roleType());
        setRole(user, request.roleType());

        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean updateUser(String id, UpdateUserRequest request, BindingResult bindingResult) {
        User user = getUserById(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            bindingResult.rejectValue("email", "duplicate", "Email đã tồn tại");
            return false;
        }

        if (request.roleType() == RoleType.BRANCH_STAFF &&
                (request.branchId() == null || request.branchId().isBlank())) {
            bindingResult.rejectValue("branchId", "required", "Nhân viên chi nhánh phải chọn chi nhánh");
            return false;
        }

        userMapper.updateUserFromRequest(request, user);

        user.setEmail(email);
        user.setFirstName(trimToNull(request.firstName()));
        user.setLastName(trimToNull(request.lastName()));
        user.setPhone(trimToNull(request.phone()));
        user.setUserStatus(request.userStatus() == null ? UserStatus.ACTIVE : request.userStatus());

        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                bindingResult.rejectValue("password", "size", "Mật khẩu tối thiểu 6 ký tự");
                return false;
            }

            user.setPassword(passwordEncoder.encode(request.password()));
        }

        setBranch(user, request.branchId(), request.roleType());
        setRole(user, request.roleType());

        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public void toggleStatus(String id) {
        User user = getUserById(id);

        if (user.getUserStatus() == UserStatus.ACTIVE) {
            user.setUserStatus(UserStatus.LOCKED);


            expireUserSessions(user.getEmail());
        } else {
            user.setUserStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);
    }

    private void setBranch(User user, String branchId, RoleType roleType) {
        if (roleType == RoleType.ADMIN) {
            user.setBranch(null);
            return;
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh"));

        user.setBranch(branch);
    }

    private void setRole(User user, RoleType roleType) {
        Role role = roleRepository.findByRoleType(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền"));

        if (user.getUserHasRoles() == null) {
            user.setUserHasRoles(new HashSet<>());
        }

        if (user.getUserHasRoles().isEmpty()) {
            UserHasRole userHasRole = UserHasRole.builder()
                    .user(user)
                    .role(role)
                    .build();

            user.getUserHasRoles().add(userHasRole);
            return;
        }

        UserHasRole existingUserRole = user.getUserHasRoles()
                .stream()
                .findFirst()
                .orElseThrow();

        existingUserRole.setUser(user);
        existingUserRole.setRole(role);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private void expireUserSessions(String email) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof User loggedInUser) {
                if (loggedInUser.getEmail().equalsIgnoreCase(email)) {
                    List<SessionInformation> sessions =
                            sessionRegistry.getAllSessions(principal, false);

                    for (SessionInformation session : sessions) {
                        session.expireNow();
                    }
                }
            }
        }
    }
}
