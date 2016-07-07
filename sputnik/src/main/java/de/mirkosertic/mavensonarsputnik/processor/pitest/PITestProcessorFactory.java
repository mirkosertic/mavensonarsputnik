package de.mirkosertic.mavensonarsputnik.processor.pitest;

import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.processor.ReviewProcessorFactory;

public class PITestProcessorFactory implements ReviewProcessorFactory<PITestProcessor> {

    @Override
    public boolean isEnabled(Configuration aConfiguration) {
        return Boolean.valueOf(aConfiguration.getProperty(PITestProcessor.PITEST_ENABLED));
    }

    @Override
    public PITestProcessor create(Configuration aConfiguration) {
        return new PITestProcessor(aConfiguration);
    }
}
