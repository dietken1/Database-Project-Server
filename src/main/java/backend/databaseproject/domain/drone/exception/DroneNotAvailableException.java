package backend.databaseproject.domain.drone.exception;

/**
 * 사용 가능한 드론이 없을 때 발생하는 예외
 */
public class DroneNotAvailableException extends RuntimeException {
    public DroneNotAvailableException(String message) {
        super(message);
    }
}
