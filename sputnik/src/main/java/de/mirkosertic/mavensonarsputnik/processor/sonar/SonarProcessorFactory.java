package de.mirkosertic.mavensonarsputnik.processor.sonar;

import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.processor.ReviewProcessorFactory;

public class SonarProcessorFactory implements ReviewProcessorFactory<SonarProcessor> {

    @Override
    public boolean isEnabled(Configuration aConfiguration) {
        return Boolean.valueOf(aConfiguration.getProperty(SonarProcessor.SONAR_ENABLED));
    }

    @Override
    public SonarProcessor create(Configuration aConfiguration) {
        return new SonarProcessor(aConfiguration);
    }
}