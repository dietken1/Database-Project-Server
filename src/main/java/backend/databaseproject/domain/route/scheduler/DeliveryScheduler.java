package backend.databaseproject.domain.route.scheduler;

import backend.databaseproject.domain.route.service.DeliveryBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 배송 스케줄러
 * 주기적으로 배송 배치 처리를 실행합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryScheduler {

    private final DeliveryBatchService deliveryBatchService;

    /**
     * 매 10분마다 배송 배치 처리 실행
     * cron 표현식: "초 분 시 일 월 요일"
     * 매 10분마다 실행 (0분, 10분, 20분, 30분, 40분, 50분)
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void scheduleBatch() {
        log.info("========================================");
        log.info("배송 배치 처리 스케줄러 시작");
        log.info("========================================");

        try {
            deliveryBatchService.processBatch();
        } catch (Exception e) {
            log.error("배송 배치 처리 스케줄러 실행 중 오류 발생", e);
        }

        log.info("========================================");
        log.info("배송 배치 처리 스케줄러 완료");
        log.info("========================================");
    }
}
