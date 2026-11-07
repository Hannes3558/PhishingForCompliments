package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.logic.MailTemplates.Template;
import com.example.phishingforcompliments.logic.Manipulations.Variant;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManipulationsTest {

    private final RuleChecker checker = new RuleChecker();

    private static List<Template> internalTemplates() {
        List<Template> all = new ArrayList<>(MailTemplates.LURES);
        all.addAll(MailTemplates.LEGIT_INTERNAL);
        return all;
    }

    private static List<Template> partnerTemplates() {
        List<Template> all = new ArrayList<>(MailTemplates.PARTNER_LURES);
        all.addAll(MailTemplates.LEGIT_PARTNER);
        return all;
    }

    @Test
    void allTemplatesAreCleanBeforeManipulation() {
        List<Template> all = new ArrayList<>(internalTemplates());
        all.addAll(partnerTemplates());
        for (Template t : all) {
            for (long seed = 0; seed < 10; seed++) {
                Email e = new MailDraft(t, new Random(seed), "Alex", seed % 2 == 0).toEmail();
                assertTrue(checker.violations(e, 5).isEmpty(), "Vorlage " + t.key() + " ist nicht sauber: " + e);
            }
        }
    }

    @Test
    void everyVariantViolatesExactlyItsOwnFeature() {
        for (Variant v : Manipulations.ALL) {
            List<Template> templates = new ArrayList<>(internalTemplates());
            if (!v.internalOnly()) {
                templates.addAll(partnerTemplates());
            }
            for (Template t : templates) {
                for (long seed = 0; seed < 25; seed++) {
                    Random rng = new Random(seed);
                    MailDraft d = new MailDraft(t, rng, "Alex", false);
                    v.apply().accept(d, rng);
                    Email e = d.toEmail();
                    assertEquals(List.of(v.feature()), checker.violations(e, 5),
                            "Variante \"" + v.name() + "\" auf Vorlage " + t.key() + ": " + e);
                    assertFalse(checker.explain(v.feature(), e).isBlank());
                }
            }
        }
    }

    @Test
    void everyFeatureHasSeveralSubtleAndObviousVariants() {
        for (Feature f : Feature.values()) {
            assertTrue(Manipulations.variants(f, true, false).size() >= 4, "zu wenig subtile Fehlerarten für " + f);
            assertTrue(Manipulations.variants(f, false, false).size() >= 3, "zu wenig deutliche Fehlerarten für " + f);
        }
    }

    @Test
    void requestedVariantsExist() {
        List<String> names = Manipulations.ALL.stream().map(Variant::name).toList();
        assertTrue(names.contains("Stern fehlt"), "Logo nur ohne Stern");
        long partialCode = Manipulations.ALL.stream()
                .filter(v -> v.feature() == Feature.SIGNATUR && v.subtle() && v.name().startsWith("Code:"))
                .count();
        assertTrue(partialCode >= 3, "Mitarbeiter-Code nur teilweise falsch");
    }

    @Test
    void lookalikeNeverReturnsTheOriginal() {
        for (String stem : List.of("komplimento", "nordlicht-logistik", "bueroprofi24", "fernweh-reisen")) {
            for (long seed = 0; seed < 200; seed++) {
                String fake = Manipulations.lookalike(stem, new Random(seed));
                assertFalse(fake.equalsIgnoreCase(stem), fake);
            }
        }
    }
}
