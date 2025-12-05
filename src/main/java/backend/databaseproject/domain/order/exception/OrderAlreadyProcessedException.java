package backend.databaseproject.domain.order.exception;

/**
 * 이미 처리된 주문에 대한 예외
 */
public class OrderAlreadyProcessedException extends RuntimeException {
    public OrderAlreadyProcessedException(String message) {
        super(message);
    }
}
