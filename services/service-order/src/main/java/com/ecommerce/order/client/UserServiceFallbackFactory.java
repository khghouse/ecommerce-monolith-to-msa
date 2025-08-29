package com.ecommerce.order.client;

import com.ecommerce.order.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Slf4j
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return id -> {
            logError(cause, id);
            return createFallbackUser(id);
        };
    }

    private void logError(Throwable cause, Long id) {
        String baseMsg = "[USER-SERVICE] Failed to fetch user {}: {}";
        if (cause instanceof SocketTimeoutException) {
            log.error(baseMsg, id, cause.getMessage(), cause);
        } else if (cause instanceof ConnectException) {
            log.error(baseMsg, id, cause.getMessage(), cause);
        } else {
            log.error(baseMsg, id, cause.getMessage(), cause);
        }
    }

    public UserDto createFallbackUser(Long id) {
        return new UserDto(id, "Unknown User", "unknown@example.com", true);
    }

}
