package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.PhishingApp;
import com.example.phishingforcompliments.logic.ChefMessages;
import com.example.phishingforcompliments.logic.CorporateGuidelines;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.model.Outcome;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Der "Homescreen" (Skizze 2): Hintergrund mit Hügeln, Icons für Mail, Handbuch,
 * Corporate Guidelines und Tagesanweisung. Unten eine Taskleiste im Windows-Stil
 * ({@link Taskbar}): Start-Menü, ein Button pro offenem (auch minimiertem) Fenster und der
 * Infobereich mit Feierabend, Punkten, Verwarnungen und Uhr.
 */
public class DesktopScreen extends BorderPane {

    static final double MAIL_WINDOW_WIDTH = 960;

    // Programm-Icons in klein (Titelleiste, Taskleiste, Start-Menü) – jeder Ort braucht einen eigenen Node
    private static final Supplier<Node> MAIL_ICON = () -> small(envelopeGraphic(), 18);
    private static final Supplier<Node> HANDBOOK_ICON = () -> small(bookIcon(), 18);
    private static final Supplier<Node> GUIDELINES_ICON = () -> small(paragraphIcon(), 18);
    private static final Supplier<Node> NOTE_ICON = () -> small(noteIcon(), 18);

    private final PhishingApp app;
    private final GameSession session;
    private final String briefing;
    private final Pane windowLayer = new Pane();
    private final Map<String, DesktopWindow> windows = new HashMap<>();
    private final PauseOverlay pauseOverlay;
    private final Taskbar taskbar;
    private final AnimationTimer timer;
    private long lastNanos;

    private final Label clockLabel = new Label("09:00");
    private final ProgressBar dayProgress = new ProgressBar(0);
    private final Label scoreLabel = new Label();
    private final Label warnLabel = new Label();
    private final Label mailBadge = new Label();
    private MailAppView mailApp;

    public DesktopScreen(PhishingApp app, GameSession session, String briefing) {
        this.app = app;
        this.session = session;
        this.briefing = briefing;

        windowLayer.setPickOnBounds(false);
        StackPane desktopArea = new StackPane(buildWallpaper(), buildIcons(), windowLayer);
        pauseOverlay = new PauseOverlay(() -> setPaused(false));
        StackPane stack = new StackPane(desktopArea, pauseOverlay);

        setCenter(stack);
        taskbar = new Taskbar(windowLayer, buildStartMenu(), buildTray());
        setBottom(taskbar);

        session.setOnTimeOver(this::onTimeOver);
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastNanos == 0 ? 0 : (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                session.tick(dt);
                refreshStatus();
            }
        };
        timer.start();

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                setPaused(!session.isPaused());
                e.consume();
            }
        });
        setFocusTraversable(true);

        refreshStatus();
        Platform.runLater(() -> {
            openChefMessage("Tagesanweisung", briefing, "Verstanden", null);
            requestFocus();
        });
    }

    public void dispose() {
        timer.stop();
    }

    Taskbar taskbar() {
        return taskbar;
    }

    // ------------------------------------------------------------------
    // Aufbau
    // ------------------------------------------------------------------

    private Node buildWallpaper() {
        Ellipse hillBack = new Ellipse();
        hillBack.setFill(Color.web("#6DBE6B"));
        Ellipse hillFront = new Ellipse();
        hillFront.setFill(Color.web("#4CAF50"));
        Ellipse hillRight = new Ellipse();
        hillRight.setFill(Color.web("#5BB75B"));
        Ellipse cloud1 = cloud();
        Ellipse cloud2 = cloud();
        Ellipse cloud3 = cloud();

        Pane wallpaper = new Pane(cloud1, cloud2, cloud3, hillBack, hillRight, hillFront) {
            @Override
            protected void layoutChildren() {
                double w = getWidth();
                double h = getHeight();
                hillBack.setCenterX(w * 0.25);
                hillBack.setCenterY(h * 1.05);
                hillBack.setRadiusX(w * 0.55);
                hillBack.setRadiusY(h * 0.45);
                hillRight.setCenterX(w * 0.85);
                hillRight.setCenterY(h * 1.1);
                hillRight.setRadiusX(w * 0.45);
                hillRight.setRadiusY(h * 0.42);
                hillFront.setCenterX(w * 0.5);
                hillFront.setCenterY(h * 1.2);
                hillFront.setRadiusX(w * 0.7);
                hillFront.setRadiusY(h * 0.4);
                cloud1.setCenterX(w * 0.2);
                cloud1.setCenterY(h * 0.14);
                cloud2.setCenterX(w * 0.55);
                cloud2.setCenterY(h * 0.08);
                cloud3.setCenterX(w * 0.8);
                cloud3.setCenterY(h * 0.2);
            }
        };
        wallpaper.getStyleClass().add("wallpaper");
        return wallpaper;
    }

    private static Ellipse cloud() {
        Ellipse e = new Ellipse(90, 28);
        e.setFill(Color.rgb(255, 255, 255, 0.75));
        return e;
    }

    private Node buildIcons() {
        VBox icons = new VBox(22,
                icon("E-Mail", envelopeIcon(), this::openMailApp),
                icon("Handbuch", bookIcon(), this::openHandbook),
                icon("Corporate\nGuidelines", paragraphIcon(), this::openGuidelines),
                icon("Anweisung", noteIcon(), this::openBriefing));
        icons.setPadding(new Insets(24));
        icons.setAlignment(Pos.TOP_CENTER);
        icons.setFillWidth(false);
        icons.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        icons.setPickOnBounds(false);
        StackPane wrapper = new StackPane(icons);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setPickOnBounds(false);
        return wrapper;
    }

    private Node icon(String label, Node graphic, Runnable onClick) {
        Label text = new Label(label);
        text.getStyleClass().add("desktop-icon-label");
        VBox box = new VBox(6, graphic, text);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(96);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.getStyleClass().add("desktop-icon");
        box.setOnMouseClicked(e -> onClick.run());
        return box;
    }

    private Node envelopeIcon() {
        mailBadge.getStyleClass().add("badge");
        StackPane.setAlignment(mailBadge, Pos.TOP_RIGHT);
        mailBadge.setTranslateX(10);
        mailBadge.setTranslateY(-10);
        StackPane withBadge = new StackPane(envelopeGraphic(), mailBadge);
        withBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return withBadge;
    }

    private static Node envelopeGraphic() {
        Rectangle env = new Rectangle(56, 38, Color.WHITE);
        env.setStroke(Color.web("#1F5FBF"));
        env.setStrokeWidth(3);
        env.setArcWidth(6);
        env.setArcHeight(6);
        Polyline flap = new Polyline(0, 0, 28, 20, 56, 0);
        flap.setStroke(Color.web("#1F5FBF"));
        flap.setStrokeWidth(3);
        StackPane envelope = new StackPane(env, flap);
        StackPane.setAlignment(flap, Pos.TOP_CENTER);
        return envelope;
    }

    private static Node bookIcon() {
        Rectangle cover = new Rectangle(46, 56, Color.web("#5D2A1A"));
        cover.setArcWidth(6);
        cover.setArcHeight(6);
        Rectangle spine = new Rectangle(8, 56, Color.web("#3E1A0F"));
        Line l1 = new Line(0, 0, 24, 0);
        Line l2 = new Line(0, 0, 24, 0);
        l1.setStroke(Color.web("#D7B98E"));
        l2.setStroke(Color.web("#D7B98E"));
        l1.setStrokeWidth(2);
        l2.setStrokeWidth(2);
        VBox lines = new VBox(5, l1, l2);
        lines.setAlignment(Pos.CENTER);
        StackPane book = new StackPane(cover, spine, lines);
        StackPane.setAlignment(spine, Pos.CENTER_LEFT);
        return book;
    }

    private static Node paragraphIcon() {
        Rectangle bg = new Rectangle(52, 56, Color.WHITE);
        bg.setStroke(Color.web("#C62828"));
        bg.setStrokeWidth(3);
        bg.setArcWidth(8);
        bg.setArcHeight(8);
        Text p = new Text("§");
        p.setFill(Color.web("#C62828"));
        p.setFont(Font.font(null, FontWeight.BOLD, 36));
        return new StackPane(bg, p);
    }

    private static Node noteIcon() {
        Rectangle note = new Rectangle(54, 54, Color.web("#FFF176"));
        note.setStroke(Color.web("#C9B037"));
        VBox lines = new VBox(7);
        lines.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Line l = new Line(0, 0, 34, 0);
            l.setStroke(Color.web("#9E9D24"));
            l.setStrokeWidth(2);
            lines.getChildren().add(l);
        }
        return new StackPane(note, lines);
    }

    /** Verkleinert ein Desktop-Icon (ca. 56 px) auf {@code size} Pixel. */
    private static Node small(Node icon, double size) {
        double scale = size / 56.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        StackPane box = new StackPane(new Group(icon));
        box.setPrefSize(size, size);
        box.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMouseTransparent(true);
        return box;
    }

    /** Start-Menü (links in der Taskleiste): Programme und Spiel-Funktionen. */
    private MenuButton buildStartMenu() {
        MenuButton start = new MenuButton("Start", new LogoView(CorporateGuidelines.OFFICIAL_LOGO, 22));
        start.getStyleClass().add("start-button");
        start.setPopupSide(Side.TOP);
        start.setFocusTraversable(false);
        start.setMinWidth(Region.USE_PREF_SIZE);

        MenuItem mail = new MenuItem("E-Mail", MAIL_ICON.get());
        mail.setOnAction(e -> openMailApp());
        MenuItem handbook = new MenuItem("Handbuch", HANDBOOK_ICON.get());
        handbook.setOnAction(e -> openHandbook());
        MenuItem guidelines = new MenuItem("Corporate Guidelines", GUIDELINES_ICON.get());
        guidelines.setOnAction(e -> openGuidelines());
        MenuItem note = new MenuItem("Tagesanweisung", NOTE_ICON.get());
        note.setOnAction(e -> openBriefing());

        MenuItem pause = new MenuItem("Pause (Esc)");
        pause.setOnAction(e -> setPaused(true));
        MenuItem save = new MenuItem("Spielstand speichern");
        save.setOnAction(e -> {
            if (app.saveQuietly()) {
                info("Gespeichert", "Spielstand gespeichert (Stand: Beginn von Tag " + session.state().day() + ").");
            }
        });
        MenuItem toTitle = new MenuItem("Zum Hauptmenü");
        toTitle.setOnAction(e -> {
            if (confirm("Zum Hauptmenü?", "Der laufende Tag geht verloren. Gespeichert ist der Stand vom Tagesbeginn.")) {
                app.showTitle();
            }
        });
        MenuItem quit = new MenuItem("Speichern & Beenden");
        quit.setOnAction(e -> app.saveAndExit());

        start.getItems().addAll(mail, handbook, guidelines, note, new SeparatorMenuItem(),
                pause, save, new SeparatorMenuItem(), toTitle, quit);
        return start;
    }

    /** Infobereich rechts in der Taskleiste: Feierabend, Punkte, Verwarnungen, Tagesfortschritt, Uhr + Tag. */
    private Node buildTray() {
        Button endDay = new Button("Feierabend");
        endDay.getStyleClass().add("btn-end-day");
        endDay.setFocusTraversable(false);
        endDay.setOnAction(e -> requestEndDay());

        scoreLabel.getStyleClass().add("taskbar-text");
        warnLabel.getStyleClass().add("taskbar-text");
        dayProgress.setPrefWidth(90);
        Tooltip.install(dayProgress, new Tooltip("Arbeitstag 09:00 – 17:00"));

        Label dayLabel = new Label("Tag " + session.state().day() + " / " + GameState.CAMPAIGN_DAYS);
        dayLabel.getStyleClass().add("taskbar-date");
        clockLabel.getStyleClass().add("taskbar-clock");
        VBox clock = new VBox(-1, clockLabel, dayLabel);
        clock.setAlignment(Pos.CENTER);
        clock.getStyleClass().add("tray-clock");

        HBox tray = new HBox(16, endDay, scoreLabel, warnLabel, dayProgress, clock);
        tray.getStyleClass().add("tray");
        tray.setAlignment(Pos.CENTER_RIGHT);
        tray.setPadding(new Insets(0, 10, 0, 8));
        for (Node n : tray.getChildren()) {
            if (n instanceof Region r) {
                r.setMinWidth(Region.USE_PREF_SIZE);
            }
        }
        tray.setMinWidth(Region.USE_PREF_SIZE);
        return tray;
    }

    // ------------------------------------------------------------------
    // Fenster
    // ------------------------------------------------------------------

    /** Öffnet ein Programmfenster – oder holt das schon offene (auch minimierte) nach vorne. */
    private DesktopWindow open(String key, String title, Supplier<Node> icon, Supplier<Node> content,
                               double w, double h, double x, double y) {
        DesktopWindow existing = windows.get(key);
        if (existing != null) {
            existing.restore();
            return existing;
        }
        DesktopWindow win = new DesktopWindow(title, content.get(), w, h, null).setIcon(icon);
        win.relocate(x, y);
        addWindow(key, win);
        return win;
    }

    private void addWindow(String key, DesktopWindow win) {
        windows.put(key, win);
        win.addCloseListener(() -> windows.remove(key, win));
        windowLayer.getChildren().add(win);
        taskbar.track(win);
    }

    private void openMailApp() {
        if (mailApp == null) {
            mailApp = new MailAppView(session, this::onDecision);
        } else {
            mailApp.refreshList();
        }
        open("mail", "E-Mail – " + session.state().playerName(), MAIL_ICON, () -> mailApp,
                MAIL_WINDOW_WIDTH, 600, 140, 20);
    }

    private void openHandbook() {
        open("handbook", "Handbuch", HANDBOOK_ICON, HandbookView::new, 620, 460, 180, 60);
    }

    private void openGuidelines() {
        open("guidelines", "Corporate Guidelines", GUIDELINES_ICON,
                () -> new GuidelinesView(session.state().day()), 760, 560, 240, 40);
    }

    private void openBriefing() {
        openChefMessage("Tagesanweisung", briefing, "Schließen", null);
    }

    private DesktopWindow openChefMessage(String heading, String text, String buttonText, Runnable afterClose) {
        String key = "chef-" + heading;
        DesktopWindow old = windows.get(key);
        if (old != null) {
            old.close();
        }
        DesktopWindow[] holder = new DesktopWindow[1];
        ChefMessageView view = new ChefMessageView(heading, text, buttonText, () -> {
            holder[0].close();
            if (afterClose != null) {
                afterClose.run();
            }
        });
        // Kurzer Titel, damit er in der Taskleiste lesbar bleibt; die Absenderin steht in der Nachricht
        holder[0] = new DesktopWindow(heading, view, 520, 420, null).setIcon(NOTE_ICON);
        holder[0].relocate(330, 90);
        addWindow(key, holder[0]);
        return holder[0];
    }

    // ------------------------------------------------------------------
    // Spielereignisse
    // ------------------------------------------------------------------

    private void onDecision(DecisionResult result) {
        refreshStatus();
        if (result.outcome() == Outcome.FILED_WRONG) {
            boolean fired = session.state().isFired();
            session.setPaused(true);
            // Die Verwarnung hält das Spiel an – darum nicht minimierbar (sonst stünde die Uhr unsichtbar still)
            openChefMessage("Verwarnung", ChefMessages.warning(session.state(), result),
                    fired ? "Schreibtisch räumen" : "Verstanden",
                    () -> {
                        session.setPaused(false);
                        if (fired) {
                            app.endDay();
                        }
                    }).setMinimizable(false);
        }
    }

    private void onTimeOver() {
        timer.stop();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Es ist 17:00 Uhr – Feierabend! Offene Mails zählen als nicht bearbeitet.");
        alert.setHeaderText("Feierabend");
        alert.showAndWait();
        app.endDay();
    }

    private void requestEndDay() {
        int open = session.openMails().size();
        if (open > 0 && !confirm("Schon Feierabend?",
                "Es sind noch " + open + " Mails offen. Jede unbearbeitete Mail kostet "
                        + Math.abs(GameSession.POINTS_UNPROCESSED) + " Punkte.")) {
            return;
        }
        app.endDay();
    }

    private void setPaused(boolean paused) {
        session.setPaused(paused);
        pauseOverlay.setVisible(paused);
        if (!paused) {
            requestFocus();
        }
    }

    private void refreshStatus() {
        clockLabel.setText(session.clockText());
        dayProgress.setProgress(session.dayProgress());
        scoreLabel.setText("Punkte: " + session.state().score());
        warnLabel.setText("Verwarnungen: " + session.state().warnings() + " / " + GameState.MAX_WARNINGS);
        long unread = session.unreadCount();
        mailBadge.setText(Long.toString(unread));
        mailBadge.setVisible(unread > 0);
    }

    private boolean confirm(String header, String text) {
        session.setPaused(true);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(header);
        boolean yes = alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
        if (pauseOverlay.isVisible()) {
            return yes;
        }
        session.setPaused(false);
        return yes;
    }

    private void info(String header, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
