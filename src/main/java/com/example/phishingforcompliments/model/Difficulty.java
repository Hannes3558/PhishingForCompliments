package com.example.phishingforcompliments.model;

/**
 * Schwierigkeitsgrad einer Partie (UC-04).
 * Steuert Mail-Anzahl, Phishing-Anteil, Anzahl der Manipulationen pro Phishing-Mail,
 * wie subtil die Manipulationen sind und wie lange ein Arbeitstag dauert.
 */
public enum Difficulty {
    LEICHT("Leicht", 4, 0.5, 2, 3, 0.2, 420),
    MITTEL("Mittel", 6, 0.5, 1, 2, 0.5, 360),
    SCHWER("Schwer", 8, 0.6, 1, 1, 1.0, 300);

    private final String label;
    private final int baseMailsPerDay;
    private final double phishingShare;
    private final int minManipulations;
    private final int maxManipulations;
    private final double subtleShare;
    private final int daySeconds;

    Difficulty(String label, int baseMailsPerDay, double phishingShare,
               int minManipulations, int maxManipulations, double subtleShare, int daySeconds) {
        this.label = label;
        this.baseMailsPerDay = baseMailsPerDay;
        this.phishingShare = phishingShare;
        this.minManipulations = minManipulations;
        this.maxManipulations = maxManipulations;
        this.subtleShare = subtleShare;
        this.daySeconds = daySeconds;
    }

    public String label() {
        return label;
    }

    /** Anzahl Mails an einem bestimmten Tag (steigt leicht mit dem Tag). */
    public int mailsForDay(int day) {
        return baseMailsPerDay + Math.max(0, day - 1);
    }

    public double phishingShare() {
        return phishingShare;
    }

    public int minManipulations() {
        return minManipulations;
    }

    public int maxManipulations() {
        return maxManipulations;
    }

    /**
     * Anteil subtiler Manipulationen (0 = alle deutlich, 1 = alle subtil),
     * z.B. "kornplimento.de" statt "k0mplimento-login.ru".
     */
    public double subtleShare() {
        return subtleShare;
    }

    /** Kurzbeschreibung der Fehlerintensität für den Titelbildschirm. */
    public String intensityLabel() {
        if (subtleShare < 0.34) {
            return "überwiegend deutliche";
        }
        return subtleShare < 0.67 ? "gemischte" : "subtile";
    }

    /** Reale Sekunden, die ein Arbeitstag (09:00-17:00) dauert. */
    public int daySeconds() {
        return daySeconds;
    }

    @Override
    public String toString() {
        return label;
    }
}
