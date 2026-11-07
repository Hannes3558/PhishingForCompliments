package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.PhishingApp;
import com.example.phishingforcompliments.logic.ChefMessages;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.model.Outcome;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Der "Homescreen" (Skizze 2): Hintergrund mit Hügeln, Icons für Mail, Handbuch,
 * Corporate Guidelines und Tagesanweisung, Taskleiste mit Uhr, Punkten und Feierabend.
 */
public class DesktopScreen extends BorderPane {

    static final double MAIL_WINDOW_WIDTH = 960;

    private final PhishingApp app;
    private final GameSession session;
    private final String briefing;
    private final Pane windowLayer = new Pane();
    private final Map<String, DesktopWindow> windows = new HashMap<>();
    private final PauseOverlay pauseOverlay;
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
        setBottom(buildTaskbar());

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
                icon("E-Mail", envelopeIcon(), () -> openMailApp()),
                icon("Handbuch", bookIcon(), () -> open("handbook", "Handbuch", HandbookView::new, 620, 460, 180, 60)),
                icon("Corporate\nGuidelines", paragraphIcon(),
                        () -> open("guidelines", "Corporate Guidelines", () -> new GuidelinesView(session.state().day()), 760, 560, 240, 40)),
                icon("Anweisung", noteIcon(), () -> openChefMessage("Tagesanweisung", briefing, "Schließen", null)));
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

        mailBadge.getStyleClass().add("badge");
        StackPane.setAlignment(mailBadge, Pos.TOP_RIGHT);
        mailBadge.setTranslateX(10);
        mailBadge.setTranslateY(-10);
        StackPane withBadge = new StackPane(envelope, mailBadge);
        withBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return withBadge;
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
        p.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");
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

    private Node buildTaskbar() {
        MenuButton menu = new MenuButton("☰  Menü");
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
        menu.getItems().addAll(pause, save, new SeparatorMenuItem(), toTitle, quit);

        Label dayLabel = new Label("Tag " + session.state().day() + " / " + GameState.CAMPAIGN_DAYS);
        dayLabel.getStyleClass().add("taskbar-text");
        clockLabel.getStyleClass().add("taskbar-clock");
        dayProgress.setPrefWidth(120);
        scoreLabel.getStyleClass().add("taskbar-text");
        warnLabel.getStyleClass().add("taskbar-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button endDay = new Button("Feierabend");
        endDay.getStyleClass().add("btn-end-day");
        endDay.setOnAction(e -> requestEndDay());

        HBox bar = new HBox(18, menu, dayLabel, clockLabel, dayProgress, scoreLabel, warnLabel, spacer, endDay);
        bar.getStyleClass().add("taskbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));
        return bar;
    }

    // ------------------------------------------------------------------
    // Fenster
    // ------------------------------------------------------------------

    private DesktopWindow open(String key, String title, Supplier<Node> content,
                               double w, double h, double x, double y) {
        DesktopWindow existing = windows.get(key);
        if (existing != null) {
            existing.toFront();
            return existing;
        }
        DesktopWindow win = new DesktopWindow(title, content.get(), w, h, () -> windows.remove(key));
        win.relocate(x, y);
        windows.put(key, win);
        windowLayer.getChildren().add(win);
        return win;
    }

    private void openMailApp() {
        if (mailApp == null) {
            mailApp = new MailAppView(session, this::onDecision);
        } else {
            mailApp.refreshList();
        }
        open("mail", "E-Mail – " + session.state().playerName(), () -> mailApp, MAIL_WINDOW_WIDTH, 600, 140, 20);
    }

    private void openChefMessage(String heading, String text, String buttonText, Runnable afterClose) {
        String key = "chef-" + heading;
        DesktopWindow old = windows.remove(key);
        if (old != null) {
            old.close(null);
        }
        DesktopWindow[] holder = new DesktopWindow[1];
        ChefMessageView view = new ChefMessageView(heading, text, buttonText, () -> {
            holder[0].close(null);
            windows.remove(key);
            if (afterClose != null) {
                afterClose.run();
            }
        });
        holder[0] = new DesktopWindow("Nachricht von " + ChefMessages.BOSS_NAME, view, 520, 420,
                () -> windows.remove(key));
        holder[0].relocate(330, 90);
        windows.put(key, holder[0]);
        windowLayer.getChildren().add(holder[0]);
    }

    // ------------------------------------------------------------------
    // Spielereignisse
    // ------------------------------------------------------------------

    private void onDecision(DecisionResult result) {
        refreshStatus();
        if (result.outcome() == Outcome.FILED_WRONG) {
            boolean fired = session.state().isFired();
            session.setPaused(true);
            openChefMessage("Verwarnung", ChefMessages.warning(session.state(), result),
                    fired ? "Schreibtisch räumen" : "Verstanden",
                    () -> {
                        session.setPaused(false);
                        if (fired) {
                            app.endDay();
                        }
                    });
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
