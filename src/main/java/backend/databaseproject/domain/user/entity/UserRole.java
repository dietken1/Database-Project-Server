package backend.databaseproject.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 역할 Enum
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    CUSTOMER("고객"),
    OWNER("점주");

    private final String description;
}
