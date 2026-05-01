package com.hfing.tonadmin.mappers;


import com.hfing.tonadmin.dto.request.CreateUserRequest;
import com.hfing.tonadmin.dto.request.UpdateUserRequest;
import com.hfing.tonadmin.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Mapping;


@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {


    User toUser(CreateUserRequest request);

    @Mapping(target = "password", ignore = true)
    UpdateUserRequest toUpdateUserRequest(User user);


    @Mapping(target = "password", ignore = true)
    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}