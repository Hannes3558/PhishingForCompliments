package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.model.LogoSpec;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Zeichnet ein {@link LogoSpec} (Kreis/Quadrat, Buchstabe, Stern) – für Mails und Guidelines. */
public class LogoView extends StackPane {

    public LogoView(LogoSpec spec, double size) {
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);
        setAlignment(Pos.CENTER);

        if (spec == null || spec.isNone()) {
            Rectangle placeholder = new Rectangle(size, size);
            placeholder.setFill(Color.TRANSPARENT);
            placeholder.setStroke(Color.web("#9AA4B2"));
            placeholder.getStrokeDashArray().addAll(6.0, 4.0);
            placeholder.setArcWidth(10);
            placeholder.setArcHeight(10);
            Label text = new Label("kein\nLogo");
            text.getStyleClass().add("muted");
            text.setStyle("-fx-font-size: " + Math.max(9, size / 7) + "px; -fx-text-alignment: center;");
            getChildren().addAll(placeholder, text);
            return;
        }

        Color fill;
        try {
            fill = Color.web(spec.colorHex());
        } catch (IllegalArgumentException e) {
            fill = Color.GRAY;
        }
        Shape base;
        if ("circle".equals(spec.shape())) {
            base = new Circle(size / 2, fill);
        } else {
            Rectangle r = new Rectangle(size, size, fill);
            r.setArcWidth(size * 0.18);
            r.setArcHeight(size * 0.18);
            base = r;
        }

        Text letter = new Text(spec.letter());
        letter.setFill(Color.WHITE);
        letter.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.58));

        getChildren().addAll(base, letter);

        if (spec.star()) {
            Polygon star = star(size * 0.16);
            star.setFill(Color.web("#F2C230"));
            star.setStroke(Color.web("#B8860B"));
            star.setStrokeWidth(1);
            StackPane.setAlignment(star, Pos.TOP_RIGHT);
            star.setTranslateX(size * 0.04);
            star.setTranslateY(-size * 0.04);
            getChildren().add(star);
        }
    }

    private static Polygon star(double outerRadius) {
        Polygon p = new Polygon();
        double inner = outerRadius * 0.45;
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outerRadius : inner;
            double angle = Math.PI / 2 + i * Math.PI / 5;
            p.getPoints().addAll(Math.cos(angle) * r, -Math.sin(angle) * r);
        }
        return p;
    }
}
