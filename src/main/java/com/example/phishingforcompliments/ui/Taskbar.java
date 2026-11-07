package com.example.phishingforcompliments.ui;

import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Taskleiste im Windows-Stil: links das Start-Menü, daneben ein Button pro offenem Fenster,
 * rechts der Infobereich (Feierabend, Punkte, Uhr …) und ganz außen "Desktop anzeigen".
 *
 * <p>Klick auf einen Fenster-Button: minimiertes Fenster wiederherstellen, verdecktes nach vorne
 * holen, das Fenster im Vordergrund minimieren – wie unter Windows.
 */
public class Taskbar extends HBox {

    static final PseudoClass MINIMIZED = PseudoClass.getPseudoClass("minimized");
    static final double TASK_BUTTON_WIDTH = 190;

    private final Pane windowLayer;
    private final HBox taskArea = new HBox(4);
    private final Map<DesktopWindow, Button> buttons = new LinkedHashMap<>();
    private final List<DesktopWindow> hiddenByShowDesktop = new ArrayList<>();

    public Taskbar(Pane windowLayer, Node startButton, Node tray) {
        this.windowLayer = windowLayer;
        getStyleClass().add("taskbar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(0, 0, 0, 6));
        setMinHeight(46);

        taskArea.getStyleClass().add("task-area");
        taskArea.setAlignment(Pos.CENTER_LEFT);
        taskArea.setMinWidth(0);
        HBox.setHgrow(taskArea, Priority.ALWAYS);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(taskArea.widthProperty());
        clip.heightProperty().bind(taskArea.heightProperty());
        taskArea.setClip(clip);

        Button showDesktop = new Button();
        showDesktop.getStyleClass().add("show-desktop");
        showDesktop.setFocusTraversable(false);
        showDesktop.setTooltip(new Tooltip("Desktop anzeigen"));
        showDesktop.setMaxHeight(Double.MAX_VALUE);
        showDesktop.setOnAction(e -> toggleShowDesktop());

        getChildren().addAll(startButton, taskArea, tray, showDesktop);
        // Fenster nach vorne holen ordnet die Kinder um – danach Vordergrund-Markierung neu setzen
        windowLayer.getChildren().addListener((ListChangeListener<Node>) c -> refresh());
    }

    /** Nimmt ein neu geöffnetes Fenster auf; beim Schließen verschwindet sein Button von selbst. */
    public void track(DesktopWindow window) {
        if (buttons.containsKey(window) || window.isClosed()) {
            return;
        }
        Button button = new Button(window.getTitle(), window.createIcon());
        button.getStyleClass().add("task-button");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(window.getTitle()));
        button.setMinWidth(44);
        button.setPrefWidth(TASK_BUTTON_WIDTH);
        button.setMaxWidth(TASK_BUTTON_WIDTH);
        button.setOnAction(e -> activate(window));
        buttons.put(window, button);
        taskArea.getChildren().add(button);
        window.minimizedProperty().addListener((obs, was, now) -> refresh());
        window.addCloseListener(() -> untrack(window));
        refresh();
    }

    private void untrack(DesktopWindow window) {
        Button button = buttons.remove(window);
        if (button != null) {
            taskArea.getChildren().remove(button);
        }
        hiddenByShowDesktop.remove(window);
        refresh();
    }

    /** Verhalten beim Klick auf den Fenster-Button. */
    void activate(DesktopWindow window) {
        hiddenByShowDesktop.clear();
        if (window.isMinimized()) {
            window.restore();
        } else if (window == activeWindow()) {
            window.minimize();
        } else {
            window.toFront();
        }
    }

    /** Das oberste nicht minimierte Fenster, oder {@code null} wenn der Desktop frei ist. */
    public DesktopWindow activeWindow() {
        List<Node> children = windowLayer.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i) instanceof DesktopWindow w && !w.isMinimized()) {
                return w;
            }
        }
        return null;
    }

    Button buttonFor(DesktopWindow window) {
        return buttons.get(window);
    }

    /** "Desktop anzeigen": alles minimieren – ein zweiter Klick holt genau diese Fenster zurück. */
    void toggleShowDesktop() {
        // in Z-Reihenfolge (unten → oben), damit sie danach wieder genauso übereinander liegen
        List<DesktopWindow> visible = windowLayer.getChildren().stream()
                .filter(DesktopWindow.class::isInstance).map(DesktopWindow.class::cast)
                .filter(w -> !w.isMinimized() && w.isMinimizable())
                .toList();
        if (!visible.isEmpty()) {
            visible.forEach(DesktopWindow::minimize);
            hiddenByShowDesktop.clear();
            hiddenByShowDesktop.addAll(visible);
        } else {
            List<DesktopWindow> restore = List.copyOf(hiddenByShowDesktop);
            hiddenByShowDesktop.clear();
            restore.forEach(DesktopWindow::restore);
        }
    }

    private void refresh() {
        DesktopWindow active = activeWindow();
        buttons.forEach((window, button) -> {
            button.pseudoClassStateChanged(DesktopWindow.ACTIVE, window == active);
            button.pseudoClassStateChanged(MINIMIZED, window.isMinimized());
            window.pseudoClassStateChanged(DesktopWindow.ACTIVE, window == active);
        });
    }
}
