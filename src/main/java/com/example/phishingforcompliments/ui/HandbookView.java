package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Handbuch mit blätterbaren Seiten (UC-06, Skizze 4): Tutorial und Phishing-Tipps. */
public class HandbookView extends BorderPane {

    private record Page(String title, String text) {
    }

    private static final List<Page> PAGES = List.of(
            new Page("Willkommen",
                    "Du arbeitest in der Poststelle der Komplimento AG und prüfst alle eingehenden E-Mails, bevor sie "
                            + "an die Kolleginnen und Kollegen weitergehen.\n\n"
                            + "Ziel: Möglichst viele Mails richtig einordnen – Phishing melden, harmlose Mails durchlassen.\n\n"
                            + "Jeden Morgen bekommst du eine Anweisung von der Chefin. Dort stehen neue Regeln, die ab diesem "
                            + "Tag gelten. Die Regeln findest du jederzeit in den Corporate Guidelines."),
            new Page("Bedienung",
                    "1. Öffne das Mail-Programm über das Symbol auf dem Desktop.\n"
                            + "2. Wähle eine Mail aus der Liste.\n"
                            + "3. Klicke oben in der Leiste \"Prüfen:\" auf ein Merkmal (Absender, Link, Logo, Signatur, "
                            + "Anhang). Absender, Logo und Signatur kannst du auch direkt in der Mail anklicken. "
                            + "Es öffnet sich der Vergleich mit den Corporate Guidelines.\n"
                            + "4. Weicht das Merkmal ab → \"Abweichung melden\". Passt es → \"Zurück\" und weiterprüfen.\n"
                            + "5. Ist alles in Ordnung → \"Als sicher ablegen\".\n\n"
                            + "Wichtig: Links und Anhänge niemals anklicken, um sie zu prüfen! Fahre mit der Maus über "
                            + "einen Link – unten in der Statusleiste siehst du das echte Ziel.\n"
                            + "Ein gelb umrandeter Prüf-Button markiert die Regel, die heute neu ist.\n"
                            + "Esc pausiert das Spiel."),
            new Page("Punkte",
                    "Abweichung richtig gemeldet: +" + GameSession.POINTS_REPORT_CORRECT + "\n"
                            + "Harmlose Mail als sicher abgelegt: +" + GameSession.POINTS_FILE_CORRECT + "\n"
                            + "Merkmal gemeldet, das in Ordnung war: " + GameSession.POINTS_REPORT_WRONG + "\n"
                            + "Phishing-Mail als sicher abgelegt: " + GameSession.POINTS_FILE_WRONG + " und eine Verwarnung\n"
                            + "Mail bei Feierabend nicht bearbeitet: " + GameSession.POINTS_UNPROCESSED + "\n\n"
                            + "Bei " + GameState.MAX_WARNINGS + " Verwarnungen wirst du entlassen. "
                            + "Die Kampagne dauert " + GameState.CAMPAIGN_DAYS + " Arbeitstage."),
            new Page("Merkmal: Absender",
                    "Phisher fälschen Absenderadressen. Achte auf die Domain nach dem @:\n\n"
                            + "• Ähnliche Zeichen: kornplimento.de (rn statt m), kompIimento.de (großes I statt l), k0mplimento.de\n"
                            + "• Buchstabe fehlt, doppelt oder vertauscht: komplimetno.de, kompllimento.de\n"
                            + "• Andere Endung: komplimento.com, komplimento.eu\n"
                            + "• Zusätze: mail-komplimento.de, komplimento-de.com\n"
                            + "• Angehängte Domain: komplimento.de.mail-service.net – entscheidend ist der letzte Teil!\n"
                            + "• Freemail-Adressen (gmail.com, web.de …) oder fremde Endungen (.ru, .xyz)\n\n"
                            + "Der angezeigte Name (\"IT-Support\") sagt nichts aus – nur die Adresse zählt."),
            new Page("Merkmal: Links",
                    "Der angezeigte Linktext kann beliebig sein – sogar eine echt aussehende Adresse. "
                            + "Entscheidend ist das Ziel. Und: Zum Prüfen NIE anklicken!\n\n"
                            + "• Maus über den Link halten und das Ziel in der Statusleiste lesen\n"
                            + "• Die Domain ist der Teil zwischen https:// und dem nächsten /\n"
                            + "• Steht ein @ darin, zählt nur, was NACH dem @ kommt: "
                            + "https://komplimento.de@login-check.net führt zu login-check.net\n"
                            + "• intranet.komplimento.de.irgendwas.net oder login-komplimento.de gehören NICHT zur Firma\n"
                            + "• Kurz-Links (bit.ly, tinyurl.com) verstecken das Ziel und sind nicht erlaubt\n"
                            + "• IP-Adressen (http://185.23.44.17/...) sind fast immer verdächtig"),
            new Page("Merkmale: Logo, Signatur, Anhang",
                    "Logo: Vergleiche Form, Farbe, Buchstabe und Stern mit dem offiziellen Logo in den Guidelines. "
                            + "Oft fehlt nur der Stern, das K ist klein oder der Blauton stimmt nicht.\n\n"
                            + "Signatur: Interne Mails enden mit Name, Abteilung und Mitarbeiter-Code. Prüfe im "
                            + "Mitarbeiterverzeichnis Zeichen für Zeichen: Häufig ist nur ein Zeichen falsch "
                            + "(0 statt O, 1 statt I), zwei Zeichen sind vertauscht oder eins fehlt. Auch ein leicht "
                            + "falsch geschriebener Name oder eine falsche Abteilung verraten die Fälschung. "
                            + "Partnerfirmen haben keine Codes – das ist in Ordnung.\n\n"
                            + "Anhang: Nur .pdf, .docx, .xlsx und .png sind erlaubt. Vorsicht bei Doppel-Endungen wie "
                            + "Rechnung.pdf.exe (die letzte Endung zählt), Makro-Dateien (.docm, .xlsm), "
                            + "HTML-Dateien, Archiven (.zip) und Disk-Images (.iso)."),
            new Page("Warnsignale im Text",
                    "Diese Hinweise sind keine Regel, aber ein guter Grund, genauer hinzusehen:\n\n"
                            + "• Zeitdruck: \"innerhalb von 24 Stunden\", \"sofort\", \"sonst wird Ihr Konto gesperrt\"\n"
                            + "• Geld: Überweisungen, Boni, Mahnungen, Bankdaten bestätigen\n"
                            + "• Geheimhaltung: \"Diskretion\", \"ich bin nicht erreichbar\"\n"
                            + "• Tippfehler und unübliche Anrede\n\n"
                            + "Aber: Auch eine seriös klingende Mail kann Phishing sein – und eine Mail über Passwörter "
                            + "kann harmlos sein. Entscheide immer anhand der Merkmale."));

    private int index;
    private final Label title = new Label();
    private final Label text = new Label();
    private final Label counter = new Label();

    public HandbookView() {
        getStyleClass().add("handbook");
        title.getStyleClass().add("h1");
        text.setWrapText(true);
        text.getStyleClass().add("handbook-text");

        ScrollPane scroll = new ScrollPane(text);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");

        VBox page = new VBox(12, title, scroll);
        page.setPadding(new Insets(18));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button prev = new Button("◀  Zurück");
        Button next = new Button("Weiter  ▶");
        prev.setOnAction(e -> show(index - 1));
        next.setOnAction(e -> show(index + 1));
        Region s1 = new Region();
        Region s2 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        counter.getStyleClass().add("muted");
        HBox nav = new HBox(10, prev, s1, counter, s2, next);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(10, 18, 14, 18));

        setCenter(page);
        setBottom(nav);
        show(0);
    }

    private void show(int i) {
        index = Math.floorMod(i, PAGES.size());
        Page p = PAGES.get(index);
        title.setText(p.title());
        text.setText(p.text());
        counter.setText("Seite " + (index + 1) + " von " + PAGES.size());
    }
}
