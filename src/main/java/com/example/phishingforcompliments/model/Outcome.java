package com.example.phishingforcompliments.model;

/** Ergebnis einer Spielerentscheidung zu einer Mail. */
public enum Outcome {
    /** Abweichung korrekt gemeldet, Mail erledigt. */
    REPORTED_CORRECT,
    /** Merkmal gemeldet, aber es war in Ordnung. Mail bleibt offen. */
    REPORTED_WRONG,
    /** Legitime Mail korrekt als sicher abgelegt. */
    FILED_CORRECT,
    /** Phishing-Mail fälschlich als sicher abgelegt. Verwarnung. */
    FILED_WRONG,
    /** Bei Feierabend nicht bearbeitet. */
    UNPROCESSED
}
