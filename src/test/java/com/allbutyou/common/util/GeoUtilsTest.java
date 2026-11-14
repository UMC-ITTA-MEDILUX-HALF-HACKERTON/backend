package com.allbutyou.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    /** 반지름 6,371km 구에서 위도/경도 1도에 해당하는 대권 거리 = R * π / 180 */
    private static final double ONE_DEGREE_METERS = GeoUtils.EARTH_RADIUS_METERS * Math.PI / 180;

    @Test
    void 같은_좌표는_거리가_0이다() {
        assertThat(GeoUtils.distanceMeters(37.5665, 126.9780, 37.5665, 126.9780)).isEqualTo(0.0);
    }

    @Test
    void 경선을_따라_위도_1도는_약_111km다() {
        double d = GeoUtils.distanceMeters(37.0, 127.0, 38.0, 127.0);
        assertThat(d).isCloseTo(ONE_DEGREE_METERS, within(0.01));
        assertThat(d).isCloseTo(111_195, within(1.0));
    }

    @Test
    void 적도에서_경도_1도는_위도_1도와_같다() {
        assertThat(GeoUtils.distanceMeters(0, 0, 0, 1)).isCloseTo(ONE_DEGREE_METERS, within(0.01));
    }

    @Test
    void 거리는_대칭이다() {
        double ab = GeoUtils.distanceMeters(37.5547, 126.9707, 37.4979, 127.0276);
        double ba = GeoUtils.distanceMeters(37.4979, 127.0276, 37.5547, 126.9707);
        assertThat(ab).isCloseTo(ba, within(1e-6));
    }

    @Test
    void 위도_0_0009도_차이는_약_100m다() {
        double d = GeoUtils.distanceMeters(37.5665, 126.9780, 37.5674, 126.9780);
        assertThat(d).isCloseTo(ONE_DEGREE_METERS * 0.0009, within(0.01));
        assertThat(d).isBetween(99.0, 101.0);
    }

    @Test
    void 반대편_지점은_반원둘레다() {
        assertThat(GeoUtils.distanceMeters(0, 0, 0, 180))
                .isCloseTo(Math.PI * GeoUtils.EARTH_RADIUS_METERS, within(0.01));
    }
}
