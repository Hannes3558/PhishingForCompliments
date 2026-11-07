package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.logic.MailTemplates.Template;
import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Employee;
import com.example.phishingforcompliments.model.GameState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailTemplatesTest {

    private static List<Template> all() {
        List<Template> all = new ArrayList<>();
        all.addAll(MailTemplates.LEGIT_INTERNAL);
        all.addAll(MailTemplates.LEGIT_PARTNER);
        all.addAll(MailTemplates.LURES);
        all.addAll(MailTemplates.PARTNER_LURES);
        return all;
    }

    private static int count(String text, String token) {
        return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    @Test
    void keysAreUnique() {
        Set<String> keys = new HashSet<>();
        for (Template t : all()) {
            assertTrue(keys.add(t.key()), "doppelter Schlüssel " + t.key());
        }
    }

    @Test
    void linkPlaceholderMatchesLink() {
        for (Template t : all()) {
            int expected = t.hasLink() ? 1 : 0;
            assertEquals(expected, count(t.body(), "{link}"), "{link} in " + t.key());
            if (t.sloppyBody() != null) {
                assertEquals(expected, count(t.sloppyBody(), "{link}"), "{link} im schlampigen Text von " + t.key());
            }
        }
    }

    @Test
    void internalTemplatesAddressThePlayerAndUseExistingDepartments() {
        List<Template> internal = new ArrayList<>(MailTemplates.LEGIT_INTERNAL);
        internal.addAll(MailTemplates.LURES);
        for (Template t : internal) {
            assertFalse(t.isPartner(), t.key());
            assertTrue(t.body().contains("{player}"), "{player} fehlt in " + t.key());
            if (t.sloppyBody() != null) {
                assertTrue(t.sloppyBody().contains("{player}"), "{player} fehlt im schlampigen Text von " + t.key());
            }
            assertFalse(CorporateGuidelines.byDepartment(t.department()).isEmpty(),
                    "Abteilung \"" + t.department() + "\" aus " + t.key() + " gibt es nicht");
        }
    }

    @Test
    void everyDepartmentHasHarmlessAndPhishingTemplates() {
        for (String dept : CorporateGuidelines.departments()) {
            assertTrue(MailTemplates.LEGIT_INTERNAL.stream().anyMatch(t -> dept.equals(t.department())),
                    "keine harmlose Vorlage für " + dept);
        }
        long lureDepartments = MailTemplates.LURES.stream().map(Template::department).distinct().count();
        assertTrue(lureDepartments >= 7, "Köder sollen aus vielen Abteilungen kommen: " + lureDepartments);
    }

    @Test
    void everyPartnerHasHarmlessAndPhishingTemplates() {
        for (String domain : CorporateGuidelines.PARTNER_DOMAINS) {
            assertTrue(MailTemplates.LEGIT_PARTNER.stream().anyMatch(t -> domain.equals(t.partnerDomain())), domain);
            assertTrue(MailTemplates.PARTNER_LURES.stream().anyMatch(t -> domain.equals(t.partnerDomain())), domain);
        }
    }

    @Test
    void directoryIsConsistent() {
        Set<String> codes = new HashSet<>();
        Set<String> mails = new HashSet<>();
        for (Employee e : CorporateGuidelines.EMPLOYEES) {
            assertTrue(e.code().matches("[A-Z0-9]{8}"), "Code-Format " + e);
            assertTrue(codes.add(e.code()), "doppelter Code " + e.code());
            assertTrue(mails.add(e.email()), "doppelte Mailadresse " + e.email());
            assertTrue(e.email().matches("[a-z.]+@komplimento\\.de"), "Mailadresse nur ASCII: " + e.email());
        }
        assertTrue(CorporateGuidelines.EMPLOYEES.size() >= 20);
        for (String dept : CorporateGuidelines.departments()) {
            assertTrue(CorporateGuidelines.byDepartment(dept).size() >= 2, "mindestens 2 Personen in " + dept);
        }
    }

    @Test
    void mailboxesAreNormalized() {
        assertEquals("dieter.mueller", CorporateGuidelines.normalizeMailbox("Dieter.Müller"));
        assertEquals("yusuf.celik", CorporateGuidelines.normalizeMailbox("Yusuf.Çelik"));
        assertEquals("kemal.aydin", CorporateGuidelines.normalizeMailbox("kemal.aydın"));
        assertEquals("gross.strasse", CorporateGuidelines.normalizeMailbox(" Gross.Straße "));
    }

    @Test
    void personalMailsComeFromTheSignersMailbox() {
        MailGenerator generator = new MailGenerator();
        RuleChecker checker = new RuleChecker();
        Map<String, Employee> byName = CorporateGuidelines.EMPLOYEES.stream()
                .collect(Collectors.toMap(Employee::name, Function.identity()));
        Set<String> functionMailboxes = Set.of("it-support", "postmaster", "dokumente", "voicemail",
                "security", "soc", "poststelle");
        for (long seed = 1; seed <= 20; seed++) {
            for (Email m : generator.generate(Difficulty.MITTEL, 5, seed, "Alex")) {
                if (checker.isPhishing(m, 5) || checker.isPartnerMail(m)) {
                    continue;
                }
                String local = m.senderAddress().substring(0, m.senderAddress().indexOf('@'));
                if (!functionMailboxes.contains(local)) {
                    assertEquals(byName.get(m.signature().name()).email(), m.senderAddress(),
                            "Absender passt nicht zur Unterschrift: " + m.subject());
                }
            }
        }
    }

    @Test
    void allEmployeesActuallyShowUpInTheGame() {
        MailGenerator generator = new MailGenerator();
        Set<String> seen = new HashSet<>();
        for (Difficulty d : Difficulty.values()) {
            for (int day = 1; day <= GameState.CAMPAIGN_DAYS; day++) {
                for (long seed = 1; seed <= 30; seed++) {
                    generator.generate(d, day, seed, "Alex").forEach(m -> seen.add(m.signature().name()));
                }
            }
        }
        for (Employee e : CorporateGuidelines.EMPLOYEES) {
            assertTrue(seen.contains(e.name()), e.name() + " taucht nie als Absender auf");
        }
    }
}
