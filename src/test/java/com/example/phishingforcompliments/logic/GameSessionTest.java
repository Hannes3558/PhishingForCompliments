package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.DayReport;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.model.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    private final RuleChecker checker = new RuleChecker();

    private static GameSession startedSession(Difficulty difficulty, int day) {
        GameSession s = new GameSession(new GameState("Alex", difficulty, day, 0, 0, 7L));
        s.startDay();
        return s;
    }

    private Email first(GameSession s, boolean phishing) {
        int day = s.state().day();
        return s.openMails().stream().filter(m -> checker.isPhishing(m, day) == phishing).findFirst().orElseThrow();
    }

    @Test
    void reportingRealViolationClosesMailAndAwardsPoints() {
        GameSession s = startedSession(Difficulty.LEICHT, 1);
        Email phishing = first(s, true);
        int before = s.openMails().size();

        DecisionResult r = s.reportFeature(phishing, Feature.ABSENDER);

        assertEquals(Outcome.REPORTED_CORRECT, r.outcome());
        assertEquals(GameSession.POINTS_REPORT_CORRECT, r.points());
        assertEquals(GameSession.POINTS_REPORT_CORRECT, s.state().score());
        assertEquals(before - 1, s.openMails().size());
        assertThrows(IllegalStateException.class, () -> s.fileAsSafe(phishing));
    }

    @Test
    void reportingFineFeatureCostsPointsButKeepsMailOpen() {
        GameSession s = startedSession(Difficulty.LEICHT, 1);
        Email legit = first(s, false);
        int before = s.openMails().size();

        DecisionResult r = s.reportFeature(legit, Feature.ABSENDER);

        assertEquals(Outcome.REPORTED_WRONG, r.outcome());
        assertEquals(GameSession.POINTS_REPORT_WRONG, s.state().score());
        assertEquals(before, s.openMails().size());

        DecisionResult filed = s.fileAsSafe(legit);
        assertEquals(Outcome.FILED_CORRECT, filed.outcome());
        assertEquals(GameSession.POINTS_REPORT_WRONG + GameSession.POINTS_FILE_CORRECT, s.state().score());
    }

    @Test
    void filingPhishingAsSafeGivesWarningAndFiresAfterThree() {
        GameSession s = startedSession(Difficulty.LEICHT, 3); // 6 Mails, 3 Phishing
        List<Email> phishing = s.openMails().stream().filter(m -> checker.isPhishing(m, 3)).toList();
        assertEquals(3, phishing.size());

        DecisionResult r1 = s.fileAsSafe(phishing.get(0));
        assertEquals(Outcome.FILED_WRONG, r1.outcome());
        assertEquals(1, s.state().warnings());
        assertFalse(s.state().isFired());

        s.fileAsSafe(phishing.get(1));
        s.fileAsSafe(phishing.get(2));
        assertEquals(3, s.state().warnings());
        assertTrue(s.state().isFired());
        assertEquals(3 * GameSession.POINTS_FILE_WRONG, s.state().score());
    }

    @Test
    void endDayPenalisesUnprocessedMailsAndAdvancesDay() {
        GameSession s = startedSession(Difficulty.MITTEL, 1);
        int open = s.openMails().size();

        DayReport report = s.endDay();

        assertEquals(1, report.day());
        assertEquals(open, report.results().size());
        assertTrue(report.results().stream().allMatch(r -> r.outcome() == Outcome.UNPROCESSED));
        assertEquals(open * GameSession.POINTS_UNPROCESSED, report.pointsToday());
        assertEquals(2, s.state().day());
        assertTrue(s.openMails().isEmpty());
        assertFalse(report.campaignFinished());
    }

    @Test
    void lastDayReportMarksCampaignFinished() {
        GameSession s = startedSession(Difficulty.LEICHT, GameState.CAMPAIGN_DAYS);
        assertTrue(s.endDay().campaignFinished());
    }

    @Test
    void clockRunsOnlyWhileNotPausedAndFiresTimeOver() {
        GameSession s = startedSession(Difficulty.SCHWER, 1);
        boolean[] fired = {false};
        s.setOnTimeOver(() -> fired[0] = true);

        assertEquals("09:00", s.clockText());
        s.setPaused(true);
        s.tick(1000);
        assertEquals("09:00", s.clockText());
        s.setPaused(false);

        s.tick(Difficulty.SCHWER.daySeconds() / 2.0);
        assertEquals("13:00", s.clockText());
        assertFalse(fired[0]);

        s.tick(Difficulty.SCHWER.daySeconds());
        assertTrue(fired[0]);
        assertFalse(s.isDayRunning());
        assertEquals("17:00", s.clockText());
    }
}
