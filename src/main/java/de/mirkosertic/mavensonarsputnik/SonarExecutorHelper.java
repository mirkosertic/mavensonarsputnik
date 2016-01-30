package de.mirkosertic.mavensonarsputnik;

public class SonarExecutorHelper {

    private static final ThreadLocal<SonarExecutor> EXECUTOR = new ThreadLocal<>();

    SonarExecutorHelper() {
    }

    public static SonarExecutor get() {
        return EXECUTOR.get();
    }

    public static void set(SonarExecutor aExecutor) {
        EXECUTOR.set(aExecutor);
    }

    public static void remove() {
        EXECUTOR.remove();
    }
}
