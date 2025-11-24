package backend.databaseproject.domain.user.service;

import backend.databaseproject.domain.user.dto.response.UserResponse;
import backend.databaseproject.domain.user.entity.User;
import backend.databaseproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public UserResponse getUserInfo(Long userId) {
        log.debug("사용자 정보 조회 요청: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. userId: " + userId
                ));

        log.debug("사용자 정보 조회 완료: {}", user.getName());
        return UserResponse.from(user);
    }
}
