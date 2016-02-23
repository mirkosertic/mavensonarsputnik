package de.mirkosertic.mavensonarsputnik.processor.sonar;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import de.mirkosertic.mavensonarsputnik.MavenEnvironment;
import de.mirkosertic.mavensonarsputnik.Options;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonarsource.scanner.maven.DependencyCollector;
import org.sonarsource.scanner.maven.ExtensionsFactory;
import org.sonarsource.scanner.maven.bootstrap.LogHandler;
import org.sonarsource.scanner.maven.bootstrap.MavenProjectConverter;
import org.sonarsource.scanner.maven.bootstrap.PropertyDecryptor;
import org.sonarsource.scanner.maven.bootstrap.RunnerBootstrapper;
import org.sonarsource.scanner.maven.bootstrap.RunnerFactory;

import com.google.common.annotations.VisibleForTesting;

import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationOption;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewException;
import pl.touk.sputnik.review.ReviewFile;
import pl.touk.sputnik.review.ReviewProcessor;
import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.Violation;

@Slf4j
public class SonarProcessor implements ReviewProcessor {

    private static final String PROCESSOR_NAME = "Sonar";

    public final static ConfigurationOption SONAR_CONFIGURATION = new ConfigurationOption() {
        @Override
        public String getKey() {
            return "sonar.configurationFile";
        }

        @Override
        public String getDescription() {
            return "Sonar configuration file";
        }

        @Override
        public String getDefaultValue() {
            return "";
        }
    };

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

            File theWorkingDirectory = MavenProjectConverter.getSonarWorkDir(theEnvironment.getMavenSession().getCurrentProject());
            theWorkingDirectory.mkdirs();

            // This will switch the cache to the working directory
            System.setProperty("SONAR_USER_HOME", theWorkingDirectory.toString());

            ExtensionsFactory theExtensionsFactory = new ExtensionsFactory(theEnvironment.getLog(), theEnvironment.getMavenSession(), theEnvironment.getLifecycleExecutor(), theEnvironment.getArtifactFactory(), theEnvironment.getLocalRepository(), theEnvironment.getArtifactMetadataSource(), theEnvironment.getArtifactCollector(),
                    theEnvironment.getDependencyTreeBuilder(), theEnvironment.getProjectBuilder());
            DependencyCollector theDependencyCollector = new DependencyCollector(theEnvironment.getDependencyTreeBuilder(), theEnvironment.getLocalRepository());
            MavenProjectConverter theMavenProjectConverter = new MavenProjectConverter(theEnvironment.getLog(), theDependencyCollector);
            LogHandler theLogHandler = new LogHandler(theEnvironment.getLog());

            PropertyDecryptor thePropertyDecryptor = new PropertyDecryptor(theEnvironment.getLog(), theEnvironment.getSecurityDispatcher());

            RunnerFactory theRunnerFactory = new RunnerFactory(theLogHandler, theEnvironment.getLog().isDebugEnabled(), theEnvironment.getRuntimeInformation(), theEnvironment.getMavenSession(), thePropertyDecryptor);

            EmbeddedRunner theRunner = theRunnerFactory.create();

            Properties theSonarConfigurationToAdd = new Properties();
            theSonarConfigurationToAdd.load(getClass().getResourceAsStream("/default-sonar.properties"));

            String theSonarConfiguration = configuration.getProperty(SONAR_CONFIGURATION);
            if (!StringUtils.isEmpty(theSonarConfiguration)) {
                try (InputStream theStream = new FileInputStream(new File(theSonarConfiguration))) {
                    theSonarConfigurationToAdd.load(theStream);
                }
            }

            theRunner.addGlobalProperties(theSonarConfigurationToAdd);

            new RunnerBootstrapper(theEnvironment.getLog(), theEnvironment.getMavenSession(), theRunner, theMavenProjectConverter, theExtensionsFactory, thePropertyDecryptor).execute();

            File resultFile = new File(theWorkingDirectory, "sonar-report.json");

            SonarResultParser parser = new SonarResultParser(resultFile);

            String theAdditionalFileNames = configuration.getProperty(Options.ADDITIONAL_REPORTS);
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