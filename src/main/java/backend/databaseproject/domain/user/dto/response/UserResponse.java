package backend.databaseproject.domain.user.dto.response;

import backend.databaseproject.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 사용자 정보 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "사용자 정보 응답")
public class UserResponse {

    @Schema(description = "사용자명", example = "홍길동")
    private String name;

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "위도", example = "37.5665")
    private BigDecimal lat;

    @Schema(description = "경도", example = "126.9780")
    private BigDecimal lng;

    /**
     * User 엔티티를 UserResponse로 변환
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .name(user.getName())
                .address(user.getAddress())
                .lat(user.getLat())
                .lng(user.getLng())
                .build();
    }
}
