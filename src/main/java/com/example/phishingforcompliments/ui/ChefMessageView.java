package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.logic.ChefMessages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

/** Nachricht der Chefin (Tagesanweisung oder Verwarnung). */
public class ChefMessageView extends VBox {

    public ChefMessageView(String heading, String message, String buttonText, Runnable onConfirm) {
        setSpacing(10);
        setPadding(new Insets(14));
        getStyleClass().add("chef-note");

        Circle avatar = new Circle(22, Color.web("#8E44AD"));
        Text initials = new Text(initials(ChefMessages.BOSS_NAME));
        initials.setFill(Color.WHITE);
        initials.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        StackPane avatarPane = new StackPane(avatar, initials);

        Label name = new Label(ChefMessages.BOSS_NAME);
        name.getStyleClass().add("bold");
        Label title = new Label(ChefMessages.BOSS_TITLE + " · " + heading);
        title.getStyleClass().add("muted");
        VBox who = new VBox(2, name, title);
        HBox header = new HBox(12, avatarPane, who);
        header.setAlignment(Pos.CENTER_LEFT);

        Label body = new Label(message);
        body.setWrapText(true);
        body.getStyleClass().add("chef-text");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button ok = new Button(buttonText);
        ok.getStyleClass().add("btn-primary");
        ok.setOnAction(e -> onConfirm.run());
        HBox buttons = new HBox(ok);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, new Separator(), scroll, buttons);
    }

    /** "Dr. Sabine Vogt" → "SV" (Titel wie "Dr." werden übersprungen). */
    static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.endsWith(".") && !part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.length() <= 2 ? sb.toString() : "" + sb.charAt(0) + sb.charAt(sb.length() - 1);
    }
}
