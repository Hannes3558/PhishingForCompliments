package com.example.phishingforcompliments.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Verschiebbares "Programmfenster" auf dem Spiel-Desktop: Titelleiste mit Icon, Minimieren und
 * rotem Schließen-Button. Minimierte Fenster bleiben offen und werden über die {@link Taskbar}
 * wiederhergestellt.
 */
public class DesktopWindow extends BorderPane {

    /** Das oberste sichtbare Fenster ("im Vordergrund") – setzt die {@link Taskbar}. */
    static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private final String title;
    private final HBox titleBar;
    private final Button minimizeButton;
    private final BooleanProperty minimized = new SimpleBooleanProperty(this, "minimized", false);
    private final List<Runnable> closeListeners = new ArrayList<>();
    private Supplier<Node> iconFactory;
    private boolean closed;
    private double dragOffsetX;
    private double dragOffsetY;

    public DesktopWindow(String title, Node content, double width, double height, Runnable onClose) {
        this.title = title;
        getStyleClass().add("desktop-window");
        setPrefSize(width, height);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (onClose != null) {
            closeListeners.add(onClose);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("window-title");
        titleLabel.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        minimizeButton = new Button("—");
        minimizeButton.getStyleClass().add("window-minimize");
        minimizeButton.setFocusTraversable(false);
        minimizeButton.setTooltip(new Tooltip("Minimieren"));
        minimizeButton.setOnAction(e -> minimize());

        Button close = new Button("✕");
        close.getStyleClass().add("window-close");
        close.setFocusTraversable(false);
        close.setTooltip(new Tooltip("Schließen"));
        close.setOnAction(e -> close());

        titleBar = new HBox(6, titleLabel, spacer, minimizeButton, close);
        titleBar.getStyleClass().add("window-titlebar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(4, 4, 4, 10));

        titleBar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX() - getLayoutX();
            dragOffsetY = e.getSceneY() - getLayoutY();
            toFront();
        });
        titleBar.setOnMouseDragged(e -> {
            double x = e.getSceneX() - dragOffsetX;
            double y = e.getSceneY() - dragOffsetY;
            if (getParent() instanceof Region parent) {
                x = Math.max(0, Math.min(x, parent.getWidth() - getWidth()));
                y = Math.max(0, Math.min(y, parent.getHeight() - getHeight()));
            }
            relocate(x, y);
        });
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> toFront());

        minimized.addListener((obs, was, now) -> setVisible(!now));

        setTop(titleBar);
        setCenter(content);
    }

    public String getTitle() {
        return title;
    }

    /** Kleines Programm-Icon für Titelleiste und Taskleiste (Fabrik, weil jeder Ort einen eigenen Node braucht). */
    public DesktopWindow setIcon(Supplier<Node> factory) {
        if (iconFactory == null && factory != null) {
            titleBar.getChildren().add(0, factory.get());
        }
        iconFactory = factory;
        return this;
    }

    Node createIcon() {
        return iconFactory == null ? null : iconFactory.get();
    }

    /** Fenster, die das Spiel anhalten (z. B. eine Verwarnung), sollen nicht weggeklickt werden können. */
    public DesktopWindow setMinimizable(boolean minimizable) {
        minimizeButton.setVisible(minimizable);
        minimizeButton.setManaged(minimizable);
        if (!minimizable) {
            restore();
        }
        return this;
    }

    public boolean isMinimizable() {
        return minimizeButton.isVisible();
    }

    public void minimize() {
        if (isMinimizable()) {
            minimized.set(true);
        }
    }

    /** Holt das Fenster aus der Taskleiste zurück und legt es nach vorne. */
    public void restore() {
        minimized.set(false);
        toFront();
    }

    public boolean isMinimized() {
        return minimized.get();
    }

    public ReadOnlyBooleanProperty minimizedProperty() {
        return minimized;
    }

    public void addCloseListener(Runnable listener) {
        closeListeners.add(listener);
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (getParent() instanceof Pane parent) {
            parent.getChildren().remove(this);
        }
        for (Runnable listener : List.copyOf(closeListeners)) {
            listener.run();
        }
    }
}
