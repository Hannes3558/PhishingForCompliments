package com.example.phishingforcompliments.model;

import java.util.Arrays;
import java.util.List;

/**
 * Prüfbares Merkmal einer E-Mail (UC-07). Merkmale werden wie bei "Papers, Please"
 * Tag für Tag freigeschaltet: erst ab {@link #introducedOnDay()} gilt die zugehörige Regel.
 */
public enum Feature {
    ABSENDER("Absender", 1, "Die Absenderadresse muss exakt auf @komplimento.de (oder eine Subdomain davon) "
            + "bzw. auf eine Partnerdomain enden – Zeichen für Zeichen."),
    LINK("Link", 2, "Jedes Link-Ziel muss auf komplimento.de oder eine Partnerdomain zeigen. "
            + "Der angezeigte Text zählt nicht – nur das echte Ziel (Maus darüber halten, nicht anklicken!)."),
    LOGO("Logo", 3, "Das Firmenlogo ist ein blauer Kreis mit weißem, großem K und goldenem Stern oben rechts."),
    SIGNATUR("Signatur", 4, "Interne Mails enden mit Name, Abteilung und Mitarbeiter-Code. "
            + "Alles muss exakt zum Mitarbeiterverzeichnis passen – auch ein einzelnes falsches Zeichen zählt."),
    ANHANG("Anhang", 5, "Erlaubt sind nur .pdf, .docx, .xlsx und .png. Keine Programme, Skripte, Archive, "
            + "Makro-Dateien (.docm/.xlsm) oder Doppel-Endungen (.pdf.exe).");

    private final String label;
    private final int introducedOnDay;
    private final String rule;

    Feature(String label, int introducedOnDay, String rule) {
        this.label = label;
        this.introducedOnDay = introducedOnDay;
        this.rule = rule;
    }

    public String label() {
        return label;
    }

    public int introducedOnDay() {
        return introducedOnDay;
    }

    public String rule() {
        return rule;
    }

    public boolean activeOn(int day) {
        return day >= introducedOnDay;
    }

    public static List<Feature> activeFeatures(int day) {
        return Arrays.stream(values()).filter(f -> f.activeOn(day)).toList();
    }

    /** Das Merkmal, das an diesem Tag neu eingeführt wird, oder null. */
    public static Feature introducedOn(int day) {
        return Arrays.stream(values()).filter(f -> f.introducedOnDay == day).findFirst().orElse(null);
    }
}
