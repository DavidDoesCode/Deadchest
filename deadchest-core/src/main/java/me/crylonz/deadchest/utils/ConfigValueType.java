package me.crylonz.deadchest.utils;

public enum ConfigValueType {
    BOOLEAN("boolean"),
    INTEGER("integer"),
    DOUBLE("decimal"),
    STRING("string"),
    STRING_LIST("comma-separated list");

    private final String label;

    ConfigValueType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
