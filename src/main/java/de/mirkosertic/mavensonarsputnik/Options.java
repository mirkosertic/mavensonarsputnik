package de.mirkosertic.mavensonarsputnik;

import pl.touk.sputnik.configuration.ConfigurationOption;

public enum Options implements ConfigurationOption {

    ADDITIONAL_REPORTS("sonar.additionalReviewCommentFiles", "Comma saparated list of additional reports to add to add as comments", ""),;

    private final String key;
    private final String description;
    private final String defaultValue;

    Options(String aKey, String aDescription, String aDefaultValue) {
        this.key = aKey;
        this.description = aDescription;
        this.defaultValue = aDefaultValue;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
