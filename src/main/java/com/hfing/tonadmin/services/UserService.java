package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.CreateUserRequest;
import com.hfing.tonadmin.dto.request.UpdateUserRequest;
import com.hfing.tonadmin.entities.User;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();

    User getUserById(String id);

    boolean createUser(CreateUserRequest request, BindingResult bindingResult);

    boolean updateUser(String id, UpdateUserRequest request, BindingResult bindingResult);

    void toggleStatus(String id);
}
