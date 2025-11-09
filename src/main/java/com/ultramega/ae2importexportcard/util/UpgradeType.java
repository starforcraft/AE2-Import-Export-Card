package com.ultramega.ae2importexportcard.util;

public enum UpgradeType {
    IMPORT(0),
    EXPORT(1);

    private final int id;

    UpgradeType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
