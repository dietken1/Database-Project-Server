package backend.databaseproject.domain.user.controller;

import backend.databaseproject.domain.user.dto.response.UserResponse;
import backend.databaseproject.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 정보 조회
     * 요청 헤더의 userId를 통해 사용자 정보를 조회합니다.
     *
     * @param userId 사용자 ID (헤더)
     * @return 사용자 정보 (이름, 주소, 위도, 경도)
     */
    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "요청 헤더의 userId를 통해 사용자의 이름, 주소, 위치 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (헤더 누락 또는 사용자를 찾을 수 없음)"
                    )
            }
    )
    public UserResponse getMyInfo(
            @Parameter(
                    name = "userId",
                    description = "사용자 ID",
                    required = true,
                    example = "1"
            )
            @RequestHeader("userId") Long userId
    ) {
        return userService.getUserInfo(userId);
    }
}
