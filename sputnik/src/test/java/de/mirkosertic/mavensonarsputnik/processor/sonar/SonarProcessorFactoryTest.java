package de.mirkosertic.mavensonarsputnik.processor.sonar;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import pl.touk.sputnik.configuration.Configuration;

public class SonarProcessorFactoryTest {

    @Test
    public void testIsEnabled() throws Exception {
        Configuration theConfig = mock(Configuration.class);
        when(theConfig.getProperty(same(SonarProcessor.SONAR_ENABLED))).thenReturn("true");
        SonarProcessorFactory theFactory = new SonarProcessorFactory();
        assertTrue(theFactory.isEnabled(theConfig));
    }

    @Test
    public void testCreate() throws Exception {
        Configuration theConfig = mock(Configuration.class);
        SonarProcessorFactory theFactory = new SonarProcessorFactory();
        assertTrue(theFactory.create(theConfig) instanceof SonarProcessor);
    }
}
