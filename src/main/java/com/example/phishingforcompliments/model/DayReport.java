package com.example.phishingforcompliments.model;

import java.util.List;

/** Zusammenfassung eines abgeschlossenen Arbeitstags (Tagesbericht). */
public record DayReport(
        int day,
        List<DecisionResult> results,
        int pointsToday,
        int totalScore,
        int warnings,
        boolean fired,
        boolean campaignFinished) {

    public DayReport {
        results = List.copyOf(results);
    }

    public long correctCount() {
        return results.stream().filter(DecisionResult::isCorrect).count();
    }

    /** Anteil korrekt behandelter Mails, 0..1. Zählt nur abschließende Entscheidungen. */
    public double accuracy() {
        long closing = results.stream().filter(DecisionResult::closesEmail).count();
        return closing == 0 ? 0 : (double) correctCount() / closing;
    }
}
