package me.crylonz.deadchest.commands;

import me.crylonz.deadchest.utils.ConfigKey;

import java.util.*;
import java.util.stream.Collectors;

final class DCConfigCommandSupport {

    static final String ACTION_GET = "get";
    static final String ACTION_SET = "set";
    static final String ACTION_RESET = "reset";
    static final String ACTION_EDIT = "edit";

    private static final List<String> ACTIONS = Collections.unmodifiableList(
            Arrays.asList(ACTION_GET, ACTION_SET, ACTION_RESET, ACTION_EDIT)
    );

    private DCConfigCommandSupport() {
    }

    static List<String> actions() {
        return ACTIONS;
    }

    static List<String> keys() {
        return Arrays.stream(ConfigKey.values())
                .map(ConfigKey::canonicalPath)
                .sorted()
                .collect(Collectors.toList());
    }

    static List<String> interactiveEditKeys() {
        return Arrays.stream(ConfigKey.values())
                .filter(ConfigKey::supportsInteractiveEdit)
                .map(ConfigKey::canonicalPath)
                .sorted()
                .collect(Collectors.toList());
    }

    static ConfigKey resolveKey(String rawPath) {
        if (rawPath == null) {
            return null;
        }
        return ConfigKey.fromCanonicalPath(rawPath.trim().toLowerCase(Locale.ROOT));
    }

    static ParsedConfigValue parseValue(ConfigKey key, String rawValue) {
        if (key == null) {
            throw new IllegalArgumentException("Unknown key");
        }

        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(expectedValueDescription(key));
        }

        switch (key.valueType()) {
            case BOOLEAN:
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    return new ParsedConfigValue(Boolean.parseBoolean(value), value.toLowerCase(Locale.ROOT));
                }
                break;
            case INTEGER:
                try {
                    int parsed = Integer.parseInt(value);
                    return new ParsedConfigValue(parsed, String.valueOf(parsed));
                } catch (NumberFormatException ignored) {
                    break;
                }
            case DOUBLE:
                try {
                    double parsed = Double.parseDouble(value);
                    return new ParsedConfigValue(parsed, stripTrailingZero(parsed));
                } catch (NumberFormatException ignored) {
                    break;
                }
            case STRING:
                String normalizedString = normalizeSuggestedValue(key, value);
                return new ParsedConfigValue(normalizedString, normalizedString);
            case STRING_LIST:
                List<String> parts = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));
                return new ParsedConfigValue(parts, String.join(", ", parts));
            default:
                break;
        }

        throw new IllegalArgumentException(expectedValueDescription(key));
    }

    static String expectedValueDescription(ConfigKey key) {
        List<String> suggestions = key.suggestedValues();
        if (!suggestions.isEmpty()) {
            return key.valueType().label() + " (" + String.join(", ", suggestions) + ")";
        }
        return key.valueType().label();
    }

    static String formatValue(Object value) {
        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        if (value instanceof Double) {
            return stripTrailingZero((Double) value);
        }
        return String.valueOf(value);
    }

    static List<String> valueSuggestions(ConfigKey key) {
        return key == null ? Collections.emptyList() : key.suggestedValues();
    }

    private static String normalizeSuggestedValue(ConfigKey key, String rawValue) {
        for (String suggestion : key.suggestedValues()) {
            if (suggestion.equalsIgnoreCase(rawValue)) {
                return suggestion;
            }
        }
        return rawValue;
    }

    private static String stripTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    static final class ParsedConfigValue {
        private final Object value;
        private final String displayValue;

        ParsedConfigValue(Object value, String displayValue) {
            this.value = value;
            this.displayValue = displayValue;
        }

        Object value() {
            return value;
        }

        String displayValue() {
            return displayValue;
        }
    }
}
