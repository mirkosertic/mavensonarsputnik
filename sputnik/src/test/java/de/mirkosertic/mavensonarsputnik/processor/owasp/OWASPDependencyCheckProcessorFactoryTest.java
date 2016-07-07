package de.mirkosertic.mavensonarsputnik.processor.owasp;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

import org.junit.Test;
import pl.touk.sputnik.configuration.Configuration;

public class OWASPDependencyCheckProcessorFactoryTest {

    @Test
    public void testIsEnabled() throws Exception {
        Configuration theConfig = mock(Configuration.class);
        when(theConfig.getProperty(same(OWASPDependencyCheckProcessor.OWASPDEPENDENCYCHECK_ENABLED))).thenReturn("true");
        OWASPDependencyCheckProcessorFactory theFactory = new OWASPDependencyCheckProcessorFactory();
        assertTrue(theFactory.isEnabled(theConfig));
    }

    @Test
    public void testCreate() throws Exception {
        Configuration theConfig = mock(Configuration.class);
        OWASPDependencyCheckProcessorFactory theFactory = new OWASPDependencyCheckProcessorFactory();
        assertTrue(theFactory.create(theConfig) instanceof OWASPDependencyCheckProcessor);
    }
}