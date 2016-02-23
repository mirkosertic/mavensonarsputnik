package de.mirkosertic.mavensonarsputnik.processor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultConfigurationOptionTest {

    @Test
    public void testInit() {
        DefaultConfigurationOption theOption = new DefaultConfigurationOption("A", "B", "C");
        assertEquals("A", theOption.getKey());
        assertEquals("B", theOption.getDescription());
        assertEquals("C", theOption.getDefaultValue());
    }
}