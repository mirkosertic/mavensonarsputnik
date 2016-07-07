package de.mirkosertic.mavensonarsputnik.processor.owasp;

import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.processor.ReviewProcessorFactory;

public class OWASPDependencyCheckProcessorFactory implements ReviewProcessorFactory<OWASPDependencyCheckProcessor> {

    @Override
    public boolean isEnabled(Configuration aConfiguration) {
        return Boolean.valueOf(aConfiguration.getProperty(OWASPDependencyCheckProcessor.OWASPDEPENDENCYCHECK_ENABLED));
    }

    @Override
    public OWASPDependencyCheckProcessor create(Configuration aConfiguration) {
        return new OWASPDependencyCheckProcessor(aConfiguration);
    }
}
