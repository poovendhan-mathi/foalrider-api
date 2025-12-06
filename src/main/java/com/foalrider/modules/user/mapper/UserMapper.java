package com.foalrider.modules.user.mapper;

import com.foalrider.modules.user.dto.UserResponse;
import com.foalrider.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for User entity.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    UserResponse toResponse(User user);
}
