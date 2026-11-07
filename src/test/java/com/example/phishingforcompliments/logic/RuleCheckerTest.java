package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Attachment;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.EmailLink;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.LogoSpec;
import com.example.phishingforcompliments.model.Signature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleCheckerTest {

    private final RuleChecker checker = new RuleChecker();

    private static final Signature VALID_SIGNATURE = new Signature("Markus Brandt", "IT-Sicherheit", "Q7M2H9KL");

    private static Email mail(String address, List<EmailLink> links, LogoSpec logo, Signature sig, List<Attachment> att) {
        String domain = address.substring(address.lastIndexOf('@') + 1);
        return new Email("x", "Absender", address, CorporateGuidelines.companyForDomain(domain), "Betreff",
                "Text {link}", links, logo, sig, att, "09:00");
    }

    private static Email cleanInternal() {
        return mail("markus.brandt@komplimento.de",
                List.of(new EmailLink("Wiki", "https://wiki.komplimento.de/it")),
                CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE,
                List.of(new Attachment("Plan.pdf", 100)));
    }

    @Test
    void cleanInternalMailHasNoViolations() {
        assertTrue(checker.violations(cleanInternal(), 5).isEmpty());
        assertFalse(checker.isPhishing(cleanInternal(), 5));
    }

    @Test
    void lookalikeSenderDomainIsViolation() {
        Email e = mail("markus.brandt@kornplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE, List.of());
        assertEquals(List.of(Feature.ABSENDER), checker.violations(e, 1));

        Email appended = mail("it@komplimento.de.secure-mail.net", List.of(), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE, List.of());
        assertTrue(checker.isViolated(Feature.ABSENDER, appended));

        Email subdomain = mail("it@mail.komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE, List.of());
        assertFalse(checker.isViolated(Feature.ABSENDER, subdomain));
    }

    @Test
    void linkMismatchOnlyCountsOnceLinkRuleIsActive() {
        Email e = mail("markus.brandt@komplimento.de",
                List.of(new EmailLink("https://komplimento.de/login", "http://k0mplimento-login.ru/konto")),
                CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE, List.of());
        assertTrue(checker.violations(e, 1).isEmpty(), "Tag 1: Link-Regel noch nicht aktiv");
        assertEquals(List.of(Feature.LINK), checker.violations(e, 2));
    }

    @Test
    void wrongEmployeeCodeIsSignatureViolation() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO,
                new Signature("Markus Brandt", "IT-Sicherheit", "X0U3A25D"), List.of());
        assertEquals(List.of(Feature.SIGNATUR), checker.violations(e, 4));
        assertTrue(checker.explain(Feature.SIGNATUR, e).contains("Q7M2H9KL"));
    }

    @Test
    void missingSignatureOnInternalMailIsViolation() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO, Signature.NONE, List.of());
        assertTrue(checker.isViolated(Feature.SIGNATUR, e));
    }

    @Test
    void doubleExtensionAttachmentIsViolation() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE,
                List.of(new Attachment("Rechnung.pdf.exe", 300)));
        assertTrue(checker.violations(e, 4).isEmpty(), "Tag 4: Anhang-Regel noch nicht aktiv");
        assertEquals(List.of(Feature.ANHANG), checker.violations(e, 5));
    }

    @Test
    void alteredLogoIsViolation() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), new LogoSpec("#1F5FBF", "K", false, "circle"),
                VALID_SIGNATURE, List.of());
        assertEquals(List.of(Feature.LOGO), checker.violations(e, 3));
    }

    @Test
    void partnerMailIsNotCheckedForLogoAndSignature() {
        Email e = mail("buchhaltung@nordlicht-logistik.de",
                List.of(new EmailLink("Portal", "https://www.nordlicht-logistik.de/portal")),
                new LogoSpec("#2E7D32", "N", false, "square"),
                new Signature("Petra Nordmann", "Buchhaltung", ""),
                List.of(new Attachment("Rechnung.pdf", 80)));
        assertTrue(checker.violations(e, 5).isEmpty());
        assertTrue(checker.isPartnerMail(e));
    }

    @Test
    void userInfoTrickInLinkIsDetectedAndExplained() {
        EmailLink link = new EmailLink("https://intranet.komplimento.de/login",
                "https://komplimento.de@login-check.net/login");
        assertEquals("login-check.net", link.host());
        Email e = mail("markus.brandt@komplimento.de", List.of(link), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE, List.of());
        assertEquals(List.of(Feature.LINK), checker.violations(e, 2));
        assertTrue(checker.explain(Feature.LINK, e).contains("@"));
    }

    @Test
    void partiallyWrongCodeNamesTheDifferingPosition() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO,
                new Signature("Markus Brandt", "IT-Sicherheit", "Q7M2H9LK"), List.of());
        String text = checker.explain(Feature.SIGNATUR, e);
        assertTrue(text.contains("Stellen 7 (L statt K) und 8 (K statt L)"), text);
        assertTrue(text.contains("Q7M2H9KL"), text);
    }

    @Test
    void misspelledNamePointsToRealEmployee() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO,
                new Signature("Markus Brand", "IT-Sicherheit", "Q7M2H9KL"), List.of());
        assertTrue(checker.isViolated(Feature.SIGNATUR, e));
        assertTrue(checker.explain(Feature.SIGNATUR, e).contains("Markus Brandt"));
    }

    @Test
    void logoWithoutStarIsExplainedPrecisely() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), new LogoSpec("#1F5FBF", "K", false, "circle"),
                VALID_SIGNATURE, List.of());
        assertEquals("Das Logo weicht vom offiziellen Logo ab: der goldene Stern fehlt.", checker.explain(Feature.LOGO, e));
    }

    @Test
    void macroAttachmentIsViolation() {
        Email e = mail("markus.brandt@komplimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE,
                List.of(new Attachment("Budget_Q1.xlsm", 80)));
        assertEquals(List.of(Feature.ANHANG), checker.violations(e, 5));
        assertTrue(checker.explain(Feature.ANHANG, e).contains("Makros"));
    }

    @Test
    void lookalikeSenderGetsHelpfulHint() {
        Email e = mail("it@kompIimento.de", List.of(), CorporateGuidelines.OFFICIAL_LOGO, VALID_SIGNATURE, List.of());
        assertTrue(checker.isViolated(Feature.ABSENDER, e));
        assertTrue(checker.explain(Feature.ABSENDER, e).contains("sieht nur so aus wie \"komplimento.de\""));
    }

    @Test
    void fakedPartnerSenderOnlyViolatesSender() {
        Email e = new Email("x", "Nordlicht Logistik – Buchhaltung", "buchhaltung@nordlicht-logistlk.de",
                "Nordlicht Logistik GmbH", "Neue Bankverbindung", "Text", List.of(),
                new LogoSpec("#2E7D32", "N", false, "square"),
                new Signature("Petra Nordmann", "Buchhaltung", ""), List.of(new Attachment("Konto.pdf", 50)), "09:00");
        assertEquals(List.of(Feature.ABSENDER), checker.violations(e, 5));
    }

    @Test
    void multipleViolationsAreAllReported() {
        Email e = mail("it@k0mplimento-login.ru",
                List.of(new EmailLink("Hier", "http://185.23.44.17/login")),
                LogoSpec.NONE, Signature.NONE,
                List.of(new Attachment("Update.exe", 900)));
        assertEquals(List.of(Feature.ABSENDER, Feature.LINK, Feature.LOGO, Feature.SIGNATUR, Feature.ANHANG),
                checker.violations(e, 5));
    }
}
