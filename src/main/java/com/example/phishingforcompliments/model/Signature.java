package com.example.phishingforcompliments.model;

/** Signatur am Ende einer E-Mail: Name, Abteilung, Mitarbeiter-Code (z.B. "X0U3A25D"). */
public record Signature(String name, String department, String code) {

    public static final Signature NONE = new Signature("", "", "");

    public boolean isEmpty() {
        return name.isBlank() && department.isBlank() && code.isBlank();
    }
}
