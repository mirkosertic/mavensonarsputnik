package pl.touk.sputnik.processor.sonar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import de.mirkosertic.mavensonarsputnik.SonarExecutor;
import de.mirkosertic.mavensonarsputnik.SonarExecutorHelper;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.annotations.VisibleForTesting;

import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.GeneralOption;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewException;
import pl.touk.sputnik.review.ReviewFile;
import pl.touk.sputnik.review.ReviewProcessor;
import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.Violation;

@Slf4j
public class SonarProcessor implements ReviewProcessor {

    private static final String PROCESSOR_NAME = "Sonar";

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
            SonarExecutor theExecutor = SonarExecutorHelper.get();

            File resultFile = theExecutor.executeSonar();

            SonarResultParser parser = new SonarResultParser(resultFile);

            final Properties theSonarProperties = new Properties();
            for (final String property: StringUtils.split(configuration.getProperty(GeneralOption.SONAR_PROPERTIES), ',')){
                final File propertyFile = new File(StringUtils.strip(property));
                log.info("Loading {}", propertyFile.getAbsolutePath());
                theSonarProperties.load(new FileInputStream(propertyFile));
            }

            String theAdditionalFileNames = theSonarProperties.getProperty("sonar.additionalReviewCommentFiles");
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

            return filterResults(parser.parseResults(), review);
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
        Set<String> reviewFiles = new HashSet<>();
        for (ReviewFile file : review.getFiles()) {
            reviewFiles.add(file.getReviewFilename());
        }
        for (Violation violation : results.getViolations()) {
            if (reviewFiles.contains(violation.getFilenameOrJavaClassName())) {
                filteredResults.add(violation);
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