package de.mirkosertic.mavensonarsputnik.processor;

import pl.touk.sputnik.configuration.ConfigurationOption;

public class DefaultConfigurationOption implements ConfigurationOption {

    private final String key;
    private final String description;
    private final String defaultValue;

    public DefaultConfigurationOption(String aKey, String aDescription, String aDefaultValue) {
        key = aKey;
        description = aDescription;
        defaultValue = aDefaultValue;
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
