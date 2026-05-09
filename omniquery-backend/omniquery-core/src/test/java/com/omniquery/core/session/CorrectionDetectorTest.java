package com.omniquery.core.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrectionDetectorTest {

    private final CorrectionDetector detector = new CorrectionDetector();

    @Test
    void detectsFollowUpCorrections() {
        assertTrue(detector.isCorrection("只看 PAID 状态"));
        assertTrue(detector.isCorrection("刚才那个按金额排序"));
        assertTrue(detector.isCorrection("不是订单金额，是退款金额"));
    }

    @Test
    void keepsIndependentQuestionsAsNewQueries() {
        assertFalse(detector.isCorrection("show recent orders with customer names"));
    }
}
