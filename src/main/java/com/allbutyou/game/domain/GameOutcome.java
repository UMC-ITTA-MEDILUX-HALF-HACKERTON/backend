package com.allbutyou.game.domain;

/** 지각자 팀 기준 결과와 1인당 지각비 규칙. */
public enum GameOutcome {
    /** 지각자 평균 > 도착자 평균: 지각비 면제 */
    WIN(0),
    /** 지각자 평균 < 도착자 평균: 기본 지각비 x2 */
    LOSE(2),
    /** 평균 같음: 기본 지각비 */
    DRAW(1),
    /** 어느 한 팀이라도 제출자 0명: 기본 지각비로 표시 */
    PENDING(1);

    private final int feeMultiplier;

    GameOutcome(int feeMultiplier) {
        this.feeMultiplier = feeMultiplier;
    }

    public int penalty(int baseLateFee) {
        return baseLateFee * feeMultiplier;
    }

    /**
     * 팀별 제출 점수 합과 제출자 수로 결과를 정한다.
     * 평균을 double로 나눠 비교하지 않고 lateSum/lateCount 와 onTimeSum/onTimeCount 를 교차 곱으로 정확히 비교한다.
     */
    public static GameOutcome decide(long lateSum, int lateCount, long onTimeSum, int onTimeCount) {
        if (lateCount == 0 || onTimeCount == 0) {
            return PENDING;
        }
        int cmp = Long.compare(lateSum * onTimeCount, onTimeSum * lateCount);
        if (cmp > 0) {
            return WIN;
        }
        return cmp < 0 ? LOSE : DRAW;
    }
}
