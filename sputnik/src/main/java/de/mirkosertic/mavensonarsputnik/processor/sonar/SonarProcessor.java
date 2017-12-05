package de.mirkosertic.mavensonarsputnik.processor.sonar;

import com.google.common.annotations.VisibleForTesting;
import de.mirkosertic.mavensonarsputnik.MavenEnvironment;
import de.mirkosertic.mavensonarsputnik.processor.DefaultConfigurationOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.Utils;
import org.sonarsource.scanner.maven.DependencyCollector;
import org.sonarsource.scanner.maven.ExtensionsFactory;
import org.sonarsource.scanner.maven.bootstrap.JavaVersionResolver;
import org.sonarsource.scanner.maven.bootstrap.LogHandler;
import org.sonarsource.scanner.maven.bootstrap.MavenProjectConverter;
import org.sonarsource.scanner.maven.bootstrap.PropertyDecryptor;
import org.sonarsource.scanner.maven.bootstrap.ScannerBootstrapper;
import org.sonarsource.scanner.maven.bootstrap.ScannerFactory;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationOption;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewException;
import pl.touk.sputnik.review.ReviewFile;
import pl.touk.sputnik.review.ReviewProcessor;
import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.Violation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class SonarProcessor implements ReviewProcessor {

    private static final String PROCESSOR_NAME = "Custom Sonar";

    public static final ConfigurationOption SONAR_ENABLED = new DefaultConfigurationOption("customsonar.enabled", "Custom Sonar enabled", "true");
    public static final ConfigurationOption SONAR_CONFIGURATION = new DefaultConfigurationOption("customsonar.configurationFile", "Custom Sonar configuration file", "");

    public static final ConfigurationOption ADDITIONAL_REPORTS = new DefaultConfigurationOption("customsonar.additionalReviewCommentFiles", "Comma saparated list of additional reports to add to add as comments", "");

    private final Configuration configuration;

    public SonarProcessor(@NotNull final Configuration aConfiguration) {
        configuration = aConfiguration;
    }

    @Nullable
    @Override
    public ReviewResult process(@NotNull Review review) {
        if (review.getFiles().isEmpty()) {
            return new ReviewResult();
        }

        try {
            MavenEnvironment theEnvironment = MavenEnvironment.get();

            File theWorkingDirectory = MavenEnvironment.getSonarWorkDir(theEnvironment.getMavenSession().getCurrentProject());
            theWorkingDirectory.mkdirs();

            // This will switch the cache to the working directory
            System.setProperty("SONAR_USER_HOME", theWorkingDirectory.toString());

            ExtensionsFactory theExtensionsFactory = new ExtensionsFactory(theEnvironment.getLog(), theEnvironment.getMavenSession(), theEnvironment.getLifecycleExecutor(), theEnvironment.getArtifactFactory(), theEnvironment.getLocalRepository(), theEnvironment.getArtifactMetadataSource(), theEnvironment.getArtifactCollector(),
                    theEnvironment.getDependencyTreeBuilder(), theEnvironment.getProjectBuilder());
            DependencyCollector theDependencyCollector = new DependencyCollector(theEnvironment.getDependencyTreeBuilder(), theEnvironment.getLocalRepository());

            JavaVersionResolver theVersionResolver = new JavaVersionResolver(theEnvironment.getMavenSession(), theEnvironment.getLifecycleExecutor(), theEnvironment.getLog());

            Properties theEnvProps = Utils.loadEnvironmentProperties(System.getenv());

            MavenProjectConverter theMavenProjectConverter = new MavenProjectConverter(theEnvironment.getLog(), theDependencyCollector, theVersionResolver, theEnvProps);
            LogHandler theLogHandler = new LogHandler(theEnvironment.getLog());

            PropertyDecryptor thePropertyDecryptor = new PropertyDecryptor(theEnvironment.getLog(), theEnvironment.getSecurityDispatcher());

            ScannerFactory theRunnerFactory = new ScannerFactory(theLogHandler,
                    theEnvironment.getLog(),
                    theEnvironment.getRuntimeInformation(),
                    theEnvironment.getMojoExecution(),
                    theEnvironment.getMavenSession(),
                    theEnvProps,
                    thePropertyDecryptor);

            EmbeddedScanner theScanner = theRunnerFactory.create();

            Properties theSonarConfigurationToAdd = new Properties();
            theSonarConfigurationToAdd.load(getClass().getResourceAsStream("/default-sonar.properties"));

            String theSonarConfiguration = configuration.getProperty(SONAR_CONFIGURATION);
            if (!StringUtils.isEmpty(theSonarConfiguration)) {
                try (InputStream theStream = new FileInputStream(new File(theSonarConfiguration))) {
                    theSonarConfigurationToAdd.load(theStream);
                }
            }

            theScanner.addGlobalProperties(theSonarConfigurationToAdd);

            new ScannerBootstrapper(theEnvironment.getLog(), theEnvironment.getMavenSession(), theScanner, theMavenProjectConverter, theExtensionsFactory, thePropertyDecryptor).execute();

            File resultFile = new File(theWorkingDirectory, "sonar-report.json");

            SonarResultParser parser = new SonarResultParser(resultFile);

            String theAdditionalFileNames = configuration.getProperty(ADDITIONAL_REPORTS);
            if (theAdditionalFileNames != null) {
                for (String theFileName : StringUtils.split(theAdditionalFileNames,",")) {
                    File theSingleFile = new File(resultFile.getParent(), theFileName);
                    if (theSingleFile.exists()) {
                        log.info("Adding {} to review comment", theSingleFile);
                        try (FileInputStream theStream = new FileInputStream(theSingleFile)) {
                            review.getMessages().add(IOUtils.toString(theStream) + "\n\n");
                        }
                    } else {
                        log.info("Skipping {} as it does not exist", theSingleFile);
                    }
                }
            }

            return filterResults(parser.parseResults(Boolean.parseBoolean(theSonarConfigurationToAdd.getProperty("sonar.includeAllIssues"))), review);
        }
        catch (Exception e) {
            throw new ReviewException("SonarResultParser error", e);
        }
    }

    /**
     * Filters a ReviewResult to keep only the violations that are about a file
     * which is modified by a given review.
     */
    @VisibleForTesting
    ReviewResult filterResults(ReviewResult results, Review review) {
        ReviewResult filteredResults = new ReviewResult();

        // Sonar might not report the full qualified file names, so this is the best guess
        for (Violation violation : results.getViolations()) {
            for (ReviewFile theFile : review.getFiles()) {
                if (theFile.getReviewFilename().endsWith(violation.getFilenameOrJavaClassName())) {
                    filteredResults.add(new Violation(theFile.getReviewFilename(), violation.getLine(), violation.getMessage(), violation.getSeverity()));
                }
            }
        }
        return filteredResults;
    }

    @NotNull
    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }
}