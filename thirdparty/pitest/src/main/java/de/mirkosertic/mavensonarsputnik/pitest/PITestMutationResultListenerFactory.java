package de.mirkosertic.mavensonarsputnik.pitest;

import org.pitest.mutationtest.ListenerArguments;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.MutationResultListenerFactory;

import java.util.Properties;

public class PITestMutationResultListenerFactory implements MutationResultListenerFactory {

    @Override
    public MutationResultListener getListener(Properties aProperties, ListenerArguments aListenerArguments) {
        return new PITestResultListener(aListenerArguments.getOutputStrategy());
    }

    @Override
    public String name() {
        return "EXTENDEDXML";
    }

    @Override
    public String description() {
        return "Sputnik Extended XML Report";
    }
}