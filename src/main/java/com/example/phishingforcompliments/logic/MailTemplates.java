package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.LogoSpec;
import com.example.phishingforcompliments.model.Signature;

import java.util.List;

/**
 * Inhaltliche Vorlagen für E-Mails. Alle Vorlagen sind "sauber" (vertrauenswürdige
 * Domains, offizielles Logo, gültige Signatur); erst der {@link MailGenerator} baut
 * Fehler aus dem {@link Manipulations}-Katalog ein. Neue Vorlagen einfach in die Listen
 * aufnehmen – {@code MailTemplatesTest} prüft die Grundregeln.
 *
 * Platzhalter im Body: {player} = Spielername (Pflicht bei internen Mails),
 * {link} = Position des Links (genau einmal, wenn die Vorlage einen Link hat).
 */
public final class MailTemplates {

    /**
     * @param key            eindeutiger Schlüssel (für Debugging und Tests)
     * @param subject        Betreff
     * @param body           Mailtext (sauber formuliert)
     * @param sloppyBody     Variante mit Tippfehlern/Druck für leichtere Stufen, oder null
     * @param senderName     angezeigter Absendername; null = Name der signierenden Person
     * @param localPart      Funktionspostfach vor dem @ (z.B. "it-support"); null = Postfach der signierenden Person
     * @param department     Abteilung, aus der die signierende Person gewählt wird (interne Mails)
     * @param partnerDomain  Domain einer Partnerfirma (externe Mails), sonst null
     * @param externalSignature Signatur für externe Mails, sonst null
     * @param externalLogo   Logo für externe Mails, sonst null
     * @param linkText       Anzeigetext des Links oder null
     * @param linkUrl        Ziel des Links oder null
     * @param attachment     Dateiname des Anhangs oder null
     */
    public record Template(
            String key,
            String subject,
            String body,
            String sloppyBody,
            String senderName,
            String localPart,
            String department,
            String partnerDomain,
            Signature externalSignature,
            LogoSpec externalLogo,
            String linkText,
            String linkUrl,
            String attachment) {

        public boolean isPartner() {
            return partnerDomain != null;
        }

        public boolean hasLink() {
            return linkText != null;
        }
    }

    private MailTemplates() {
    }

    private static Template internal(String key, String department, String subject, String body,
                                     String linkText, String linkUrl, String attachment) {
        return new Template(key, subject, body, null, null, null, department,
                null, null, null, linkText, linkUrl, attachment);
    }

    private static Template lure(String key, String department, String senderName, String localPart,
                                 String subject, String body, String sloppyBody,
                                 String linkText, String linkUrl, String attachment) {
        return new Template(key, subject, body, sloppyBody, senderName, localPart, department,
                null, null, null, linkText, linkUrl, attachment);
    }

    private static Template partner(String key, String partnerDomain, String senderName, String localPart,
                                    String subject, String body, String linkText, String linkUrl, String attachment) {
        return new Template(key, subject, body, null, senderName, localPart, null,
                partnerDomain, PARTNER_SIGNATURES.get(partnerDomain), PARTNER_LOGOS.get(partnerDomain),
                linkText, linkUrl, attachment);
    }

    private static final java.util.Map<String, Signature> PARTNER_SIGNATURES = java.util.Map.of(
            "nordlicht-logistik.de", new Signature("Petra Nordmann", "Buchhaltung · Nordlicht Logistik GmbH", ""),
            "bueroprofi24.de", new Signature("Kundenservice", "Büroprofi24 GmbH", ""),
            "fernweh-reisen.de", new Signature("Firmenreiseservice", "Fernweh Reisen GmbH", ""));

    private static final java.util.Map<String, LogoSpec> PARTNER_LOGOS = java.util.Map.of(
            "nordlicht-logistik.de", new LogoSpec("#2E7D32", "N", false, "square"),
            "bueroprofi24.de", new LogoSpec("#F57C00", "B", false, "square"),
            "fernweh-reisen.de", new LogoSpec("#00897B", "F", false, "circle"));

    // =====================================================================
    // Harmlose interne Mails (dienen auf "Schwer" auch als Basis für Phishing)
    // =====================================================================

    public static final List<Template> LEGIT_INTERNAL = List.of(
            internal("kantine", "Kantine & Facility", "Speiseplan für nächste Woche",
                    "Hallo {player},\n\nanbei der Speiseplan für die kommende Woche. Am Donnerstag gibt es wieder "
                            + "Käsespätzle, am Freitag eine vegane Linsen-Bolognese. Bitte bis Mittwoch über das "
                            + "Kantinen-Portal anmelden: {link}\n\nGuten Appetit!",
                    "Kantinen-Portal", "https://intranet.komplimento.de/kantine", "Speiseplan_KW46.pdf"),
            internal("parkplatz", "Kantine & Facility", "Parkplatz P2 am Donnerstag gesperrt",
                    "Hallo {player},\n\nam Donnerstag wird der Parkplatz P2 neu markiert und ist ganztägig gesperrt. "
                            + "Bitte nutzt P1 oder das Parkhaus gegenüber. Den Lageplan findet ihr im Anhang.",
                    null, null, "Lageplan_Parkplatz.png"),
            internal("brandschutz", "Kantine & Facility", "Brandschutzübung am Mittwoch um 10 Uhr",
                    "Hallo {player},\n\nam Mittwoch um 10 Uhr findet eine angekündigte Brandschutzübung statt. "
                            + "Bitte verlasst bei Alarm das Gebäude über die gekennzeichneten Fluchtwege und sammelt "
                            + "euch am Treffpunkt B. Alle Infos: {link}",
                    "Brandschutz im Intranet", "https://intranet.komplimento.de/brandschutz", null),
            internal("wartung", "IT-Support", "Geplante Wartung der Fileserver am Samstag",
                    "Hallo {player},\n\nam Samstag zwischen 08:00 und 12:00 Uhr werden unsere Fileserver gewartet. "
                            + "In dieser Zeit ist kein Zugriff auf das Laufwerk Z: möglich. Bitte speichert eure Arbeit "
                            + "vorher.\n\nDetails stehen im Wiki: {link}",
                    "Wartungsplan im Wiki", "https://wiki.komplimento.de/it/wartung", null),
            internal("vpn", "IT-Support", "VPN-Client wird am Montag aktualisiert",
                    "Hallo {player},\n\nam Montag verteilen wir automatisch eine neue Version des VPN-Clients. "
                            + "Ihr müsst nichts tun, nur den Rechner danach einmal neu starten. Fragen beantwortet "
                            + "das Wiki: {link}",
                    "VPN im Wiki", "https://wiki.komplimento.de/it/vpn", null),
            internal("drucker", "IT-Support", "Neuer Farbdrucker im 2. OG",
                    "Hallo {player},\n\nim 2. OG neben der Teeküche steht ab heute ein neuer Farbdrucker "
                            + "(\"PR-2OG-Farbe\"). Er wird automatisch auf euren Rechnern eingerichtet – ihr müsst "
                            + "nichts installieren.",
                    null, null, null),
            internal("passwortrichtlinie", "IT-Sicherheit", "Neue Passwort-Richtlinie ab 1. Dezember",
                    "Hallo {player},\n\nab 1. Dezember gelten neue Mindestanforderungen für Passwörter (14 Zeichen). "
                            + "Es ist nichts zu tun – beim nächsten regulären Wechsel weist das System euch darauf hin. "
                            + "Die Richtlinie findet ihr im Wiki: {link}",
                    "Passwort-Richtlinie", "https://wiki.komplimento.de/it/passwort-richtlinie", null),
            internal("phishingsim", "IT-Sicherheit", "Ankündigung: Phishing-Simulation im Dezember",
                    "Hallo {player},\n\nim Dezember führen wir eine angekündigte Phishing-Simulation durch. "
                            + "Verdächtige Mails meldet ihr wie gewohnt über die Prüfstelle. Hintergründe und Tipps "
                            + "stehen im Wiki: {link}",
                    "Security-Awareness im Wiki", "https://wiki.komplimento.de/it/awareness", null),
            internal("gehalt", "Personalabteilung", "Ihre Gehaltsabrechnung für Oktober",
                    "Hallo {player},\n\nIhre Gehaltsabrechnung für Oktober liegt im Personalportal bereit: {link}\n\n"
                            + "Zur Information: Die Abrechnung hängt zusätzlich als PDF an.",
                    "Personalportal", "https://personal.komplimento.de/abrechnungen", "Gehaltsabrechnung_2026-10.pdf"),
            internal("urlaub", "Personalabteilung", "Ihr Urlaubsantrag wurde genehmigt",
                    "Hallo {player},\n\nIhr Urlaubsantrag für den 22.12. bis 02.01. wurde genehmigt. Den aktuellen "
                            + "Stand Ihres Urlaubskontos sehen Sie im Personalportal: {link}\n\nErholsame Feiertage!",
                    "Urlaubskonto ansehen", "https://personal.komplimento.de/urlaub", null),
            internal("zeiterfassung", "Personalabteilung", "Erinnerung: Zeiterfassung für Oktober abschließen",
                    "Hallo {player},\n\nbitte schließen Sie Ihre Zeiterfassung für Oktober bis Freitag ab. "
                            + "Offene Tage sind im Portal markiert: {link}",
                    "Zur Zeiterfassung", "https://personal.komplimento.de/zeiterfassung", null),
            internal("schulung", "Personalabteilung", "Erinnerung: Datenschutz-Schulung am Dienstag",
                    "Hallo {player},\n\nkleine Erinnerung an die Datenschutz-Schulung am Dienstag um 14 Uhr "
                            + "(Raum Weser). Die Teilnahme ist Pflicht, die Folien gibt es danach im Intranet.",
                    null, null, null),
            internal("budget", "Buchhaltung", "Budgetplanung Q1 – Rückfrage",
                    "Hallo {player},\n\nfür die Budgetplanung Q1 fehlt mir noch deine Schätzung für Reisekosten. "
                            + "Trag sie bitte bis Freitag in die angehängte Tabelle ein und schick sie mir zurück.\n\nDanke!",
                    null, null, "Budget_Q1_Reisekosten.xlsx"),
            internal("reisekosten", "Buchhaltung", "Reisekostenabrechnung: Hotelbeleg fehlt",
                    "Hallo {player},\n\nzu Ihrer Reisekostenabrechnung vom 14.10. fehlt noch der Hotelbeleg. "
                            + "Laden Sie ihn bitte im Finanzportal hoch, dann können wir die Erstattung anweisen: {link}",
                    "Finanzportal", "https://finanzen.komplimento.de/reisekosten", null),
            internal("jahresabschluss", "Buchhaltung", "Zeitplan Jahresabschluss 2026",
                    "Hallo {player},\n\nanbei der Zeitplan für den Jahresabschluss. Bitte reicht offene Rechnungen "
                            + "und Belege bis spätestens 15. Dezember ein, damit wir pünktlich fertig werden.",
                    null, null, "Zeitplan_Jahresabschluss.pdf"),
            internal("meeting", "Marketing", "Einladung: Team-Meeting am Donnerstag, 10:00 Uhr",
                    "Hallo {player},\n\nwie besprochen treffen wir uns am Donnerstag um 10 Uhr im Raum Elbe. "
                            + "Die Agenda hängt an. Bitte gib kurz Bescheid, falls du nicht kannst.",
                    null, null, "Agenda_Team-Meeting.docx"),
            internal("sommerfest", "Marketing", "Save the Date: Sommerfest am 12. Juni",
                    "Hallo {player},\n\nunser Sommerfest findet am 12. Juni statt. Anmeldung und Essenswünsche bitte "
                            + "über das Intranet: {link}\n\nWir freuen uns auf euch!",
                    "Zur Anmeldung", "https://intranet.komplimento.de/sommerfest", "Sommerfest_Plakat.png"),
            internal("newsletter", "Marketing", "Interner Newsletter – Ausgabe November",
                    "Hallo {player},\n\ndie neue Ausgabe unseres Newsletters ist da: Neues aus dem Vertrieb, ein "
                            + "Rückblick auf die Messe und die Gewinner des Fotowettbewerbs. Online lesen: {link}",
                    "Newsletter lesen", "https://intranet.komplimento.de/newsletter", "Newsletter_November.pdf"),
            internal("messe", "Vertrieb", "Nachbericht Fachmesse – Kontaktliste",
                    "Hallo {player},\n\ndanke für die Unterstützung am Messestand! Im Anhang findest du die Liste "
                            + "der neuen Kontakte. Bitte trag bis Ende der Woche ein, bei wem du nachfassen kannst.",
                    null, null, "Messekontakte.xlsx"),
            internal("angebot", "Vertrieb", "Angebot Hansen & Co. – bitte gegenlesen",
                    "Hallo {player},\n\nkönntest du bis morgen Mittag einen Blick auf das Angebot für Hansen & Co. "
                            + "werfen? Mir geht es vor allem um die Formulierungen auf Seite 2.",
                    null, null, "Angebot_Hansen.docx"),
            internal("rahmenvertrag", "Einkauf", "Neuer Rahmenvertrag für Büromaterial",
                    "Hallo {player},\n\nab Januar bestellen wir Büromaterial über den neuen Rahmenvertrag mit "
                            + "Büroprofi24. Bestellliste und Konditionen findest du im Intranet: {link}",
                    "Einkauf im Intranet", "https://intranet.komplimento.de/einkauf", "Rahmenvertrag_Bueromaterial.pdf"),
            internal("lieferantenbewertung", "Einkauf", "Lieferantenbewertung Q4 – deine Einschätzung",
                    "Hallo {player},\n\nfür die Lieferantenbewertung Q4 brauche ich deine Einschätzung zu Termintreue "
                            + "und Qualität. Die Tabelle hängt an – eine kurze Bewertung pro Zeile reicht.",
                    null, null, "Lieferantenbewertung_Q4.xlsx"),
            internal("datenschutz", "Recht & Compliance", "Aktualisierte Datenschutzrichtlinie",
                    "Hallo {player},\n\nwir haben die interne Datenschutzrichtlinie überarbeitet. Die wichtigsten "
                            + "Änderungen sind auf Seite 1 zusammengefasst, die vollständige Fassung steht im Wiki: {link}",
                    "Richtlinie im Wiki", "https://wiki.komplimento.de/recht/datenschutz", "Datenschutzrichtlinie_2027.pdf"),
            internal("geschenke", "Recht & Compliance", "Erinnerung: Geschenke von Geschäftspartnern melden",
                    "Hallo {player},\n\nzur Weihnachtszeit eine kurze Erinnerung: Geschenke von Geschäftspartnern "
                            + "über 35 € müssen gemeldet werden. Das Formular findest du hier: {link}",
                    "Meldeformular", "https://intranet.komplimento.de/compliance", null),
            internal("townhall", "Geschäftsführung", "Quartalszahlen Q3 und Townhall am Freitag",
                    "Hallo {player},\n\nam Freitag um 15 Uhr stellen wir in der Townhall die Zahlen für das dritte "
                            + "Quartal vor. Die Präsentation hängt bereits an, den Livestream findet ihr im Intranet: {link}",
                    "Zum Livestream", "https://intranet.komplimento.de/townhall", "Quartalszahlen_Q3.pdf"));

    // =====================================================================
    // Harmlose Mails von Partnerfirmen (eigenes Logo, keine Mitarbeiter-Signatur)
    // =====================================================================

    public static final List<Template> LEGIT_PARTNER = List.of(
            partner("nordlicht", "nordlicht-logistik.de", "Nordlicht Logistik – Buchhaltung", "buchhaltung",
                    "Rechnung Nr. 2026-1187",
                    "Sehr geehrte Damen und Herren,\n\nanbei erhalten Sie unsere Rechnung Nr. 2026-1187 für die "
                            + "Transporte im Oktober. Zahlbar innerhalb von 14 Tagen.\n\nMit freundlichen Grüßen",
                    null, null, "Rechnung_2026-1187.pdf"),
            partner("lieferavis", "nordlicht-logistik.de", "Nordlicht Logistik – Disposition", "disposition",
                    "Lieferavis: Ihre Sendung kommt am Donnerstag",
                    "Guten Tag,\n\nIhre Sendung mit 4 Paletten wird am Donnerstag zwischen 8 und 12 Uhr an Rampe 2 "
                            + "angeliefert. Den aktuellen Status sehen Sie hier: {link}",
                    "Sendungsstatus", "https://www.nordlicht-logistik.de/sendungen/7741", null),
            partner("bueroprofi", "bueroprofi24.de", "Büroprofi24 Kundenservice", "service",
                    "Ihre Bestellung #48213 wurde versandt",
                    "Guten Tag,\n\nIhre Bestellung #48213 (Druckerpapier, Toner) wurde heute versandt. "
                            + "Die Sendungsverfolgung finden Sie hier: {link}",
                    "Sendung verfolgen", "https://www.bueroprofi24.de/tracking/48213", null),
            partner("reise", "fernweh-reisen.de", "Fernweh Reisen – Firmenservice", "firmenservice",
                    "Reisebestätigung Hamburg – München, 18.11.",
                    "Guten Tag,\n\nanbei Ihre Reisebestätigung für die Dienstreise nach München am 18.11. "
                            + "(Zug und Hotel). Änderungen können Sie bis 24 Stunden vorher im Portal vornehmen: {link}",
                    "Buchung ansehen", "https://www.fernweh-reisen.de/buchungen/FR-30981", "Reisebestaetigung_FR-30981.pdf"));

    // =====================================================================
    // Typische Phishing-Köder im Namen der Komplimento AG (Druck, Geld, Zugangsdaten).
    // Alle Merkmale sind standardmäßig sauber – Fehler baut der Generator ein.
    // =====================================================================

    public static final List<Template> LURES = List.of(
            lure("passwort", "IT-Support", "IT-Support", "it-support", "Ihr Passwort läuft in 24 Stunden ab",
                    "Hallo {player},\n\nIhr Passwort läuft in 24 Stunden ab. Um den Zugriff auf Ihr Konto nicht zu "
                            + "verlieren, bestätigen Sie bitte umgehend Ihre Zugangsdaten: {link}\n\n"
                            + "Bei Nichtbeachtung wird Ihr Konto gesperrt.",
                    "Hallo {player},\n\nIhr Passwort läuft in 24 Stunden ab!! Um den Zugrif auf Ihr Konto nicht zu "
                            + "verlieren, bestätigen Sie SOFORT Ihre Zugangsdaten: {link}\n\n"
                            + "Bei Nichtbeachtung wird Ihr Konto unwiederruflich gesperrt!",
                    "Zugangsdaten bestätigen", "https://login.komplimento.de/konto", null),
            lure("postfach", "IT-Support", "Mail-Administrator", "postmaster", "Ihr Postfach ist zu 98 % voll",
                    "Hallo {player},\n\nIhr Postfach hat fast die Speichergrenze erreicht. Damit Sie weiterhin "
                            + "E-Mails empfangen können, erweitern Sie bitte Ihren Speicher: {link}",
                    "Hallo {player},\n\nIhr Postfach ist zu 98% VOLL. Damit sie weiterhin E-Mails empfangen "
                            + "können, erweitern sie jetzt sofort ihren Speicher: {link}\n\nSonst werden alle Mails gelöscht!",
                    "Speicher erweitern", "https://mail.komplimento.de/speicher", null),
            lure("lizenz", "IT-Support", "IT-Support", "it-support", "Microsoft 365: Lizenz abgelaufen",
                    "Hallo {player},\n\nIhre Microsoft-365-Lizenz ist abgelaufen. Installieren Sie bitte das "
                            + "angehängte Update, um Office weiterhin nutzen zu können. Alternativ können Sie die "
                            + "Lizenz hier erneuern: {link}",
                    "Hallo {player},\n\nIhre Microsoft 365 Lizenz ist ABGELAUFEN. Instalieren sie sofort das "
                            + "angehängte Update, sonst können sie Office nicht mehr nutzen. Oder erneuern sie "
                            + "die Lizenz hier: {link}",
                    "Lizenz erneuern", "https://it.komplimento.de/lizenzen", "Office_Update.pdf"),
            lure("dokument", "IT-Support", "Dokumentenablage", "dokumente", "Ein Dokument wurde mit Ihnen geteilt",
                    "Hallo {player},\n\nein Kollege hat das Dokument \"Gehaltsübersicht_2027.xlsx\" mit Ihnen geteilt. "
                            + "Die Freigabe läuft in 12 Stunden ab: {link}",
                    "Hallo {player},\n\nein Kolege hat das Dokument \"Gehaltsübersicht_2027.xlsx\" mit ihnen geteilt!! "
                            + "Öffnen sie es schnell, die Freigabe läuft in 12 Stunden ab: {link}",
                    "Dokument öffnen", "https://dokumente.komplimento.de/freigabe", null),
            lure("sprachnachricht", "IT-Support", "Telefonanlage", "voicemail", "Neue Sprachnachricht (0:42)",
                    "Hallo {player},\n\nSie haben eine neue Sprachnachricht von einer externen Nummer erhalten. "
                            + "Das automatische Transkript hängt an.",
                    "Hallo {player},\n\nsie haben eine NEUE Sprachnachricht von einer externen Nummer erhalten!! "
                            + "Das Transkript hängt an, bitte sofort anhören.",
                    null, null, "Sprachnachricht_Transkript.pdf"),
            lure("mfa", "IT-Sicherheit", "IT-Sicherheit", "security", "Pflicht: Zwei-Faktor-Anmeldung heute einrichten",
                    "Hallo {player},\n\nab morgen ist die Zwei-Faktor-Anmeldung für alle Konten Pflicht. Richten Sie "
                            + "sie bitte noch heute ein, sonst ist keine Anmeldung mehr möglich: {link}",
                    "Hallo {player},\n\nab MORGEN ist die Zwei-Faktor-Anmeldung Pflicht!! Richten sie sie noch heute "
                            + "ein, sonst wird ihr Zugang gesperrt: {link}",
                    "Jetzt einrichten", "https://login.komplimento.de/mfa", null),
            lure("kontogesperrt", "IT-Sicherheit", "Security Operations Center", "soc",
                    "Verdächtige Anmeldung – Konto vorübergehend gesperrt",
                    "Hallo {player},\n\nwir haben eine verdächtige Anmeldung an Ihrem Konto festgestellt und es "
                            + "vorsorglich gesperrt. Bestätigen Sie Ihre Identität, um es wieder freizuschalten: {link}",
                    "Hallo {player},\n\nwir haben eine verdächtige Anmeldung festgestellt und ihr Konto GESPERRT. "
                            + "Bestätigen sie sofort ihre Identität, sonst wird es gelöscht: {link}",
                    "Konto entsperren", "https://login.komplimento.de/entsperren", null),
            lure("ceo", "Geschäftsführung", null, null, "Dringend – vertrauliche Überweisung",
                    "Hallo {player},\n\nich sitze gerade in einer Verhandlung und brauche kurzfristig Ihre Hilfe. "
                            + "Bitte überweisen Sie 4.850 € an den Empfänger im Anhang – es geht um eine Vertragsstrafe, "
                            + "die heute fällig ist. Ich bin telefonisch nicht erreichbar.\n\nVielen Dank für Ihre Diskretion.",
                    "Hallo {player},\n\nich sitze grade in einer Verhandlung und brauche DRINGEND ihre Hilfe. "
                            + "Überweisen sie bitte sofort 4.850 € an den Empfänger im Anhang, es geht um eine Vertragsstrafe "
                            + "die heute fällig ist!! Ich bin telefonisch nicht erreichbar.\n\nDanke für ihre Diskretion.",
                    null, null, "Zahlungsdaten.pdf"),
            lure("bonus", "Personalabteilung", null, null, "Bonuszahlung – Bestätigung erforderlich",
                    "Hallo {player},\n\nfür Sie wurde eine Bonuszahlung in Höhe von 1.200 € freigegeben. Bitte "
                            + "bestätigen Sie Ihre Bankverbindung bis heute 17 Uhr im Portal: {link}\n\n"
                            + "Andernfalls verfällt der Anspruch.",
                    "Hallo {player},\n\nHerzlichen Glückwunsch!!! Für sie wurde eine Bonuszahlung von 1.200 € "
                            + "freigegeben. Bestätigen sie ihre Bankverbindung bis heute 17 Uhr: {link}\n\n"
                            + "Sonst verfällt der Anspruch!",
                    "Bankverbindung bestätigen", "https://personal.komplimento.de/bonus", null),
            lure("gehaltsanpassung", "Personalabteilung", null, null, "Vertraulich: Ihre Gehaltsanpassung 2027",
                    "Hallo {player},\n\nim Anhang finden Sie die Übersicht zu Ihrer Gehaltsanpassung 2027. Bitte "
                            + "behandeln Sie das Dokument vertraulich und bestätigen Sie den Erhalt bis heute Abend.",
                    "Hallo {player},\n\nim Anhang finden sie die Übersicht zu ihrer Gehaltsanpassung 2027!! Streng "
                            + "vertraulich, bitte SOFORT öffnen und den Erhalt bestätigen.",
                    null, null, "Gehaltsanpassung_2027.docx"),
            lure("mahnung", "Buchhaltung", null, null, "Offene Rechnung – letzte Mahnung",
                    "Hallo {player},\n\nlaut unserer Buchhaltung ist die Rechnung 2026-0931 noch offen. Prüfen Sie "
                            + "bitte die angehängte Mahnung und überweisen Sie den Betrag heute, um Mahngebühren "
                            + "zu vermeiden.",
                    "Hallo {player},\n\nlaut unserer Buchaltung ist die Rechnung 2026-0931 immernoch offen!! "
                            + "Prüfen sie SOFORT die angehängte Mahnung und überweisen sie den Betrag heute, "
                            + "sonst kommen hohe Mahngebühren auf sie zu.",
                    null, null, "Mahnung_2026-0931.pdf"),
            lure("steuer", "Buchhaltung", null, null, "Lohnsteuer-Rückerstattung – Bankdaten bestätigen",
                    "Hallo {player},\n\nIhnen steht eine Rückerstattung von 312,40 € aus dem Lohnsteuerausgleich zu. "
                            + "Bitte bestätigen Sie Ihre Bankdaten bis Freitag, damit wir die Zahlung anweisen können: {link}",
                    "Hallo {player},\n\nihnen steht eine Rückerstatung von 312,40 € zu!! Bestätigen sie ihre "
                            + "Bankdaten bis Freitag, sonst verfällt die Zahlung: {link}",
                    "Bankdaten bestätigen", "https://personal.komplimento.de/erstattung", null),
            lure("gutschein", "Marketing", null, null, "Weihnachtsgutschein für alle Mitarbeitenden",
                    "Hallo {player},\n\nals Dankeschön für das tolle Jahr erhält jede und jeder einen Gutschein über "
                            + "50 €. Lösen Sie ihn bis Sonntag ein, solange der Vorrat reicht: {link}",
                    "Hallo {player},\n\nals Dankeschön bekommt JEDER einen Gutschein über 50 €!!! Schnell einlösen, "
                            + "nur solange der Vorrat reicht: {link}",
                    "Gutschein einlösen", "https://intranet.komplimento.de/gutschein", null),
            lure("vertrag", "Vertrieb", null, null, "Kunde wartet – Vertrag bitte sofort unterschreiben",
                    "Hallo {player},\n\nder Kunde wartet auf die Gegenzeichnung des Rahmenvertrags. Ich bin den ganzen "
                            + "Tag beim Termin – bitte unterschreib ihn digital, sobald du das liest: {link}",
                    "Hallo {player},\n\nder Kunde wartet!! Bitte unterschreib den Vertrag SOFORT digital, ich bin "
                            + "den ganzen Tag nicht erreichbar: {link}",
                    "Vertrag unterschreiben", "https://vertrag.komplimento.de/signatur", null),
            lure("vorkasse", "Einkauf", null, null, "Dringend: Lieferant verlangt heute Vorkasse",
                    "Hallo {player},\n\nunser Lieferant liefert nur noch gegen Vorkasse. Damit die Produktion nicht "
                            + "stillsteht, gib bitte die angehängte Rechnung heute noch zur Zahlung frei.",
                    "Hallo {player},\n\nunser Lieferant liefert nur noch gegen VORKASSE!! Gib bitte die angehängte "
                            + "Rechnung heute noch frei, sonst steht die Produktion still.",
                    null, null, "Vorkasse_Rechnung.pdf"),
            lure("paket", "Kantine & Facility", "Poststelle", "poststelle", "Paket konnte nicht zugestellt werden",
                    "Hallo {player},\n\nein Paket für Sie konnte nicht zugestellt werden. Bitte vereinbaren Sie "
                            + "einen neuen Zustelltermin: {link}",
                    "Hallo {player},\n\nein Packet für sie konnte nicht zugestellt werden!! Vereinbaren sie "
                            + "innerhalb von 24h einen neuen Zustelltermin, sonst geht das Packet zurück: {link}",
                    "Zustelltermin wählen", "https://intranet.komplimento.de/poststelle", null));

    // =====================================================================
    // Köder im Namen von Partnerfirmen (Rechnungs- und Zahlungsbetrug). Partner-Mails haben
    // weder Firmenlogo noch Mitarbeiter-Code, daher sind nur Absender, Link und Anhang manipulierbar.
    // =====================================================================

    public static final List<Template> PARTNER_LURES = List.of(
            partner("bankwechsel", "nordlicht-logistik.de", "Nordlicht Logistik – Buchhaltung", "buchhaltung",
                    "Wichtig: Neue Bankverbindung ab sofort",
                    "Sehr geehrte Damen und Herren,\n\nbitte beachten Sie, dass sich unsere Bankverbindung geändert hat. "
                            + "Überweisen Sie die offene Rechnung 2026-1187 bitte ab sofort auf das Konto im Anhang. "
                            + "Zahlungen auf das alte Konto können nicht mehr zugeordnet werden.\n\nMit freundlichen Grüßen",
                    null, null, "Neue_Bankverbindung.pdf"),
            partner("zahlung", "bueroprofi24.de", "Büroprofi24 Kundenservice", "service",
                    "Zahlung fehlgeschlagen – Bestellung #48377",
                    "Guten Tag,\n\ndie Zahlung für Ihre Bestellung #48377 konnte nicht verarbeitet werden. "
                            + "Bitte aktualisieren Sie Ihre Zahlungsdaten innerhalb von 48 Stunden: {link}\n\n"
                            + "Andernfalls wird die Bestellung storniert.",
                    "Zahlungsdaten aktualisieren", "https://www.bueroprofi24.de/konto/zahlung", null),
            partner("zahlungserinnerung", "bueroprofi24.de", "Büroprofi24 Buchhaltung", "rechnung",
                    "Zahlungserinnerung Rechnung 2026-5521",
                    "Guten Tag,\n\nfür die Rechnung 2026-5521 konnten wir noch keinen Zahlungseingang feststellen. "
                            + "Die Rechnung hängt erneut an. Bitte begleichen Sie den Betrag innerhalb von 3 Tagen.",
                    null, null, "Rechnung_2026-5521.pdf"),
            partner("storno", "fernweh-reisen.de", "Fernweh Reisen – Firmenservice", "firmenservice",
                    "Ihre Buchung wird storniert – Zahlungsdaten bestätigen",
                    "Guten Tag,\n\nbei Ihrer Buchung FR-30981 ist die Kreditkartenzahlung fehlgeschlagen. Bestätigen "
                            + "Sie Ihre Zahlungsdaten bis heute 18 Uhr, sonst wird die Reise storniert: {link}",
                    "Zahlungsdaten bestätigen", "https://www.fernweh-reisen.de/buchungen/zahlung", null));
}
