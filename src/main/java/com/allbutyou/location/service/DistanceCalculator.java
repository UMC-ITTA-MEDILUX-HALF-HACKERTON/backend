package com.allbutyou.location.service;

public class DistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public static double calculate(
            double lat1, double lon1,
            double lat2, double lon2) {

        double latDifference = Math.toRadians(lat2 - lat1);
        double lonDifference = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(latDifference / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(lonDifference / 2), 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return EARTH_RADIUS_METERS * c;
    }
}