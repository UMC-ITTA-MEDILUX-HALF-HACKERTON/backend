package com.allbutyou.common.util;

public final class GeoUtils {

    /** 지구 평균 반지름 (m) */
    public static final double EARTH_RADIUS_METERS = 6_371_000d;

    private GeoUtils() {
    }

    /**
     * 하버사인 공식으로 두 좌표 사이의 직선(대권) 거리를 미터 단위로 계산한다.
     */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
