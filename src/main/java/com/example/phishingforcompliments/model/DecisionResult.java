package com.example.phishingforcompliments.model;

import java.util.List;

/**
 * Ausgewertete Entscheidung des Spielers zu einer Mail.
 *
 * @param email        betroffene Mail
 * @param outcome      Art des Ergebnisses
 * @param points       Punktänderung (kann negativ sein)
 * @param explanation  Erklärung für den Spieler (Lerneffekt)
 * @param violations   tatsächliche Regelverstöße der Mail (für den Tagesbericht)
 */
public record DecisionResult(Email email, Outcome outcome, int points, String explanation, List<Feature> violations) {

    public boolean isCorrect() {
        return outcome == Outcome.REPORTED_CORRECT || outcome == Outcome.FILED_CORRECT;
    }

    public boolean closesEmail() {
        return outcome != Outcome.REPORTED_WRONG;
    }
}
