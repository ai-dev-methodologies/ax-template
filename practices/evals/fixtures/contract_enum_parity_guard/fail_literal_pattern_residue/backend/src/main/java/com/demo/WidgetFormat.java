package com.demo;

/** Constructor-arg shape (mirrors reportexport.ExportFormat:12-13). */
public enum WidgetFormat {
    CSV("text/csv", ".csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String contentType;
    private final String ext;

    WidgetFormat(String contentType, String ext) {
        this.contentType = contentType;
        this.ext = ext;
    }

    public String contentType() { return contentType; }
    public String ext() { return ext; }
}
