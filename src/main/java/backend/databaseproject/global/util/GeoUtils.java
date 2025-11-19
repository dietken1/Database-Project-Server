package backend.databaseproject.global.util;

/**
 * 지리 정보 유틸리티 클래스
 * 위경도 기반 거리 계산 등의 기능을 제공합니다.
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Haversine 공식을 이용한 두 지점 간의 거리 계산
     *
     * @param lat1 시작점 위도
     * @param lng1 시작점 경도
     * @param lat2 도착점 위도
     * @param lng2 도착점 경도
     * @return 두 지점 간의 거리 (km)
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 특정 지점이 배송 가능 범위 내에 있는지 확인
     *
     * @param centerLat 중심점 위도 (매장)
     * @param centerLng 중심점 경도 (매장)
     * @param targetLat 목표 위도 (고객)
     * @param targetLng 목표 경도 (고객)
     * @param radiusKm  배송 가능 반경 (km)
     * @return 범위 내에 있으면 true, 아니면 false
     */
    public static boolean isWithinRadius(double centerLat, double centerLng,
                                         double targetLat, double targetLng,
                                         double radiusKm) {
        double distance = calculateDistance(centerLat, centerLng, targetLat, targetLng);
        return distance <= radiusKm;
    }

    /**
     * 두 지점 사이의 중간 지점 계산 (선형 보간)
     *
     * @param lat1     시작점 위도
     * @param lng1     시작점 경도
     * @param lat2     도착점 위도
     * @param lng2     도착점 경도
     * @param fraction 진행률 (0.0 ~ 1.0)
     * @return [위도, 경도] 배열
     */
    public static double[] interpolate(double lat1, double lng1, double lat2, double lng2, double fraction) {
        double lat = lat1 + (lat2 - lat1) * fraction;
        double lng = lng1 + (lng2 - lng1) * fraction;
        return new double[]{lat, lng};
    }
}
