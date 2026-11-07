package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Attachment;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.EmailLink;
import com.example.phishingforcompliments.model.Employee;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.LogoSpec;
import com.example.phishingforcompliments.model.Signature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prüft eine E-Mail gegen die {@link CorporateGuidelines}. Hier entsteht die Wahrheit,
 * ob eine Mail Phishing ist – nicht im Generator. Es werden nur Merkmale geprüft,
 * deren Regel am jeweiligen Tag bereits eingeführt wurde.
 */
public final class RuleChecker {

    private static final Set<String> FREEMAIL = Set.of("gmail.com", "web.de", "outlook.com", "gmx.net", "yahoo.com");
    private static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl.com", "t.ly", "goo.gl");
    private static final Set<String> EXECUTABLES = Set.of("exe", "bat", "msi", "scr", "lnk", "com", "cmd");
    private static final Set<String> SCRIPTS = Set.of("js", "vbs", "ps1");
    private static final Set<String> MACROS = Set.of("docm", "xlsm", "pptm");
    private static final Set<String> CONTAINERS = Set.of("zip", "rar", "7z", "iso", "img");

    /** Alle verletzten, am Tag {@code day} aktiven Merkmale. */
    public List<Feature> violations(Email email, int day) {
        return Feature.activeFeatures(day).stream().filter(f -> isViolated(f, email)).toList();
    }

    public boolean isPhishing(Email email, int day) {
        return !violations(email, day).isEmpty();
    }

    /**
     * Mails im Namen von Partnerfirmen werden nicht auf Logo und Signatur geprüft. Entscheidend ist,
     * als wen sich die Mail ausgibt – eine gefälschte Partner-Mail verstößt "nur" beim Absender.
     */
    public boolean isPartnerMail(Email email) {
        return !CorporateGuidelines.COMPANY_NAME.equals(email.senderCompany());
    }

    public boolean isViolated(Feature feature, Email email) {
        return switch (feature) {
            case ABSENDER -> !CorporateGuidelines.isTrustedDomain(email.senderDomain());
            case LINK -> email.links().stream().anyMatch(l -> !CorporateGuidelines.isTrustedDomain(l.host()));
            case LOGO -> !isPartnerMail(email) && !CorporateGuidelines.OFFICIAL_LOGO.equals(email.logo());
            case SIGNATUR -> !isPartnerMail(email) && !isValidSignature(email.signature());
            case ANHANG -> email.attachments().stream().anyMatch(a -> !CorporateGuidelines.isAllowedAttachment(a));
        };
    }

    public boolean isValidSignature(Signature s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        Optional<Employee> emp = CorporateGuidelines.findByName(s.name());
        return emp.isPresent()
                && emp.get().code().equalsIgnoreCase(s.code().trim())
                && emp.get().department().equalsIgnoreCase(s.department().trim());
    }

    // ---------------------------------------------------------------------
    // Texte für die Vergleichsansicht (UC-07: "Der Vergleich soll graphisch dargestellt werden")
    // ---------------------------------------------------------------------

    /** Was die E-Mail bei diesem Merkmal zeigt. */
    public String describeActual(Feature feature, Email email) {
        return switch (feature) {
            case ABSENDER -> email.senderName() + "\n<" + email.senderAddress() + ">\nDomain: " + email.senderDomain();
            case LINK -> email.links().isEmpty()
                    ? "Die Mail enthält keine Links."
                    : email.links().stream()
                        .map(l -> "Angezeigt: \"" + l.displayText() + "\"\nZiel:      " + l.url() + "\nZiel-Domain: " + l.host())
                        .collect(Collectors.joining("\n\n"));
            case LOGO -> email.logo().describe();
            case SIGNATUR -> email.signature().isEmpty()
                    ? "Keine Signatur vorhanden."
                    : email.signature().name() + "\n" + email.signature().department() + "\nCode: "
                        + (email.signature().code().isBlank() ? "—" : email.signature().code());
            case ANHANG -> email.attachments().isEmpty()
                    ? "Keine Anhänge."
                    : email.attachments().stream()
                        .map(a -> a.fileName() + " (" + a.sizeKb() + " KB)")
                        .collect(Collectors.joining("\n"));
        };
    }

    /** Was laut Corporate Guidelines bei diesem Merkmal zu erwarten ist. */
    public String describeExpected(Feature feature, Email email) {
        return switch (feature) {
            case ABSENDER -> "Adresse endet auf @" + CorporateGuidelines.COMPANY_DOMAIN
                    + " (oder eine Subdomain davon)\noder auf eine Partnerdomain:\n"
                    + String.join(", ", CorporateGuidelines.PARTNER_DOMAINS);
            case LINK -> "Jedes Link-Ziel liegt auf " + CorporateGuidelines.COMPANY_DOMAIN
                    + " oder einer Partnerdomain.\nAchtung: Der angezeigte Text kann vom Ziel abweichen –"
                    + " entscheidend ist die Ziel-Domain.";
            case LOGO -> isPartnerMail(email)
                    ? "Partnerfirmen verwenden ihr eigenes Logo – keine Prüfung."
                    : "Offizielles Logo:\n" + CorporateGuidelines.OFFICIAL_LOGO.describe();
            case SIGNATUR -> {
                if (isPartnerMail(email)) {
                    yield "Partnerfirmen: keine Signaturpflicht.";
                }
                Optional<Employee> emp = CorporateGuidelines.findByName(email.signature().name());
                yield emp.map(e -> "Eintrag im Mitarbeiterverzeichnis:\n" + e.name() + "\n" + e.department() + "\nCode: " + e.code())
                        .orElse("Name, Abteilung und Code müssen exakt\neinem Eintrag im Mitarbeiterverzeichnis entsprechen.");
            }
            case ANHANG -> "Erlaubte Dateitypen: "
                    + CorporateGuidelines.ALLOWED_ATTACHMENT_EXTENSIONS.stream().map(e -> "." + e).collect(Collectors.joining(", "))
                    + "\nKeine Programme, Skripte, Archive, Makro-Dateien\noder Doppel-Endungen (z.B. .pdf.exe).";
        };
    }

    /** Begründung, warum das Merkmal verletzt ist bzw. in Ordnung ist. */
    public String explain(Feature feature, Email email) {
        boolean violated = isViolated(feature, email);
        return switch (feature) {
            case ABSENDER -> violated
                    ? "Die Absenderdomain \"" + email.senderDomain() + "\" gehört weder zur "
                        + CorporateGuidelines.COMPANY_NAME + " noch zu einem Partner. " + domainHint(email.senderDomain())
                    : "Die Absenderdomain \"" + email.senderDomain() + "\" ist vertrauenswürdig.";
            case LINK -> {
                Optional<EmailLink> bad = email.links().stream()
                        .filter(l -> !CorporateGuidelines.isTrustedDomain(l.host())).findFirst();
                yield bad.map(this::explainLink)
                        .orElse(email.links().isEmpty() ? "Die Mail enthält keine Links."
                                : "Alle Links zeigen auf vertrauenswürdige Domains.");
            }
            case LOGO -> violated
                    ? "Das Logo weicht vom offiziellen Logo ab: " + String.join(", ", logoDifferences(email.logo())) + "."
                    : isPartnerMail(email) ? "Partnerfirma – das Logo wird nicht geprüft."
                        : "Das Logo entspricht dem offiziellen Firmenlogo.";
            case SIGNATUR -> violated ? explainSignature(email.signature())
                    : isPartnerMail(email) ? "Partnerfirma – keine Signaturpflicht."
                        : "Name, Abteilung und Code stimmen mit dem Mitarbeiterverzeichnis überein.";
            case ANHANG -> {
                Optional<Attachment> bad = email.attachments().stream()
                        .filter(a -> !CorporateGuidelines.isAllowedAttachment(a)).findFirst();
                yield bad.map(RuleChecker::explainAttachment)
                        .orElse(email.attachments().isEmpty() ? "Die Mail hat keine Anhänge."
                                : "Alle Anhänge haben erlaubte Dateitypen.");
            }
        };
    }

    // ------------------------------------------------------------------
    // Erklärungen im Detail (Lerneffekt)
    // ------------------------------------------------------------------

    private static List<String> trustedDomains() {
        return Stream.concat(Stream.of(CorporateGuidelines.COMPANY_DOMAIN), CorporateGuidelines.PARTNER_DOMAINS.stream()).toList();
    }

    /** Erklärt, welcher Trick hinter einer gefälschten Domain steckt. */
    static String domainHint(String domain) {
        String d = domain.toLowerCase();
        if (FREEMAIL.contains(d)) {
            return "Das ist ein Freemail-Anbieter – Firmen-Mails kommen nie von dort.";
        }
        for (String trusted : trustedDomains()) {
            if (d.contains(trusted + ".")) {
                return "Sie enthält zwar \"" + trusted + "\", entscheidend ist aber das Ende der Domain.";
            }
        }
        if (d.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return "Statt einer Domain steht dort nur eine IP-Adresse.";
        }
        String closest = trustedDomains().stream()
                .min(Comparator.comparingInt(t -> levenshtein(d, t))).orElseThrow();
        String stem = closest.substring(0, closest.lastIndexOf('.'));
        if (levenshtein(d, closest) <= 3 || d.contains(stem)) {
            return "Sie sieht nur so aus wie \"" + closest + "\" – achte auf vertauschte, fehlende oder doppelte "
                    + "Buchstaben (rn/m, I/l, 0/o), Bindestriche oder eine andere Endung.";
        }
        return "Unbekannte Domains sind immer verdächtig.";
    }

    private String explainLink(EmailLink l) {
        String host = l.host();
        String prefix = "Der Link \"" + l.displayText() + "\" führt in Wahrheit auf \"" + host + "\". ";
        if (l.hasUserInfo()) {
            return prefix + "Trick: Alles vor dem @ in der Adresse wird vom Browser ignoriert.";
        }
        if (SHORTENERS.contains(host)) {
            return prefix + "Kurz-Links verschleiern das echte Ziel und sind deshalb nicht erlaubt.";
        }
        if (l.displayText().startsWith("http")) {
            EmailLink shown = new EmailLink(l.displayText(), l.displayText());
            if (!shown.host().equals(host)) {
                return prefix + "Der angezeigte Text täuscht eine andere Adresse vor. " + domainHint(host);
            }
        }
        return prefix + domainHint(host);
    }

    static List<String> logoDifferences(LogoSpec logo) {
        LogoSpec o = CorporateGuidelines.OFFICIAL_LOGO;
        List<String> diffs = new ArrayList<>();
        if (logo == null || logo.isNone()) {
            diffs.add("das Firmenlogo fehlt komplett");
            return diffs;
        }
        if (!o.shape().equals(logo.shape())) {
            diffs.add("Form ist ein " + ("square".equals(logo.shape()) ? "Quadrat" : logo.shape()) + " statt eines Kreises");
        }
        if (!o.colorHex().equalsIgnoreCase(logo.colorHex())) {
            diffs.add("Farbe " + logo.colorHex() + " statt " + o.colorHex());
        }
        if (!o.letter().equals(logo.letter())) {
            diffs.add("Buchstabe \"" + logo.letter() + "\" statt \"" + o.letter() + "\"");
        }
        if (o.star() && !logo.star()) {
            diffs.add("der goldene Stern fehlt");
        }
        return diffs;
    }

    private static String explainSignature(Signature s) {
        if (s.isEmpty()) {
            return "Interne Mails müssen immer mit Name, Abteilung und Mitarbeiter-Code enden.";
        }
        Optional<Employee> emp = CorporateGuidelines.findByName(s.name());
        if (emp.isEmpty()) {
            Optional<Employee> similar = CorporateGuidelines.EMPLOYEES.stream()
                    .filter(e -> levenshtein(e.name().toLowerCase(), s.name().toLowerCase()) <= 3)
                    .findFirst();
            return similar.map(e -> "\"" + s.name() + "\" steht so nicht im Mitarbeiterverzeichnis – es gibt nur \""
                            + e.name() + "\". Auch ein Tippfehler im Namen ist ein Warnsignal.")
                    .orElse("\"" + s.name() + "\" steht nicht im Mitarbeiterverzeichnis.");
        }
        Employee e = emp.get();
        String code = s.code().trim();
        if (code.isEmpty()) {
            return "Der Mitarbeiter-Code fehlt – interne Mails enthalten ihn immer.";
        }
        if (!e.code().equalsIgnoreCase(code)) {
            Optional<Employee> owner = CorporateGuidelines.findByCode(code);
            if (owner.isPresent()) {
                return "Der Code \"" + code + "\" gehört zu " + owner.get().name() + ", nicht zu " + e.name()
                        + " (richtig wäre " + e.code() + ").";
            }
            if (code.length() != e.code().length()) {
                return "Der Code \"" + code + "\" hat " + code.length() + " statt " + e.code().length()
                        + " Zeichen – richtig wäre " + e.code() + ".";
            }
            return "Der Code \"" + code + "\" weicht an " + differingPositions(code, e.code())
                    + " vom Verzeichnis ab – richtig wäre " + e.code() + ".";
        }
        return "Die Abteilung \"" + s.department() + "\" passt nicht zu " + e.name()
                + " (" + e.department() + ").";
    }

    /** z.B. "Stelle 2 (O statt 0)" oder "Stelle 5 und 6 (… )". */
    static String differingPositions(String actual, String expected) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < Math.min(actual.length(), expected.length()); i++) {
            if (Character.toUpperCase(actual.charAt(i)) != Character.toUpperCase(expected.charAt(i))) {
                parts.add((i + 1) + " (" + actual.charAt(i) + " statt " + expected.charAt(i) + ")");
            }
        }
        return (parts.size() == 1 ? "Stelle " : "Stellen ") + String.join(" und ", parts);
    }

    private static String explainAttachment(Attachment a) {
        String ext = a.lastExtension();
        String name = "\"" + a.fileName() + "\": ";
        if (a.hasDoubleExtension()) {
            return name + "Doppel-Endung – nur die letzte Endung \"." + ext + "\" zählt, die vordere soll täuschen.";
        }
        if (MACROS.contains(ext)) {
            return name + "Office-Dateien mit Makros (." + ext + ") können Schadcode ausführen.";
        }
        if (EXECUTABLES.contains(ext) || SCRIPTS.contains(ext)) {
            return name + "\"." + ext + "\" ist ein Programm bzw. Skript und wird beim Öffnen ausgeführt.";
        }
        if (CONTAINERS.contains(ext)) {
            return name + "Archive und Disk-Images (." + ext + ") können Schadsoftware verstecken.";
        }
        if (ext.startsWith("htm")) {
            return name + "HTML-Anhänge öffnen oft gefälschte Login-Seiten.";
        }
        return name + "Dateityp \"." + ext + "\" ist nicht erlaubt.";
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }
}
