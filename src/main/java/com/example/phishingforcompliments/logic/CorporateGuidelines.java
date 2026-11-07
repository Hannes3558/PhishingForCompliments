package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Attachment;
import com.example.phishingforcompliments.model.Employee;
import com.example.phishingforcompliments.model.LogoSpec;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Die "Corporate Guidelines" der fiktiven Komplimento AG. Einzige Quelle der Wahrheit
 * dafür, was in einer E-Mail korrekt ist. Wird sowohl vom {@link RuleChecker} als auch
 * von der Guidelines-Ansicht im Spiel benutzt.
 */
public final class CorporateGuidelines {

    public static final String COMPANY_NAME = "Komplimento AG";
    public static final String COMPANY_DOMAIN = "komplimento.de";
    public static final List<String> PARTNER_DOMAINS = List.of("nordlicht-logistik.de", "bueroprofi24.de", "fernweh-reisen.de");
    public static final Map<String, String> PARTNER_NAMES = Map.of(
            "nordlicht-logistik.de", "Nordlicht Logistik GmbH",
            "bueroprofi24.de", "Büroprofi24 GmbH",
            "fernweh-reisen.de", "Fernweh Reisen GmbH");
    public static final LogoSpec OFFICIAL_LOGO = new LogoSpec("#1F5FBF", "K", true, "circle");
    public static final List<String> ALLOWED_ATTACHMENT_EXTENSIONS = List.of("pdf", "docx", "xlsx", "png");

    /**
     * Mitarbeiterverzeichnis. Bewusst gemischte Namen in ALLEN Abteilungen und Ebenen –
     * die Herkunft eines Namens sagt nichts über die Stelle aus.
     */
    public static final List<Employee> EMPLOYEES = List.of(
            employee("Dr. Sabine Vogt", "Geschäftsführung", "X0U3A25D", "sabine.vogt"),
            employee("Kemal Aydın", "Geschäftsführung", "G4H8N2TQ", "kemal.aydin"),
            employee("Aylin Demir", "IT-Sicherheit", "A6Y1D8RW", "aylin.demir"),
            employee("Markus Brandt", "IT-Sicherheit", "Q7M2H9KL", "markus.brandt"),
            employee("Karin Nowak", "IT-Sicherheit", "N3W5P0KT", "karin.nowak"),
            employee("Linh Nguyen", "IT-Support", "L7N4G2VX", "linh.nguyen"),
            employee("Felix Schröder", "IT-Support", "F9S2R6BH", "felix.schroeder"),
            employee("Samuel Mensah", "Personalabteilung", "S8M3H5QD", "samuel.mensah"),
            employee("Lena Hoffmann", "Personalabteilung", "P4R8T1ZN", "lena.hoffmann"),
            employee("Dieter Müller", "Personalabteilung", "E2K9W4LM", "Dieter.Müller"),
            employee("Yusuf Çelik", "Buchhaltung", "Y3C8L6KT", "yusuf.celik"),
            employee("Tobias Keller", "Buchhaltung", "B9F3W6CX", "tobias.keller"),
            employee("Elena Popescu", "Buchhaltung", "E5P7C1RN", "elena.popescu"),
            employee("Sofia Rossi", "Marketing", "R1S4F9MV", "sofia.rossi"),
            employee("Jonas Weber", "Marketing", "M5D1G8RV", "jonas.weber"),
            employee("Marcel Schneider", "Vertrieb", "D6P2S8XA", "marcel.schneider"),
            employee("Hannah Wagner", "Vertrieb", "H2W7G3NP", "hannah.wagner"),
            employee("Nadia Haddad", "Einkauf", "N9H4D1JC", "nadia.haddad"),
            employee("Olga Becker", "Einkauf", "O8B5K2ZD", "olga.becker"),
            employee("Stefan Krüger", "Kantine & Facility", "K3T8M5QW", "stefan.krueger"),
            employee("Julia Neumann", "Kantine & Facility", "J6N2U9FA", "julia.neumann"),
            employee("Dr. Nikolai Petrov", "Recht & Compliance", "C7R4P1VY", "nikolai.petrov"),
            employee("Clara Fischer", "Recht & Compliance", "C2F8H3SX", "clara.fischer"));

    /** Die Chefin, von der die täglichen Anweisungen kommen. */
    public static final Employee BOSS = EMPLOYEES.getFirst();

    private static Employee employee(String name, String department, String code, String mailbox) {
        return new Employee(name, department, code, normalizeMailbox(mailbox) + "@" + COMPANY_DOMAIN);
    }

    /**
     * Firmen-Postfächer sind immer klein und ASCII ("Dieter.Müller" → "dieter.mueller"),
     * sonst sähe eine echte Adresse im Spiel wie ein Fälschungstrick aus.
     */
    static String normalizeMailbox(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replace('ı', 'i');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // ç → c, é → e
        return s.replaceAll("[^a-z0-9.\\-]", "");
    }

    public static List<String> departments() {
        return EMPLOYEES.stream().map(Employee::department).distinct().toList();
    }

    private CorporateGuidelines() {
    }

    public static boolean isCompanyDomain(String domain) {
        String d = domain.toLowerCase();
        return d.equals(COMPANY_DOMAIN) || d.endsWith("." + COMPANY_DOMAIN);
    }

    public static boolean isPartnerDomain(String domain) {
        String d = domain.toLowerCase();
        return PARTNER_DOMAINS.stream().anyMatch(p -> d.equals(p) || d.endsWith("." + p));
    }

    /** Firmenname zu einer echten Domain (Komplimento AG oder Partner). */
    public static String companyForDomain(String domain) {
        String d = domain.toLowerCase();
        return PARTNER_DOMAINS.stream().filter(p -> d.equals(p) || d.endsWith("." + p))
                .map(PARTNER_NAMES::get).findFirst().orElse(COMPANY_NAME);
    }

    public static boolean isTrustedDomain(String domain) {
        return isCompanyDomain(domain) || isPartnerDomain(domain);
    }

    public static boolean isAllowedAttachment(Attachment attachment) {
        return !attachment.hasDoubleExtension()
                && ALLOWED_ATTACHMENT_EXTENSIONS.contains(attachment.lastExtension());
    }

    public static Optional<Employee> findByName(String name) {
        return EMPLOYEES.stream().filter(e -> e.name().equalsIgnoreCase(name.trim())).findFirst();
    }

    public static Optional<Employee> findByCode(String code) {
        return EMPLOYEES.stream().filter(e -> e.code().equalsIgnoreCase(code.trim())).findFirst();
    }

    public static List<Employee> byDepartment(String department) {
        return EMPLOYEES.stream().filter(e -> e.department().equalsIgnoreCase(department)).toList();
    }
}
