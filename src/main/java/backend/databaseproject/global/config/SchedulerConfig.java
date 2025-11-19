package backend.databaseproject.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 설정
 * 10분 단위 배송 배치 처리를 위한 스케줄러 활성화
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
