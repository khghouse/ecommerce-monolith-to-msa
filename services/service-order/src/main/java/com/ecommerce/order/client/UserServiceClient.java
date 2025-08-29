package com.ecommerce.order.client;

import com.ecommerce.order.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-user")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);

}
