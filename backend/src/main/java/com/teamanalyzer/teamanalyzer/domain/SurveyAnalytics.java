// backend/src/main/java/com/teamanalyzer/teamanalyzer/domain/SurveyAnalytics.java
package com.teamanalyzer.teamanalyzer.domain;

import java.util.List;

public final class SurveyAnalytics {
    private SurveyAnalytics() {
    }

    /**
     * Berechnet Durchschnitte für die ersten 5 Fragen (idx 1..5) über alle
     * Responses.
     * Nutzt die relationale Struktur: SurveyResponse -> SurveyAnswer ->
     * SurveyQuestion(idx).
     */
    public static double[] averages(List<SurveyResponse> responses) {
        final int Q = 5;
        double[] sum = new double[Q];
        int[] cnt = new int[Q];

        for (SurveyResponse r : responses) {
            for (SurveyAnswer a : r.getAnswers()) {
                var q = a.getQuestion();
                if (q == null)
                    continue;
                int idx = q.getIdx(); // 1-basierter Index der Frage
                if (idx >= 1 && idx <= Q) {
                    sum[idx - 1] += a.getValue();
                    cnt[idx - 1] += 1;
                }
            }
        }

        double[] avg = new double[Q];
        for (int i = 0; i < Q; i++) {
            avg[i] = (cnt[i] == 0) ? 0.0 : (sum[i] / cnt[i]);
        }
        return avg;
    }
}
