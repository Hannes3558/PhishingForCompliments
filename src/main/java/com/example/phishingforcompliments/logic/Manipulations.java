package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Employee;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.LogoSpec;
import com.example.phishingforcompliments.model.Signature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Katalog aller Fehlerarten, die der {@link MailGenerator} in Mails einbauen kann.
 * Jede Variante verfälscht genau ein Merkmal und lässt alle anderen unberührt
 * ({@code ManipulationsTest} prüft das für jede Variante und jede Vorlage).
 *
 * Neue Fehlerart: einfach einen weiteren {@link Variant}-Eintrag in {@link #ALL} ergänzen.
 */
final class Manipulations {

    /**
     * @param feature      das verfälschte Merkmal
     * @param name         kurze Beschreibung (für Tests/Debugging)
     * @param subtle       true = schwer zu erkennen, false = deutlich
     * @param internalOnly nur für Mails im Namen der Komplimento AG (Logo/Signatur gibt es bei Partnern nicht)
     * @param apply        die eigentliche Verfälschung
     */
    record Variant(Feature feature, String name, boolean subtle, boolean internalOnly,
                   BiConsumer<MailDraft, Random> apply) {
    }

    private static final List<String> FREEMAIL = List.of("gmail.com", "web.de", "outlook.com", "gmx.net");
    private static final List<String> FOREIGN_TLDS = List.of("com", "net", "eu", "co", "info");
    private static final List<String> APPENDED = List.of("mail-service.net", "secure-login.com", "cloud-mail.eu");
    private static final List<String> SUSPICIOUS_HOSTS = List.of(
            "secure-mail-service.biz", "account-verify.ru", "support-team.xyz", "prize-center.top");
    private static final List<String> SHORTENERS = List.of("bit.ly", "tinyurl.com", "t.ly");
    private static final List<String> OTHER_BLUES = List.of("#2F80ED", "#163F80", "#1F8FBF");

    private static final String[][] LOOKALIKE_SWAPS = {
            {"m", "rn"}, {"rn", "m"}, {"l", "I"}, {"i", "l"}, {"o", "0"}, {"w", "vv"}, {"cl", "d"}};

    static final List<Variant> ALL = List.of(
            // ---------------- Absender ----------------
            new Variant(Feature.ABSENDER, "ähnlich aussehende Domain", true, false,
                    (d, r) -> d.setSenderDomain(lookalike(d.stem(), r) + ".de")),
            new Variant(Feature.ABSENDER, "andere Endung", true, false,
                    (d, r) -> d.setSenderDomain(d.stem() + "." + pick(FOREIGN_TLDS, r))),
            new Variant(Feature.ABSENDER, "echte Domain mit angehängter Fremd-Domain", true, false,
                    (d, r) -> d.setSenderDomain(d.baseDomain() + "." + pick(APPENDED, r))),
            new Variant(Feature.ABSENDER, "Domain mit Bindestrich-Zusatz", true, false,
                    (d, r) -> d.setSenderDomain(r.nextBoolean()
                            ? pick(List.of("mail-", "secure-", "service-"), r) + d.stem() + ".de"
                            : d.stem() + "-de.com")),
            new Variant(Feature.ABSENDER, "Freemail-Adresse", false, false,
                    (d, r) -> d.address = d.localPart.replace('-', '.') + "." + d.stem().replace("-", "")
                            + "@" + pick(FREEMAIL, r)),
            new Variant(Feature.ABSENDER, "völlig fremde Domain", false, false,
                    (d, r) -> d.setSenderDomain(pick(SUSPICIOUS_HOSTS, r))),
            new Variant(Feature.ABSENDER, "plumpe Fälschung mit Länderendung", false, false,
                    (d, r) -> d.setSenderDomain(d.stem().replace('o', '0') + "-login." + pick(List.of("ru", "cn", "xyz"), r))),

            // ---------------- Link ----------------
            new Variant(Feature.LINK, "ähnlich aussehende Domain", true, false,
                    (d, r) -> setLink(d, r, "https://" + d.linkSubdomain() + lookalike(d.stem(), r) + ".de/" + d.linkPath(), true)),
            new Variant(Feature.LINK, "andere Endung", true, false,
                    (d, r) -> setLink(d, r, "https://" + d.linkSubdomain() + d.stem() + "." + pick(FOREIGN_TLDS, r) + "/" + d.linkPath(), true)),
            new Variant(Feature.LINK, "echte Domain als Subdomain einer Fremd-Domain", true, false,
                    (d, r) -> setLink(d, r, "https://" + d.linkSubdomain() + d.baseDomain() + "." + pick(APPENDED, r) + "/" + d.linkPath(), true)),
            new Variant(Feature.LINK, "@-Trick (alles davor wird ignoriert)", true, false,
                    (d, r) -> setLink(d, r, "https://" + d.baseDomain() + "@" + pick(List.of("login-check.net", "docs-share.eu"), r)
                            + "/" + d.linkPath(), true)),
            new Variant(Feature.LINK, "Kurz-Link verschleiert das Ziel", true, false,
                    (d, r) -> setLink(d, r, "https://" + pick(SHORTENERS, r) + "/" + randomToken(r, 6), false)),
            new Variant(Feature.LINK, "nackte IP-Adresse", false, false,
                    (d, r) -> setLink(d, r, "http://" + (80 + r.nextInt(120)) + "." + r.nextInt(256) + "."
                            + r.nextInt(256) + "." + (1 + r.nextInt(254)) + "/login", true)),
            new Variant(Feature.LINK, "völlig fremde Domain", false, false,
                    (d, r) -> setLink(d, r, "http://" + pick(SUSPICIOUS_HOSTS, r) + "/" + d.linkPath(), true)),
            new Variant(Feature.LINK, "plumpe Fälschung mit Länderendung", false, false,
                    (d, r) -> setLink(d, r, "http://" + d.stem().replace('o', '0') + "-login." + pick(List.of("ru", "cn", "xyz"), r)
                            + "/konto", true)),

            // ---------------- Logo ----------------
            new Variant(Feature.LOGO, "Stern fehlt", true, true,
                    (d, r) -> d.logo = official(o -> new LogoSpec(o.colorHex(), o.letter(), false, o.shape()))),
            new Variant(Feature.LOGO, "Quadrat statt Kreis", true, true,
                    (d, r) -> d.logo = official(o -> new LogoSpec(o.colorHex(), o.letter(), o.star(), "square"))),
            new Variant(Feature.LOGO, "anderer Blauton", true, true,
                    (d, r) -> d.logo = official(o -> new LogoSpec(pick(OTHER_BLUES, r), o.letter(), o.star(), o.shape()))),
            new Variant(Feature.LOGO, "kleines k", true, true,
                    (d, r) -> d.logo = official(o -> new LogoSpec(o.colorHex(), "k", o.star(), o.shape()))),
            new Variant(Feature.LOGO, "kein Logo", false, true,
                    (d, r) -> d.logo = LogoSpec.NONE),
            new Variant(Feature.LOGO, "fremdes Logo", false, true,
                    (d, r) -> d.logo = pick(List.of(
                            new LogoSpec("#D32F2F", "C", false, "square"),
                            new LogoSpec("#7B1FA2", "KP", false, "square")), r)),
            new Variant(Feature.LOGO, "falsche Farbe und kein Stern", false, true,
                    (d, r) -> d.logo = new LogoSpec(pick(List.of("#2E7D32", "#EF6C00"), r), "K", false, "circle")),

            // ---------------- Signatur / Mitarbeiter-Code ----------------
            new Variant(Feature.SIGNATUR, "Code: ein Zeichen verwechselbar ersetzt (0/O, 1/I …)", true, true,
                    (d, r) -> d.signature = withCode(d.signature, lookalikeCharCode(d.signature.code(), r))),
            new Variant(Feature.SIGNATUR, "Code: zwei Zeichen vertauscht", true, true,
                    (d, r) -> d.signature = withCode(d.signature, transposeCode(d.signature.code(), r))),
            new Variant(Feature.SIGNATUR, "Code: ein Zeichen fehlt", true, true,
                    (d, r) -> d.signature = withCode(d.signature, dropChar(d.signature.code(), r))),
            new Variant(Feature.SIGNATUR, "Code: ein Zeichen geändert", true, true,
                    (d, r) -> d.signature = withCode(d.signature, changeChar(d.signature.code(), r))),
            new Variant(Feature.SIGNATUR, "falsche Abteilung", true, true,
                    (d, r) -> d.signature = new Signature(d.signature.name(), otherDepartment(d.signature.department(), r),
                            d.signature.code())),
            new Variant(Feature.SIGNATUR, "Name leicht falsch geschrieben", true, true,
                    (d, r) -> d.signature = new Signature(misspell(d.signature.name(), r), d.signature.department(),
                            d.signature.code())),
            new Variant(Feature.SIGNATUR, "keine Signatur", false, true,
                    (d, r) -> d.signature = Signature.NONE),
            new Variant(Feature.SIGNATUR, "unbekannte Person", false, true,
                    (d, r) -> d.signature = new Signature("Max Mustermann", "IT Abteilung", "ABCD1234")),
            new Variant(Feature.SIGNATUR, "Code fehlt", false, true,
                    (d, r) -> d.signature = withCode(d.signature, "")),
            new Variant(Feature.SIGNATUR, "Code eines anderen Mitarbeiters", false, true,
                    (d, r) -> d.signature = withCode(d.signature, otherEmployeeCode(d.signature.code(), r))),

            // ---------------- Anhang ----------------
            new Variant(Feature.ANHANG, "Doppel-Endung", true, false,
                    (d, r) -> d.attachment = d.ensureAttachmentBase()
                            + pick(List.of(".pdf.exe", ".docx.scr", ".pdf.js", ".xlsx.bat", ".pdf.lnk"), r)),
            new Variant(Feature.ANHANG, "Office-Datei mit Makros", true, false,
                    (d, r) -> d.attachment = d.ensureAttachmentBase() + pick(List.of(".docm", ".xlsm"), r)),
            new Variant(Feature.ANHANG, "HTML-Anhang (gefälschte Login-Seite)", true, false,
                    (d, r) -> d.attachment = d.ensureAttachmentBase() + pick(List.of(".html", ".htm"), r)),
            new Variant(Feature.ANHANG, "Disk-Image", true, false,
                    (d, r) -> d.attachment = d.ensureAttachmentBase() + pick(List.of(".iso", ".img"), r)),
            new Variant(Feature.ANHANG, "Programm", false, false,
                    (d, r) -> {
                        d.ensureAttachmentBase();
                        d.attachment = pick(List.of("Rechnung.exe", "Update_installieren.bat", "Office_Patch.msi"), r);
                    }),
            new Variant(Feature.ANHANG, "Archiv", false, false,
                    (d, r) -> d.attachment = d.ensureAttachmentBase() + pick(List.of(".zip", ".rar", ".7z"), r)),
            new Variant(Feature.ANHANG, "Skript", false, false,
                    (d, r) -> d.attachment = d.ensureAttachmentBase() + pick(List.of(".vbs", ".js", ".ps1"), r)));

    private Manipulations() {
    }

    /** Kann dieses Merkmal bei einer Mail dieser Art überhaupt verfälscht werden? */
    static boolean appliesTo(Feature feature, boolean partnerMail) {
        return !partnerMail || (feature != Feature.LOGO && feature != Feature.SIGNATUR);
    }

    static List<Variant> variants(Feature feature, boolean subtle, boolean partnerMail) {
        return ALL.stream()
                .filter(v -> v.feature() == feature && v.subtle() == subtle && (!partnerMail || !v.internalOnly()))
                .toList();
    }

    /** Wendet eine zufällige passende Variante an. */
    static void apply(MailDraft draft, Feature feature, boolean subtle, Random rng) {
        List<Variant> options = variants(feature, subtle, draft.isPartner());
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Keine Fehlerart für " + feature + " bei dieser Mail");
        }
        pick(options, rng).apply().accept(draft, rng);
    }

    // ------------------------------------------------------------------
    // Hilfsfunktionen
    // ------------------------------------------------------------------

    private static void setLink(MailDraft d, Random r, String url, boolean mayFakeDisplayText) {
        d.ensureLink();
        String legitLooking = "https://" + d.linkSubdomain() + d.baseDomain() + "/" + d.linkPath();
        d.linkUrl = url;
        // Klassiker: Der sichtbare Text sieht aus wie die echte Adresse.
        if (mayFakeDisplayText && r.nextInt(3) == 0) {
            d.linkText = legitLooking;
        }
    }

    private static LogoSpec official(java.util.function.UnaryOperator<LogoSpec> change) {
        return change.apply(CorporateGuidelines.OFFICIAL_LOGO);
    }

    /** Erzeugt eine täuschend ähnliche Schreibweise (rn statt m, I statt l, Buchstabe doppelt/fehlt …). */
    static String lookalike(String stem, Random rng) {
        List<String> candidates = new ArrayList<>();
        for (int i = 0; i < stem.length(); i++) {
            for (String[] swap : LOOKALIKE_SWAPS) {
                if (stem.startsWith(swap[0], i)) {
                    candidates.add(stem.substring(0, i) + swap[1] + stem.substring(i + swap[0].length()));
                }
            }
            char c = stem.charAt(i);
            if (i > 0 && Character.isLetter(c)) {
                candidates.add(stem.substring(0, i) + stem.substring(i + 1));          // Buchstabe fehlt
                candidates.add(stem.substring(0, i + 1) + c + stem.substring(i + 1));  // Buchstabe doppelt
            }
            if (i > 0 && i + 1 < stem.length() && Character.isLetter(c)
                    && Character.isLetter(stem.charAt(i + 1)) && c != stem.charAt(i + 1)) {
                candidates.add(stem.substring(0, i) + stem.charAt(i + 1) + c + stem.substring(i + 2)); // Dreher
            }
        }
        candidates.removeIf(s -> s.equalsIgnoreCase(stem) || s.startsWith("-") || s.endsWith("-") || s.contains("--"));
        return candidates.get(rng.nextInt(candidates.size()));
    }

    private static Signature withCode(Signature s, String code) {
        return new Signature(s.name(), s.department(), code);
    }

    private static String lookalikeCharCode(String code, Random rng) {
        String from = "0O1I5S8B2Z";
        String to = "O0I1S5B8Z2";
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < code.length(); i++) {
            if (from.indexOf(code.charAt(i)) >= 0) {
                positions.add(i);
            }
        }
        if (positions.isEmpty()) {
            return changeChar(code, rng);
        }
        int idx = pick(positions, rng);
        char replacement = to.charAt(from.indexOf(code.charAt(idx)));
        return code.substring(0, idx) + replacement + code.substring(idx + 1);
    }

    private static String transposeCode(String code, Random rng) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i + 1 < code.length(); i++) {
            if (Character.toUpperCase(code.charAt(i)) != Character.toUpperCase(code.charAt(i + 1))) {
                positions.add(i);
            }
        }
        int i = pick(positions, rng);
        return code.substring(0, i) + code.charAt(i + 1) + code.charAt(i) + code.substring(i + 2);
    }

    private static String dropChar(String code, Random rng) {
        int i = rng.nextInt(code.length());
        return code.substring(0, i) + code.substring(i + 1);
    }

    private static String changeChar(String code, Random rng) {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        int i = rng.nextInt(code.length());
        char c;
        do {
            c = alphabet.charAt(rng.nextInt(alphabet.length()));
        } while (Character.toUpperCase(c) == Character.toUpperCase(code.charAt(i)));
        return code.substring(0, i) + c + code.substring(i + 1);
    }

    private static String otherEmployeeCode(String ownCode, Random rng) {
        List<Employee> others = CorporateGuidelines.EMPLOYEES.stream()
                .filter(e -> !e.code().equalsIgnoreCase(ownCode)).toList();
        return pick(others, rng).code();
    }

    private static String otherDepartment(String own, Random rng) {
        List<String> depts = CorporateGuidelines.EMPLOYEES.stream()
                .map(Employee::department).distinct().filter(dp -> !dp.equalsIgnoreCase(own)).toList();
        return pick(depts, rng);
    }

    /** Nachname minimal verändern: Buchstabe fehlt, doppelt oder vertauscht. */
    private static String misspell(String name, Random rng) {
        int space = name.lastIndexOf(' ');
        String first = name.substring(0, space + 1);
        String last = name.substring(space + 1);
        List<String> candidates = new ArrayList<>();
        for (int i = 1; i < last.length(); i++) {
            candidates.add(last.substring(0, i) + last.substring(i + 1));
            candidates.add(last.substring(0, i + 1) + last.charAt(i) + last.substring(i + 1));
            if (i + 1 < last.length() && last.charAt(i) != last.charAt(i + 1)) {
                candidates.add(last.substring(0, i) + last.charAt(i + 1) + last.charAt(i) + last.substring(i + 2));
            }
        }
        candidates.removeIf(s -> s.equalsIgnoreCase(last));
        return first + pick(candidates, rng);
    }

    private static String randomToken(Random rng, int length) {
        String chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static <T> T pick(List<T> values, Random rng) {
        return values.get(rng.nextInt(values.size()));
    }
}
