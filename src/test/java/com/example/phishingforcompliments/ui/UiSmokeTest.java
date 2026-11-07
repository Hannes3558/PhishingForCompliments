package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.PhishingApp;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.model.DayReport;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.model.Outcome;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Baut alle Screens off-screen (kein Fenster, kein Fokus) und prüft Layout, CSS und Abläufe.
 * Klicks werden als synthetische MouseEvents direkt an die Nodes geschickt – es wird nichts
 * auf dem echten Desktop bewegt oder angezeigt.
 */
class UiSmokeTest {

    private static final double W = PhishingApp.WIDTH;
    private static final double H = PhishingApp.HEIGHT;
    private static String css;

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        assertTrue(ready.await(20, TimeUnit.SECONDS), "JavaFX-Toolkit startet nicht");
        Platform.setImplicitExit(false);
        css = Objects.requireNonNull(PhishingApp.class.getResource("styles.css")).toExternalForm();
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
    }

    // ------------------------------------------------------------------
    // Hilfen
    // ------------------------------------------------------------------

    private static <T> T onFx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    /** Scene ohne Stage: CSS anwenden und Layout berechnen, ohne etwas anzuzeigen. */
    private static Scene layout(Parent root) {
        Scene scene = new Scene(root, W, H);
        scene.getStylesheets().add(css);
        relayout(root);
        return scene;
    }

    private static void relayout(Parent root) {
        root.applyCss();
        root.resize(W, H);
        root.layout();
    }

    /** Schickt Drücken/Loslassen/Klick der linken Maustaste an die Mitte des Nodes. */
    private static void click(Node node) {
        Bounds b = node.localToScene(node.getBoundsInLocal());
        double x = b.getCenterX();
        double y = b.getCenterY();
        List<EventType<MouseEvent>> types = List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED);
        for (EventType<MouseEvent> type : types) {
            Event.fireEvent(node, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1,
                    false, false, false, false, type == MouseEvent.MOUSE_PRESSED, false, false,
                    false, false, true, null));
        }
    }

    private static GameSession startedSession(int day) {
        GameSession s = new GameSession(new GameState("Alex", Difficulty.MITTEL, day, 0, 0, 42L));
        s.startDay();
        return s;
    }

    private static Color textColor(Node node) {
        return (Color) ((Label) node).getTextFill();
    }

    @SuppressWarnings("unchecked")
    private static ListView<Email> mailList(Parent root) {
        return (ListView<Email>) root.lookup(".mail-list");
    }

    private static Button inspectButton(Parent root, Feature f) {
        return root.lookupAll(".inspect-button").stream().map(Button.class::cast)
                .filter(b -> b.getUserData() == f).findFirst().orElseThrow();
    }

    private static Button buttonWithText(Parent root, String text) {
        return root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> text.equals(b.getText())).findFirst().orElseThrow(() -> new AssertionError("Button fehlt: " + text));
    }

    private static Label visibleCaution(Parent root) {
        Node n = root.lookup(".caution");
        return n != null && n.isVisible() ? (Label) n : null;
    }

    private static Node desktopIcon(Parent root, String label) {
        return root.lookupAll(".desktop-icon").stream()
                .filter(icon -> icon.lookupAll(".desktop-icon-label").stream()
                        .anyMatch(l -> label.equals(((Label) l).getText())))
                .findFirst().orElseThrow(() -> new AssertionError("Desktop-Icon fehlt: " + label));
    }

    private static List<Button> taskButtons(Parent root) {
        return root.lookupAll(".task-button").stream().map(Button.class::cast).toList();
    }

    private static boolean has(Node node, String pseudoClass) {
        return node.getPseudoClassStates().contains(PseudoClass.getPseudoClass(pseudoClass));
    }

    private static void fireStartMenu(Parent root, String item) {
        MenuButton start = (MenuButton) root.lookup(".start-button");
        start.getItems().stream().filter(i -> item.equals(i.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("Start-Menü-Eintrag fehlt: " + item)).fire();
    }

    private static void assertNotCutOff(Region r, String what) {
        assertTrue(r.getWidth() >= r.prefWidth(-1) - 0.5, what + " ist abgeschnitten");
    }

    /**
     * Baut einen Desktop und wartet, bis die Tagesanweisung (per Platform.runLater) offen ist:
     * das zweite onFx läuft erst nach allem, was der Konstruktor eingereiht hat.
     */
    private static DesktopScreen desktopWithBriefing(int day) throws Exception {
        DesktopScreen desktop = onFx(() -> {
            DesktopScreen d = new DesktopScreen(new PhishingApp(), startedSession(day), "Hallo");
            layout(d);
            return d;
        });
        onFx(() -> {
            relayout(desktop);
            return null;
        });
        return desktop;
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void titleScreenShowsFullTitleAndCompactCard() throws Exception {
        onFx(() -> {
            TitleScreen title = new TitleScreen(new PhishingApp(), false);
            layout(title);
            Label titleLabel = (Label) title.lookup(".title-text");
            assertNotNull(titleLabel);
            assertTrue(titleLabel.getWidth() >= titleLabel.prefWidth(-1) - 0.5,
                    "Titel darf nicht abgeschnitten werden");
            Node card = title.lookup(".title-card");
            assertTrue(card.getLayoutBounds().getHeight() < H - 40, "Karte soll nicht die ganze Höhe füllen");
            return null;
        });
    }

    @Test
    void desktopIconsSitInAColumnTopLeft() throws Exception {
        onFx(() -> {
            GameSession session = startedSession(1);
            DesktopScreen desktop = new DesktopScreen(new PhishingApp(), session, "Hallo");
            try {
                layout(desktop);
                Set<Node> icons = desktop.lookupAll(".desktop-icon");
                assertEquals(4, icons.size());
                List<Bounds> bounds = new ArrayList<>();
                for (Node icon : icons) {
                    bounds.add(icon.localToScene(icon.getBoundsInLocal()));
                }
                bounds.sort((a, b) -> Double.compare(a.getMinY(), b.getMinY()));
                for (Bounds b : bounds) {
                    assertTrue(b.getMinX() < 60, "Icon links am Rand erwartet, war x=" + b.getMinX());
                    assertTrue(b.getWidth() < 140, "Icon darf nicht gestreckt sein, Breite=" + b.getWidth());
                }
                for (int i = 1; i < bounds.size(); i++) {
                    assertTrue(bounds.get(i).getMinY() >= bounds.get(i - 1).getMaxY() - 1, "Icons untereinander");
                }
                Node badge = desktop.lookup(".badge");
                Bounds badgeBounds = badge.localToScene(badge.getBoundsInLocal());
                assertTrue(badgeBounds.getMinX() < 160, "Badge gehört ans Mail-Icon, nicht an den Fensterrand");
            } finally {
                desktop.dispose();
            }
            return null;
        });
    }

    @Test
    void taskbarMenuButtonIsWhiteOnDark() throws Exception {
        onFx(() -> {
            GameSession session = startedSession(1);
            DesktopScreen desktop = new DesktopScreen(new PhishingApp(), session, "Hallo");
            try {
                layout(desktop);
                MenuButton menu = (MenuButton) desktop.lookup(".menu-button");
                Label own = menu.getChildrenUnmodifiable().stream().filter(Label.class::isInstance)
                        .map(Label.class::cast).findFirst().orElseThrow();
                assertTrue(textColor(own).getBrightness() > 0.9, "Menü-Button-Text soll weiß sein");
            } finally {
                desktop.dispose();
            }
            return null;
        });
    }

    @Test
    void windowsMinimizeToTheTaskbarAndComeBack() throws Exception {
        DesktopScreen desktop = desktopWithBriefing(1);
        onFx(() -> {
            try {
                Taskbar taskbar = desktop.taskbar();
                assertEquals(1, taskButtons(desktop).size(), "Tagesanweisung gehört in die Taskleiste");
                DesktopWindow briefing = taskbar.activeWindow();
                assertNotNull(briefing);
                assertTrue(has(briefing, "active"));

                click(desktopIcon(desktop, "E-Mail"));
                relayout(desktop);
                DesktopWindow mail = taskbar.activeWindow();
                assertTrue(mail.getTitle().startsWith("E-Mail"), mail.getTitle());
                assertEquals(2, taskButtons(desktop).size());
                assertTrue(has(taskbar.buttonFor(mail), "active"));
                assertFalse(has(taskbar.buttonFor(briefing), "active"), "nur ein Fenster ist im Vordergrund");
                assertFalse(has(briefing, "active"));

                // Minimieren über die Titelleiste: Fenster weg, Button bleibt (gedimmt)
                click(mail.lookup(".window-minimize"));
                relayout(desktop);
                assertTrue(mail.isMinimized());
                assertFalse(mail.isVisible());
                assertEquals(2, taskButtons(desktop).size());
                assertTrue(has(taskbar.buttonFor(mail), "minimized"));
                assertSame(briefing, taskbar.activeWindow(), "nächstes Fenster rückt in den Vordergrund");

                // Taskleisten-Button: wiederherstellen, beim zweiten Klick (im Vordergrund) minimieren
                click(taskbar.buttonFor(mail));
                assertTrue(mail.isVisible());
                assertSame(mail, taskbar.activeWindow());
                click(taskbar.buttonFor(mail));
                assertTrue(mail.isMinimized());

                // Desktop-Icon holt das minimierte Fenster zurück statt ein zweites zu öffnen
                click(desktopIcon(desktop, "E-Mail"));
                assertFalse(mail.isMinimized());
                assertEquals(2, taskButtons(desktop).size());

                // Verdecktes Fenster per Taskleiste nach vorne holen
                click(taskbar.buttonFor(briefing));
                assertSame(briefing, taskbar.activeWindow());
                assertFalse(briefing.isMinimized());

                // "Desktop anzeigen": alles minimieren, zweiter Klick stellt die alte Reihenfolge her
                click(desktop.lookup(".show-desktop"));
                assertNull(taskbar.activeWindow());
                assertTrue(mail.isMinimized() && briefing.isMinimized());
                click(desktop.lookup(".show-desktop"));
                assertSame(briefing, taskbar.activeWindow());
                assertFalse(mail.isMinimized());

                // Schließen entfernt den Button
                click(briefing.lookup(".window-close"));
                relayout(desktop);
                assertEquals(1, taskButtons(desktop).size());
                assertSame(mail, taskbar.activeWindow());
            } finally {
                desktop.dispose();
            }
            return null;
        });
    }

    @Test
    void startMenuOpensProgramsAndTaskbarLayoutFits() throws Exception {
        DesktopScreen desktop = desktopWithBriefing(3);
        onFx(() -> {
            try {
                for (String program : List.of("E-Mail", "Handbuch", "Corporate Guidelines")) {
                    fireStartMenu(desktop, program);
                }
                fireStartMenu(desktop, "Handbuch"); // schon offen → kein zweites Fenster
                relayout(desktop);
                assertEquals(4, taskButtons(desktop).size());
                assertEquals("Handbuch", desktop.taskbar().activeWindow().getTitle());

                Region bar = desktop.taskbar();
                Bounds barBounds = bar.localToScene(bar.getBoundsInLocal());
                assertTrue(bar.getHeight() >= 40, "Taskleiste zu flach: " + bar.getHeight());
                assertTrue(barBounds.getMaxX() <= W + 0.5);

                MenuButton start = (MenuButton) desktop.lookup(".start-button");
                assertNotCutOff(start, "Start-Button");
                assertNotCutOff(buttonWithText(desktop, "Feierabend"), "Feierabend");
                Node tray = desktop.lookup(".tray");
                Bounds trayBounds = tray.localToScene(tray.getBoundsInLocal());
                for (Node n : tray.lookupAll(".label")) {
                    if (n instanceof Label l && l.getStyleClass().stream().anyMatch(c -> c.startsWith("taskbar-"))) {
                        assertNotCutOff(l, "\"" + l.getText() + "\"");
                    }
                }
                for (Button b : taskButtons(desktop)) {
                    Bounds bb = b.localToScene(b.getBoundsInLocal());
                    assertTrue(bb.getMaxX() <= trayBounds.getMinX() + 0.5, "Task-Button überlappt den Infobereich");
                    assertTrue(b.getWidth() >= 44, "Task-Button zu schmal");
                }
                Node showDesktop = desktop.lookup(".show-desktop");
                Bounds sd = showDesktop.localToScene(showDesktop.getBoundsInLocal());
                assertTrue(sd.getMaxX() >= W - 1, "\"Desktop anzeigen\" gehört an den rechten Rand");
                assertTrue(sd.getHeight() >= bar.getHeight() - 2, "\"Desktop anzeigen\" soll die volle Höhe haben");
            } finally {
                desktop.dispose();
            }
            return null;
        });
    }

    @Test
    void blockingWindowsCannotBeMinimized() throws Exception {
        onFx(() -> {
            DesktopWindow w = new DesktopWindow("Verwarnung", new Label("!"), 300, 200, null).setMinimizable(false);
            layout(new Pane(w));
            w.minimize();
            assertFalse(w.isMinimized());
            assertFalse(w.lookup(".window-minimize").isVisible());
            return null;
        });
    }

    @Test
    void openedMenuItemsAreDarkOnWhite() throws Exception {
        onFx(() -> {
            // Ein aufgeklapptes Menü hängt im CSS-Baum unter seinem MenuButton:
            // .taskbar > .menu-button > .context-menu > .menu-item > .label  – genau so nachgebaut.
            Label item = new Label("Pause (Esc)");
            StackPane menuItem = new StackPane(item);
            menuItem.getStyleClass().add("menu-item");
            StackPane contextMenu = new StackPane(menuItem);
            contextMenu.getStyleClass().add("context-menu");
            StackPane menuButton = new StackPane(contextMenu);
            menuButton.getStyleClass().add("menu-button");
            HBox taskbar = new HBox(menuButton);
            taskbar.getStyleClass().add("taskbar");
            layout(taskbar);

            Color text = textColor(item);
            Color background = (Color) contextMenu.getBackground().getFills().get(0).getFill();
            assertTrue(text.getBrightness() < 0.35, "Menütext soll dunkel sein, war " + text);
            assertTrue(background.getBrightness() > 0.95, "Menühintergrund soll weiß sein, war " + background);
            return null;
        });
    }

    @Test
    void mailDetailHasToolbarDarkTextAndFullWidthButtons() throws Exception {
        onFx(() -> {
            GameSession session = startedSession(5);
            MailAppView mailApp = new MailAppView(session, r -> { });
            DesktopWindow window = new DesktopWindow("E-Mail", mailApp, DesktopScreen.MAIL_WINDOW_WIDTH, 600, null);
            Pane layer = new Pane(window);
            layout(layer);

            ListView<Email> list = mailList(mailApp);
            assertEquals(session.openMails().size(), list.getItems().size());
            relayout(layer);
            boolean horizontalScroll = list.lookupAll(".scroll-bar").stream()
                    .filter(javafx.scene.control.ScrollBar.class::isInstance)
                    .map(javafx.scene.control.ScrollBar.class::cast)
                    .anyMatch(sb -> sb.getOrientation() == javafx.geometry.Orientation.HORIZONTAL && sb.isVisible());
            assertFalse(horizontalScroll, "Posteingang darf nicht seitlich scrollen (lange Namen kürzen)");
            for (Email shown : list.getItems()) {
                list.getSelectionModel().select(shown);
                relayout(layer);

                Label subject = (Label) mailApp.lookup(".mail-subject-big");
                assertNotNull(subject, "Mail-Detail wurde nicht angezeigt");
                assertTrue(textColor(subject).getBrightness() < 0.5, "Betreff muss dunkel sein");

                for (Node n : mailApp.lookupAll(".feature-hot")) {
                    for (Node label : n.lookupAll(".label")) {
                        if (label instanceof Label l && !l.getStyleClass().contains("muted")) {
                            assertTrue(textColor(l).getBrightness() < 0.5, "Label \"" + l.getText() + "\" ist zu hell");
                        }
                    }
                }
                // Direkt anklickbar: nur Absender, Logo, Signatur – nie Links oder Anhänge
                assertEquals(3, mailApp.lookupAll(".feature-hot").size());

                assertEquals(Feature.values().length, mailApp.lookupAll(".inspect-button").size());
                for (Feature f : Feature.values()) {
                    Button b = inspectButton(mailApp, f);
                    boolean present = switch (f) {
                        case LINK -> !shown.links().isEmpty();
                        case ANHANG -> !shown.attachments().isEmpty();
                        default -> true;
                    };
                    assertEquals(!present, b.isDisabled(), "Prüf-Button " + f);
                    assertTrue(b.getWidth() >= b.prefWidth(-1) - 0.5, "Prüf-Button " + f + " gequetscht");
                }
                Button safe = buttonWithText(mailApp, "Als sicher ablegen");
                assertTrue(safe.getWidth() >= safe.prefWidth(-1) - 0.5, "\"Als sicher ablegen\" gequetscht");
                Bounds safeBounds = safe.localToScene(safe.getBoundsInLocal());
                Bounds windowBounds = window.localToScene(window.getBoundsInLocal());
                assertTrue(safeBounds.getMaxX() <= windowBounds.getMaxX(), "Button ragt aus dem Fenster");
                assertEquals(shown.links().size(), mailApp.lookupAll(".hyperlink").size());
            }
            return null;
        });
    }

    @Test
    void clickingLinkOrAttachmentNeverOpensOrReports() throws Exception {
        onFx(() -> {
            GameSession session = startedSession(5);
            List<DecisionResult> decisions = new ArrayList<>();
            MailAppView mailApp = new MailAppView(session, decisions::add);
            layout(mailApp);
            Email withLink = session.openMails().stream()
                    .filter(m -> !m.links().isEmpty() && !m.attachments().isEmpty()).findFirst()
                    .orElseGet(() -> session.openMails().stream().filter(m -> !m.links().isEmpty()).findFirst().orElseThrow());
            mailList(mailApp).getSelectionModel().select(withLink);
            relayout(mailApp);

            assertNull(visibleCaution(mailApp));
            Hyperlink link = (Hyperlink) mailApp.lookup(".hyperlink");
            click(link);
            relayout(mailApp);

            assertTrue(mailApp.lookupAll(".compare-column").isEmpty(), "Link-Klick darf keinen Vergleich öffnen");
            Label caution = visibleCaution(mailApp);
            assertNotNull(caution, "Link-Klick soll warnen");
            assertTrue(caution.getText().contains("nie anklicken"));
            assertFalse(link.isVisited());

            Node chip = mailApp.lookup(".attachment-chip");
            if (chip != null) {
                click(chip);
                relayout(mailApp);
                assertTrue(mailApp.lookupAll(".compare-column").isEmpty(), "Anhang-Klick darf keinen Vergleich öffnen");
                assertTrue(visibleCaution(mailApp).getText().contains("Anhänge nicht öffnen"));
            }
            assertTrue(decisions.isEmpty(), "Klicks auf Link/Anhang dürfen nichts melden");
            assertEquals(0, session.state().score());
            return null;
        });
    }

    @Test
    void reportingADeviationWithMouseClicksClosesTheMail() throws Exception {
        onFx(() -> {
            GameSession session = startedSession(1);
            List<DecisionResult> decisions = new ArrayList<>();
            MailAppView mailApp = new MailAppView(session, decisions::add);
            DesktopWindow window = new DesktopWindow("E-Mail", mailApp, DesktopScreen.MAIL_WINDOW_WIDTH, 600, null);
            Pane layer = new Pane(window);
            layout(layer);

            Email phishing = session.openMails().stream()
                    .filter(m -> session.checker().isPhishing(m, 1)).findFirst().orElseThrow();
            int openBefore = session.openMails().size();
            mailList(mailApp).getSelectionModel().select(phishing);
            relayout(layer);

            click(inspectButton(mailApp, Feature.ABSENDER));
            relayout(layer);
            assertEquals(2, mailApp.lookupAll(".compare-column").size(), "Vergleichsansicht erwartet");

            click(buttonWithText(mailApp, "Abweichung melden"));
            relayout(layer);

            assertEquals(1, decisions.size());
            assertEquals(Outcome.REPORTED_CORRECT, decisions.get(0).outcome());
            assertEquals(openBefore - 1, session.openMails().size());
            assertEquals(GameSession.POINTS_REPORT_CORRECT, session.state().score());
            assertNotNull(mailApp.lookup(".feedback-ok"), "Rückmeldung soll angezeigt werden");
            assertFalse(mailList(mailApp).getItems().contains(phishing));

            // Direkter Klick auf den Absender einer anderen Mail öffnet ebenfalls den Vergleich
            Email other = session.openMails().get(0);
            mailList(mailApp).getSelectionModel().select(other);
            relayout(layer);
            Node senderRegion = mailApp.lookupAll(".feature-hot").iterator().next();
            click(senderRegion);
            relayout(layer);
            assertEquals(2, mailApp.lookupAll(".compare-column").size());
            return null;
        });
    }

    @Test
    void comparisonViewRendersBothColumns() throws Exception {
        onFx(() -> {
            GameSession session = startedSession(3);
            Email email = session.openMails().get(0);
            for (Feature f : Feature.values()) {
                ComparisonView view = new ComparisonView(session.checker(), email, f, () -> { }, () -> { });
                layout(view);
                assertEquals(2, view.lookupAll(".compare-column").size());
                Label arrows = (Label) view.lookup(".compare-arrows");
                assertTrue(arrows.getWidth() >= arrows.prefWidth(-1) - 0.5, "Pfeil darf nicht zu '…' werden");
                for (Node n : view.lookupAll(".compare-text")) {
                    assertFalse(((Label) n).getText().isBlank());
                }
            }
            return null;
        });
    }

    @Test
    void remainingScreensBuildWithoutErrors() throws Exception {
        onFx(() -> {
            layout(new GuidelinesView(2));
            layout(new HandbookView());
            layout(new ChefMessageView("Tagesanweisung", "Text", "OK", () -> { }));
            layout(new PauseOverlay(() -> { }));

            GameSession session = startedSession(1);
            Email phishing = session.openMails().stream()
                    .filter(m -> session.checker().isPhishing(m, 1)).findFirst().orElseThrow();
            session.reportFeature(phishing, Feature.ABSENDER);
            DayReport report = session.endDay();
            DayReportScreen reportScreen = new DayReportScreen(new PhishingApp(), session, report);
            layout(reportScreen);
            assertEquals(report.results().size(),
                    reportScreen.lookupAll(".result-ok").size() + reportScreen.lookupAll(".result-bad").size());
            return null;
        });
    }

    @Test
    void employeeDirectoryCanBeSearched() throws Exception {
        onFx(() -> {
            GuidelinesView view = new GuidelinesView(4);
            layout(view);
            @SuppressWarnings("unchecked")
            javafx.scene.control.TableView<com.example.phishingforcompliments.model.Employee> table =
                    (javafx.scene.control.TableView<com.example.phishingforcompliments.model.Employee>) view.lookup(".table-view");
            javafx.scene.control.TextField search = (javafx.scene.control.TextField) view.lookup(".directory-search");
            assertEquals(com.example.phishingforcompliments.logic.CorporateGuidelines.EMPLOYEES.size(), table.getItems().size());

            search.setText("it-sicherheit");
            assertEquals(3, table.getItems().size());
            search.setText("Q7M2");
            assertEquals(1, table.getItems().size());
            assertEquals("Markus Brandt", table.getItems().get(0).name());
            search.setText("");
            assertEquals(com.example.phishingforcompliments.logic.CorporateGuidelines.EMPLOYEES.size(), table.getItems().size());
            return null;
        });
    }

    @Test
    void bossInitialsSkipTitles() {
        assertEquals("SV", ChefMessageView.initials("Dr. Sabine Vogt"));
        assertEquals("DP", ChefMessageView.initials("Dimitrios Papadopoulos"));
    }

    @Test
    void logoViewDrawsStarOnlyWhenPresent() throws Exception {
        onFx(() -> {
            LogoView with = new LogoView(com.example.phishingforcompliments.logic.CorporateGuidelines.OFFICIAL_LOGO, 64);
            LogoView without = new LogoView(new com.example.phishingforcompliments.model.LogoSpec("#1F5FBF", "K", false, "circle"), 64);
            assertEquals(with.getChildren().size(), without.getChildren().size() + 1);
            assertTrue(with.getChildren().stream().noneMatch(Region.class::isInstance));
            return null;
        });
    }
}
