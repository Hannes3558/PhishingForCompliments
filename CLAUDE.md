# Phishing for Compliments

Lernspiel (Uni-Projekt, 3. Semester BIN, WS 2026/27) im Stil von *Papers, Please*: Der Spieler sitzt am
Desktop eines Mitarbeiters der fiktiven **Komplimento AG**, bekommt jeden Morgen eine Anweisung der Chefin
und muss die eingehenden E-Mails auf Phishing prüfen. Pro Tag kommt eine neue Prüfregel dazu; Schwierigkeit
steuert Mail-Anzahl, Fehleranzahl und wie subtil die Fehler sind.

Spezifikation: `docs/use-cases-hhst.pdf` (UC-01…UC-07, NFR-01…03) und `docs/gui-prototyp-hhst.pdf`
(Skizzen: Titlescreen → Homescreen → Mail-Programm → Handbuch). Der Code setzt genau diese Skizzen um.

## Build & Start

JavaFX 21 + Maven, Java 21 (Release-Level). Ein JDK ≥ 21 muss auf `JAVA_HOME` liegen – im Projekt wird
das IntelliJ-JBR benutzt (`C:\Users\<user>\AppData\Local\Programs\IntelliJ IDEA\jbr`).

```bash
./mvnw javafx:run      # Spiel starten
./mvnw test            # Unit-Tests (reine Logik, kein JavaFX nötig)
./mvnw clean package   # Build
```

In IntelliJ: `Launcher.main` ausführen (nicht `PhishingApp` direkt – Launcher umgeht das
JavaFX-Modul-Problem beim Start aus der IDE).

Git-Bash/PowerShell: `JAVA_HOME` ggf. vorher setzen, sonst findet `mvnw` kein Java.

## Als .exe weitergeben

```powershell
powershell -ExecutionPolicy Bypass -File packaging\build-exe.ps1   # optional: -SkipTests, -Jdk <Pfad>
```

Ergebnis: `dist\PhishingForCompliments.exe` (~30 MB), **eine einzelne Datei**, kein Java nötig.
`dist/` und `target/` stehen in `.gitignore` – die .exe nie einchecken, sondern bauen.
Braucht ein JDK 21+ **mit `jpackage`** (das IntelliJ-JBR hat keins; benutzt wird das neueste in
`%USERPROFILE%\.jdks`, aktuell Temurin 21).

Aufbau: `jpackage` erzeugt ein App-Abbild mit abgespeckter Java-Laufzeit → wird gezippt und in einen
kleinen C#-Starter (`packaging/launcher/PfcLauncher.cs`, kompiliert mit dem in Windows enthaltenen
`csc.exe`) eingebettet. Beim ersten Start entpackt der Starter nach
`%LOCALAPPDATA%\PhishingForCompliments\app-<build>` (Fortschrittsfenster), danach startet er sofort;
alte Builds werden aufgeräumt. `PFC_HOME` überschreibt den Zielordner (für Tests).

Das Skript prüft sich selbst: `PhishingForCompliments.exe --selftest` startet JavaFX **ohne Fenster**,
baut Titel und Desktop und liefert Exit-Code 0/1 (Log: `%TEMP%\phishingforcompliments-selftest.log`).
Icon: `packaging/make-icon.ps1` erzeugt `packaging/app.ico` und die Fenster-Icons `icon-*.png`.

Hinweis für Empfänger: Die .exe ist nicht signiert → Windows SmartScreen warnt beim ersten Start
(„Weitere Informationen" → „Trotzdem ausführen"). Mail-Anbieter blockieren .exe-Anhänge oft; besser
per Cloud-Link teilen.

## Architektur

Paket `com.example.phishingforcompliments` (Name stammt vom IntelliJ-Template, bewusst beibehalten).

```
model/   Records & Enums ohne JavaFX   (Email, Feature, Difficulty, GameState, DecisionResult, DayReport …)
logic/   Spiellogik ohne JavaFX        (CorporateGuidelines, RuleChecker, MailTemplates, MailDraft,
                                        Manipulations, MailGenerator, GameSession, ChefMessages,
                                        SaveGameService)
ui/      JavaFX-Views, programmatisch  (TitleScreen, DesktopScreen, DesktopWindow, Taskbar, MailAppView,
                                        ComparisonView, GuidelinesView, HandbookView, ChefMessageView,
                                        DayReportScreen, LogoView, PauseOverlay)
PhishingApp   Application + Szenenwechsel      Launcher   main()
resources/…/styles.css   gesamtes Styling (Theme "KomplimentOS")
```

**Fenster & Taskleiste:** Jedes Programm ist ein `DesktopWindow` (ziehbar, minimierbar, schließbar) im
`windowLayer` des `DesktopScreen`. Neue Fenster immer über `DesktopScreen.open()` bzw. `addWindow()`
anlegen – nur dann bekommen sie einen Button in der `Taskbar`. Die Taskleiste im Windows-Stil: links
Start-Menü (Programme + Pause/Speichern/Hauptmenü), Mitte ein Button pro offenem Fenster (Klick =
wiederherstellen / nach vorne / minimieren), rechts Feierabend, Punkte, Verwarnungen, Tagesfortschritt,
Uhr + Tag, ganz außen „Desktop anzeigen". Das oberste sichtbare Fenster trägt die Pseudoklasse
`:active`. Fenster, die das Spiel anhalten (Verwarnung), sind `setMinimizable(false)`.

**Schichtregel:** `model` und `logic` dürfen **kein** `javafx.*` importieren. Alles, was Spielregeln
betrifft, lebt in `logic` und ist per JUnit testbar. Die UI ruft nur `GameSession` auf.

**Kein FXML:** Views werden in Java gebaut (dynamische Fenster/Mails sind so einfacher). Styling nur über
CSS-Klassen in `styles.css`, keine Inline-Styles außer für berechnete Werte (Größen, Farben aus Daten).

### Kernfluss

1. `TitleScreen` → Name + `Difficulty` → `PhishingApp.startNewGame()` (UC-01/UC-04) oder `loadGame()` (UC-03)
2. `PhishingApp.beginDay()` → `GameSession.startDay()` erzeugt Mails (seed + Tag ⇒ deterministisch),
   speichert den Stand vom Tagesbeginn, zeigt `DesktopScreen` mit Chef-Anweisung
3. Spieler öffnet Mail → Werkzeugleiste „Prüfen: …" (oder Klick auf Absender/Logo/Signatur) →
   `ComparisonView` (UC-07) → `GameSession.reportFeature()` oder `fileAsSafe()`.
   **Links und Anhänge sind nie der Weg zum Prüfen:** Ein Klick darauf öffnet nichts und meldet nichts,
   sondern zeigt nur eine Warnung (das Spiel soll genau dieses Anklicken abtrainieren). Link-Ziele sieht
   man per Hover in der Statusleiste.
4. „Feierabend" oder Uhr 17:00 → `GameSession.endDay()` → `DayReportScreen` → nächster Tag
5. 3 Verwarnungen = gekündigt, 5 Tage = Kampagne fertig → `finishCampaign()` löscht den Spielstand

### Wahrheit über Phishing

Der `MailGenerator` baut Abweichungen in saubere Vorlagen ein, aber **ob eine Mail Phishing ist,
entscheidet ausschließlich `RuleChecker.violations(email, day)`** anhand der `CorporateGuidelines`.
Nur Merkmale, deren Regel am Tag schon gilt (`Feature.introducedOnDay`), werden geprüft. Darum:
Vorlagen in `MailTemplates` müssen immer sauber sein (Firmendomain, offizielles Logo, gültige Signatur,
erlaubte Anhänge) – sonst werden legitime Mails fälschlich zu Phishing. `ManipulationsTest` prüft das.

Fehlerarten stehen im Katalog `Manipulations.ALL` (je Merkmal mehrere subtile und deutliche Varianten,
z. B. Logo nur ohne Stern, Mitarbeiter-Code mit nur einem falschen/vertauschten/fehlenden Zeichen,
@-Trick im Link, Makro-Anhang). **Jede Variante verfälscht genau ein Merkmal** –
`ManipulationsTest` prüft jede Variante gegen jede Vorlage.

Partner-Ausnahme: Ob Logo/Signatur geprüft werden, hängt an `Email.senderCompany` (als wen die Mail
auftritt), nicht an der Absenderdomain. Eine gefälschte „Nordlicht"-Mail verstößt also nur beim Absender.

Garantie für Lerntage: An dem Tag, an dem ein Merkmal neu eingeführt wird, enthalten mindestens
`MailGenerator.NEW_FEATURE_MIN_MAILS` (= 2) Mails **genau diesen einen** Fehler.

### Spielregeln (Stand Prototyp)

| Merkmal   | ab Tag | Regel |
|-----------|--------|-------|
| Absender  | 1 | Domain `komplimento.de` (inkl. Subdomains) oder Partnerdomain |
| Link      | 2 | jedes Link-Ziel auf Firmen-/Partnerdomain; Anzeigetext egal; bei `user@host` zählt `host` |
| Logo      | 3 | blauer Kreis `#1F5FBF`, weißes großes K, goldener Stern (nicht für Partner-Mails) |
| Signatur  | 4 | Name + Abteilung + Code müssen exakt zum Mitarbeiterverzeichnis passen (nicht für Partner) |
| Anhang    | 5 | nur .pdf .docx .xlsx .png; keine Doppel-Endungen, Makros, Programme, Archive, HTML |

Punkte (`GameSession`-Konstanten): richtig gemeldet +10 · harmlos abgelegt +10 · falsches Merkmal −5
(Mail bleibt offen) · Phishing abgelegt −20 + Verwarnung · unbearbeitet bei Feierabend −5.

Schwierigkeit (`Difficulty`): Leicht 4+ Mails, 2–3 Fehler, 20 % subtil, 7 Min/Tag · Mittel 6+ Mails,
1–2 Fehler, 50 % subtil, 6 Min/Tag · Schwer 8+ Mails, 1 Fehler, immer subtil, 5 Min/Tag; je subtiler,
desto öfter basiert eine Phishing-Mail auf einer harmlos klingenden Vorlage.

## Erweitern

- **Neue Mail-Vorlage:** Eintrag in `MailTemplates.LEGIT_INTERNAL`, `LEGIT_PARTNER`, `LURES` oder
  `PARTNER_LURES`. Platzhalter `{player}` und `{link}`; Signierender wird über `department` gewählt.
- **Neue Fehlerart:** ein `Variant` in `Manipulations.ALL`; ggf. passende Erklärung in
  `RuleChecker.explain…()` ergänzen. Der Katalogtest läuft automatisch über die neue Variante.
- **Neues Merkmal:** `Feature`-Enum erweitern, `RuleChecker` (Prüfung + 3 Textmethoden),
  Varianten in `Manipulations`, ggf. `Manipulations.appliesTo()` für Partner-Mails. Der Prüf-Button in
  der `MailAppView`-Werkzeugleiste entsteht automatisch aus dem Enum.
- **Mitarbeiter/Partner/Logo ändern:** nur `CorporateGuidelines`. Persönliche Mails (Vorlage ohne
  `localPart`) kommen automatisch aus dem Postfach der Person, die unterschreibt.
  **Keine Stereotype:** Namen unterschiedlicher Herkunft gehören gleichmäßig in alle Abteilungen und
  Hierarchieebenen (Geschäftsführung, IT-Sicherheit, Recht …) – nie bestimmte Namen gehäuft in
  bestimmte Jobs. Das gilt auch für Absendernamen in Phishing-Vorlagen.
- `MailTemplatesTest` sichert ab: jede Abteilung hat Vorlagen, jede Person taucht im Spiel auf,
  `{link}`/`{player}` stimmen, Codes und Mailadressen sind eindeutig.
- **Chef-Texte:** `ChefMessages`. **Handbuch:** `HandbookView.PAGES`.

## Konventionen

- UI-Texte Deutsch, Code-Bezeichner Englisch, Kommentare Deutsch.
- Records für Daten, keine Setter. `GameState` ist unveränderlich (`withX()`).
- Fehler nie verschlucken: UI zeigt `Alert` (NFR-03), Logik wirft `IllegalStateException`.
- Tests liegen in `src/test/java/.../logic`; jede Regel- oder Punkteänderung braucht einen Test.
- **UI-Prüfung nur off-screen:** `ui/UiSmokeTest` startet das JavaFX-Toolkit ohne Fenster, baut jeden
  Screen in einer unsichtbaren Scene, wendet `styles.css` an und prüft Layout/Textfarben. Neue Views
  dort ergänzen. Das Spiel **nicht** automatisiert starten und durchklicken (keine Maus-/Tastatur-
  Automation auf dem Desktop des Nutzers) – `./mvnw javafx:run` ist für den Menschen. Wer das
  Ergebnis *sehen* will: im Test `root.resize(W, H)` + `layout()`, dann `scene.snapshot(null)` und die
  Pixel als Bild speichern – das rendert ebenfalls komplett ohne Fenster.
- JavaFX-CSS-Falle: nie `-fx-background: transparent` setzen (macht Label-Text weiß); für transparente
  ScrollPanes nur `-fx-background-color` auf `.plain-scroll` und `.viewport`.
- JavaFX-CSS-Falle 2: Aufgeklappte Menüs hängen im CSS-Baum unter ihrem `MenuButton`. Deshalb Button-Text
  nur mit `.menu-button > .label` färben, nie mit `.menu-button .label` (färbt sonst die Menüeinträge mit).

## Offene Punkte (aus der Use-Case-Doku) und aktuelle Antwort des Prototyps

- F-01 spielübergreifendes Punktesystem → noch nicht; Punkte gelten pro Kampagne (5 Tage).
- F-02 Anzahl Schwierigkeitsstufen → 3 (`Difficulty`).
- F-03 endlos vs. feste Mails → feste Vorlagen, zufällig kombiniert; Seed im Spielstand macht einen
  geladenen Tag reproduzierbar.
- Speichern erfolgt nur an Tagesgrenzen (`PhishingApp.dayStartState`), ein laufender Tag startet beim
  Laden neu. Datei: `~/.phishingforcompliments/savegame.properties`.
