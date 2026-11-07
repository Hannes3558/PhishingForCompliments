package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.GameState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailGeneratorTest {

    private static final int SEEDS = 40;

    /** Partner-Mails sprechen niemanden mit Namen an. */
    private static final Set<String> PARTNER_SUBJECTS = Stream.concat(
                    MailTemplates.LEGIT_PARTNER.stream(), MailTemplates.PARTNER_LURES.stream())
            .map(MailTemplates.Template::subject)
            .collect(Collectors.toSet());

    private final MailGenerator generator = new MailGenerator();
    private final RuleChecker checker = new RuleChecker();

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void generatesExpectedCountsAndEveryMailIsClassifiable(Difficulty difficulty) {
        for (int day = 1; day <= GameState.CAMPAIGN_DAYS; day++) {
            for (long seed = 1; seed <= SEEDS; seed++) {
                List<Email> mails = generator.generate(difficulty, day, seed, "Alex");
                assertEquals(difficulty.mailsForDay(day), mails.size(), "Anzahl Mails " + difficulty + " Tag " + day);

                int finalDay = day;
                long phishing = mails.stream().filter(m -> checker.isPhishing(m, finalDay)).count();
                assertEquals(MailGenerator.phishingCount(difficulty, day), phishing,
                        "Phishing-Anzahl " + difficulty + " Tag " + day + " Seed " + seed + ": " + mails);

                Set<String> ids = new HashSet<>();
                for (Email m : mails) {
                    assertTrue(ids.add(m.id()), "IDs müssen eindeutig sein");
                    if (!PARTNER_SUBJECTS.contains(m.subject())) {
                        assertTrue(m.body().contains("Alex"), "Spielername im Text interner Mails: " + m);
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void newFeatureIsTheOnlyViolationInAtLeastTwoMails(Difficulty difficulty) {
        for (int day = 1; day <= GameState.CAMPAIGN_DAYS; day++) {
            Feature newFeature = Feature.introducedOn(day);
            for (long seed = 1; seed <= SEEDS; seed++) {
                int finalDay = day;
                long focused = generator.generate(difficulty, day, seed, "Alex").stream()
                        .filter(m -> checker.violations(m, finalDay).equals(List.of(newFeature)))
                        .count();
                assertTrue(focused >= MailGenerator.NEW_FEATURE_MIN_MAILS,
                        difficulty + " Tag " + day + " Seed " + seed + ": nur " + focused
                                + " Mails mit ausschließlich " + newFeature);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void isDeterministicForSameSeed(Difficulty difficulty) {
        List<Email> a = generator.generate(difficulty, 3, 99L, "Kim");
        List<Email> b = generator.generate(difficulty, 3, 99L, "Kim");
        assertEquals(a, b);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void phishingManipulationsOnlyTouchActiveFeatures(Difficulty difficulty) {
        // Mails von Tag 1 dürfen keine versteckten Fehler bei Merkmalen haben, die erst später gelten.
        for (long seed = 1; seed <= SEEDS; seed++) {
            for (Email m : generator.generate(difficulty, 1, seed, "Alex")) {
                assertEquals(checker.violations(m, 1), checker.violations(m, 5),
                        "Tag-1-Mail hat versteckte Verstöße bei inaktiven Merkmalen: " + m);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void producesVarietyOfSenderDomains(Difficulty difficulty) {
        Set<String> fakeDomains = new HashSet<>();
        for (long seed = 1; seed <= SEEDS; seed++) {
            for (Email m : generator.generate(difficulty, 1, seed, "Alex")) {
                if (checker.isPhishing(m, 1)) {
                    fakeDomains.add(m.senderDomain());
                }
            }
        }
        assertTrue(fakeDomains.size() >= 15, "zu wenig Abwechslung bei gefälschten Domains: " + fakeDomains);
    }
}
