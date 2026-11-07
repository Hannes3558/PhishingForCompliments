package com.example.phishingforcompliments.model;

/**
 * Beschreibung eines Firmenlogos, so dass die UI es zeichnen und die Logik es vergleichen kann.
 *
 * @param colorHex   Hintergrundfarbe des Kreises, z.B. "#1F5FBF"
 * @param letter     Buchstabe in der Mitte
 * @param star       ob der goldene Stern rechts oben vorhanden ist
 * @param shape      "circle" oder "square"
 */
public record LogoSpec(String colorHex, String letter, boolean star, String shape) {

    public static final LogoSpec NONE = new LogoSpec("", "", false, "none");

    public boolean isNone() {
        return "none".equals(shape);
    }

    public String describe() {
        if (isNone()) {
            return "kein Logo";
        }
        return (shape.equals("circle") ? "Kreis" : "Quadrat")
                + " in " + colorHex
                + ", Buchstabe \"" + letter + "\""
                + (star ? ", mit Stern" : ", ohne Stern");
    }
}
