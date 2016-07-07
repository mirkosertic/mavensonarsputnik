package de.mirkosertic.mavensonarsputnik.processor.pitest;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import pl.touk.sputnik.configuration.Configuration;

public class PITestProcessorFactoryTest {

    @Test
    public void testIsEnabled() throws Exception {
        Configuration theConfig = mock(Configuration.class);
        when(theConfig.getProperty(same(PITestProcessor.PITEST_ENABLED))).thenReturn("true");
        PITestProcessorFactory theFactory = new PITestProcessorFactory();
        assertTrue(theFactory.isEnabled(theConfig));
    }

    @Test
    public void testCreate() throws Exception {
        Configuration theConfig = mock(Configuration.class);
        PITestProcessorFactory theFactory = new PITestProcessorFactory();
        assertTrue(theFactory.create(theConfig) instanceof PITestProcessor);
    }
}