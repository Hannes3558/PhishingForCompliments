package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.DayReport;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.model.Outcome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spielablauf eines Tages ohne jede UI-Abhängigkeit: Mails erzeugen, Entscheidungen
 * bewerten, Punkte und Verwarnungen führen, Uhr laufen lassen, Tag abschließen.
 */
public final class GameSession {

    public static final int POINTS_REPORT_CORRECT = 10;
    public static final int POINTS_REPORT_WRONG = -5;
    public static final int POINTS_FILE_CORRECT = 10;
    public static final int POINTS_FILE_WRONG = -20;
    public static final int POINTS_UNPROCESSED = -5;

    private static final int WORKDAY_START_MINUTES = 9 * 60;
    private static final int WORKDAY_MINUTES = 8 * 60;

    private final RuleChecker checker = new RuleChecker();
    private final MailGenerator generator = new MailGenerator();

    private GameState state;
    private final List<Email> openMails = new ArrayList<>();
    private final Set<String> readIds = new HashSet<>();
    private final List<DecisionResult> dayResults = new ArrayList<>();
    private int pointsToday;
    private double elapsedSeconds;
    private boolean paused;
    private boolean dayRunning;
    private Runnable onTimeOver = () -> { };

    public GameSession(GameState state) {
        this.state = state;
    }

    // ------------------------------------------------------------------
    // Tagesablauf
    // ------------------------------------------------------------------

    public void startDay() {
        openMails.clear();
        readIds.clear();
        dayResults.clear();
        pointsToday = 0;
        elapsedSeconds = 0;
        paused = false;
        dayRunning = true;
        openMails.addAll(generator.generate(state.difficulty(), state.day(), state.seed(), state.playerName()));
    }

    /** Schließt den Tag ab: offene Mails zählen als unbearbeitet. Erhöht den Tag im Spielstand. */
    public DayReport endDay() {
        dayRunning = false;
        for (Email email : new ArrayList<>(openMails)) {
            List<Feature> violations = checker.violations(email, state.day());
            String explanation = violations.isEmpty()
                    ? "Diese Mail war harmlos und hätte als sicher abgelegt werden können."
                    : "Diese Mail war Phishing: " + violationSummary(email, violations);
            record(new DecisionResult(email, Outcome.UNPROCESSED, POINTS_UNPROCESSED, explanation, violations));
        }
        openMails.clear();
        DayReport report = new DayReport(state.day(), dayResults, pointsToday, state.score(), state.warnings(),
                state.isFired(), state.day() + 1 > GameState.CAMPAIGN_DAYS);
        state = state.withDay(state.day() + 1);
        return report;
    }

    // ------------------------------------------------------------------
    // Entscheidungen (UC-07)
    // ------------------------------------------------------------------

    /** Spieler meldet ein Merkmal als Abweichung von den Corporate Guidelines. */
    public DecisionResult reportFeature(Email email, Feature feature) {
        requireOpen(email);
        List<Feature> violations = checker.violations(email, state.day());
        if (violations.contains(feature)) {
            String explanation = "Richtig erkannt! " + checker.explain(feature, email);
            if (violations.size() > 1) {
                explanation += "\n\nDiese Mail hatte außerdem Abweichungen bei: "
                        + violations.stream().filter(f -> f != feature).map(Feature::label)
                        .collect(Collectors.joining(", ")) + ".";
            }
            return record(new DecisionResult(email, Outcome.REPORTED_CORRECT, POINTS_REPORT_CORRECT, explanation, violations));
        }
        String explanation = "Das Merkmal \"" + feature.label() + "\" ist in Ordnung: "
                + checker.explain(feature, email) + "\nPrüfe weitere Merkmale oder lege die Mail als sicher ab.";
        return record(new DecisionResult(email, Outcome.REPORTED_WRONG, POINTS_REPORT_WRONG, explanation, violations));
    }

    /** Spieler legt die Mail als sicher ab. */
    public DecisionResult fileAsSafe(Email email) {
        requireOpen(email);
        List<Feature> violations = checker.violations(email, state.day());
        if (violations.isEmpty()) {
            return record(new DecisionResult(email, Outcome.FILED_CORRECT, POINTS_FILE_CORRECT,
                    "Richtig – diese Mail war harmlos.", violations));
        }
        state = state.withWarnings(state.warnings() + 1);
        String explanation = "Das war eine Phishing-Mail! Übersehen: " + violationSummary(email, violations);
        return record(new DecisionResult(email, Outcome.FILED_WRONG, POINTS_FILE_WRONG, explanation, violations));
    }

    private DecisionResult record(DecisionResult result) {
        dayResults.add(result);
        pointsToday += result.points();
        state = state.withScore(state.score() + result.points());
        if (result.closesEmail()) {
            openMails.removeIf(m -> m.id().equals(result.email().id()));
        }
        return result;
    }

    private String violationSummary(Email email, List<Feature> violations) {
        return violations.stream()
                .map(f -> f.label() + " – " + checker.explain(f, email))
                .collect(Collectors.joining("\n"));
    }

    private void requireOpen(Email email) {
        if (openMails.stream().noneMatch(m -> m.id().equals(email.id()))) {
            throw new IllegalStateException("Mail ist nicht (mehr) offen: " + email.id());
        }
    }

    // ------------------------------------------------------------------
    // Uhr
    // ------------------------------------------------------------------

    /** Von der UI regelmäßig aufgerufen; echte Sekunden seit dem letzten Aufruf. */
    public void tick(double seconds) {
        if (!dayRunning || paused) {
            return;
        }
        elapsedSeconds += seconds;
        if (elapsedSeconds >= state.difficulty().daySeconds()) {
            elapsedSeconds = state.difficulty().daySeconds();
            dayRunning = false;
            onTimeOver.run();
        }
    }

    public void setOnTimeOver(Runnable callback) {
        this.onTimeOver = callback == null ? () -> { } : callback;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isDayRunning() {
        return dayRunning;
    }

    /** Anteil des Arbeitstags, 0..1. */
    public double dayProgress() {
        return Math.min(1.0, elapsedSeconds / state.difficulty().daySeconds());
    }

    /** Spielzeit als "HH:MM" zwischen 09:00 und 17:00. */
    public String clockText() {
        int minutes = WORKDAY_START_MINUTES + (int) (dayProgress() * WORKDAY_MINUTES);
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    // ------------------------------------------------------------------
    // Zustand
    // ------------------------------------------------------------------

    public GameState state() {
        return state;
    }

    public RuleChecker checker() {
        return checker;
    }

    public List<Email> openMails() {
        return List.copyOf(openMails);
    }

    public List<DecisionResult> dayResults() {
        return List.copyOf(dayResults);
    }

    public int pointsToday() {
        return pointsToday;
    }

    public void markRead(Email email) {
        readIds.add(email.id());
    }

    public boolean isRead(Email email) {
        return readIds.contains(email.id());
    }

    public long unreadCount() {
        return openMails.stream().filter(m -> !readIds.contains(m.id())).count();
    }
}
