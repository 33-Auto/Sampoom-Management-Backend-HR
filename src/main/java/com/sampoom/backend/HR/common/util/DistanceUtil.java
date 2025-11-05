package com.sampoom.backend.HR.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DistanceUtil {

    private static final double EARTH_RADIUS_KM = 6371.0; // 지구 반지름 (km)

    /**
     * 두 위도/경도 좌표 간의 거리(km)를 계산 (Haversine formula)
     * 결과는 소수점 둘째자리까지 반올림
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        // 소수점 둘째자리까지 반올림
        return roundToTwoDecimalPlaces(distance);
    }

    /**
     * 소수점 둘째자리까지 반올림
     */
    public static double roundToTwoDecimalPlaces(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
