package com.example.phishingforcompliments.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Verschiebbares "Programmfenster" auf dem Spiel-Desktop mit Titelleiste und rotem Schließen-Button. */
public class DesktopWindow extends BorderPane {

    private double dragOffsetX;
    private double dragOffsetY;

    public DesktopWindow(String title, Node content, double width, double height, Runnable onClose) {
        getStyleClass().add("desktop-window");
        setPrefSize(width, height);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("window-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("✕");
        close.getStyleClass().add("window-close");
        close.setFocusTraversable(false);
        close.setOnAction(e -> close(onClose));

        HBox bar = new HBox(8, titleLabel, spacer, close);
        bar.getStyleClass().add("window-titlebar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 4, 4, 10));

        bar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX() - getLayoutX();
            dragOffsetY = e.getSceneY() - getLayoutY();
            toFront();
        });
        bar.setOnMouseDragged(e -> {
            double x = e.getSceneX() - dragOffsetX;
            double y = e.getSceneY() - dragOffsetY;
            if (getParent() instanceof Region parent) {
                x = Math.max(0, Math.min(x, parent.getWidth() - getWidth()));
                y = Math.max(0, Math.min(y, parent.getHeight() - getHeight()));
            }
            relocate(x, y);
        });
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> toFront());

        setTop(bar);
        setCenter(content);
    }

    public void close(Runnable onClose) {
        if (getParent() instanceof Pane parent) {
            parent.getChildren().remove(this);
        }
        if (onClose != null) {
            onClose.run();
        }
    }
}
