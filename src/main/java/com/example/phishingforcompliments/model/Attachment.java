package com.example.phishingforcompliments.model;

/** Dateianhang einer E-Mail. */
public record Attachment(String fileName, int sizeKb) {

    /** Alle Endungen nach dem ersten Punkt, z.B. "pdf.exe". */
    public String extensionChain() {
        int dot = fileName.indexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    public String lastExtension() {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    public boolean hasDoubleExtension() {
        return extensionChain().contains(".");
    }
}
