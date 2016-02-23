package de.mirkosertic.mavensonarsputnik.processor.pitest;

import de.mirkosertic.mavensonarsputnik.MavenEnvironment;
import de.mirkosertic.mavensonarsputnik.processor.DefaultConfigurationOption;
import lombok.extern.slf4j.Slf4j;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationOption;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewException;
import pl.touk.sputnik.review.ReviewFile;
import pl.touk.sputnik.review.ReviewProcessor;
import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.Severity;
import pl.touk.sputnik.review.Violation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.dependency;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

@Slf4j
public class PITestProcessor implements ReviewProcessor {

    public final static ConfigurationOption PITEST_ENABLED = new DefaultConfigurationOption("pitest.enabled", "PITest enabled", "true");
    public final static ConfigurationOption PITEST_CONFIGURATION = new DefaultConfigurationOption("pitest.configurationFile", "PITest configuration file", "");

    private static final String NAME = "PITest";

    private final Properties properties;
    private final Set<String> includedStatus;
    private final Severity severity;

    public PITestProcessor(Configuration aConfiguration) {
        properties = new Properties();
        try (InputStream theStream = getClass().getResourceAsStream("/default-pitest.properties")) {
            properties.load(theStream);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing PITest", e);
        }

        String theConfiguration = aConfiguration.getProperty(PITEST_CONFIGURATION);
        if (!StringUtils.isEmpty(theConfiguration)) {
            try (InputStream theStream = new FileInputStream(theConfiguration)) {
                properties.load(theStream);
            } catch (Exception e) {
                throw new RuntimeException("Error initializing PITest", e);
            }
        }

        severity = Severity.valueOf(properties.getProperty("pitest.severity"));

        includedStatus = new HashSet<>();
        for (String theStatus : StringUtils.split(properties.getProperty("pitest.reportstatus"),",")) {
            includedStatus.add(theStatus.trim());
        }
    }

    private String getElementValue(Element aElement, String aElementName) {
        NodeList theElements = aElement.getElementsByTagName(aElementName);
        for (int i=0;i<theElements.getLength();i++) {
            Element theElement = (Element) theElements.item(i);
            return theElement.getTextContent();
        }
        return null;
    }

    private void addFromMutationReportTo(Review aReview, File aFile, ReviewResult aResult)
            throws ParserConfigurationException, IOException, SAXException {
        log.info("Parsing {}", aFile);

        DocumentBuilderFactory theFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder theBuilder = theFactory.newDocumentBuilder();
        Document theDocument = theBuilder.parse(aFile);

        NodeList theMutations = theDocument.getElementsByTagName("mutation");
        for (int i=0;i<theMutations.getLength();i++) {
            Element theElement = (Element) theMutations.item(i);
            boolean theDetected = Boolean.parseBoolean(theElement.getAttribute("detected"));
            String theStatus = theElement.getAttribute("status");

            if (includedStatus.contains(theStatus)) {
                String theSourceFile = getElementValue(theElement, "sourceFile");
                int theLineNumber = Integer.valueOf(getElementValue(theElement, "lineNumber"));
                String theMutator = getElementValue(theElement, "mutator");
                String theDescription = getElementValue(theElement, "description");

                log.debug("Found mutation in file {}", theSourceFile);
                for (ReviewFile theFile : aReview.getFiles()) {
                    if (theFile.getIoFile().getName().equals(theSourceFile)) {

                        StringBuilder theMessage = new StringBuilder(theStatus);
                        theMessage.append(" Mutation : ");
                        theMessage.append(theMutator);
                        if (theDescription != null) {
                            theMessage.append("\n");
                            theMessage.append("\n");
                            theMessage.append(theDescription);
                        }

                        NodeList theTestInfos = theElement.getElementsByTagName("testInfo");
                        if (theTestInfos != null && theTestInfos.getLength() > 0) {
                            theMessage.append("\n");
                            theMessage.append("\n");
                            theMessage.append("Related Unit Tests:");
                            theMessage.append("\n");
                            for (int j = 0; j < theTestInfos.getLength(); j++) {
                                Element theTestInfo = (Element) theTestInfos.item(j);
                                theMessage.append("*");
                                theMessage.append(theTestInfo.getTextContent());
                                theMessage.append("\n");
                            }
                        }

                        Violation theViolation = new Violation(theFile.getReviewFilename(), theLineNumber, theMessage.toString(),
                                severity);

                        aResult.add(theViolation);
                    }
                }
            }
        }
    }

    public static Xpp3Dom plainTextConfigurationFrom(String aXML) throws IOException, XmlPullParserException {
        return Xpp3DomBuilder.build(new StringReader(aXML));
    }

    private Xpp3Dom createConfiguration() throws IOException, XmlPullParserException {

        String theDefaultConfiguration = "<failWhenNoMutations>false</failWhenNoMutations><outputFormats><outputFormat>EXTENDEDXML</outputFormat></outputFormats>";

        if (Boolean.parseBoolean(properties.getProperty("pitest.verbose"))) {
            theDefaultConfiguration = theDefaultConfiguration + "<verbose>true</verbose>";
        }

        if (Boolean.parseBoolean(properties.getProperty("pitest.onlylastcommit"))) {
            return plainTextConfigurationFrom("<configuration><analyseLastCommit>true</analyseLastCommit>" + theDefaultConfiguration + "</configuration>");
        }
        return plainTextConfigurationFrom("<configuration>" + theDefaultConfiguration + " </configuration>");
    }

    private void invokePITest() {
        MavenEnvironment theEnvironment = MavenEnvironment.get();
        MavenSession theSession = theEnvironment.getMavenSession();
        for (MavenProject theProject : theSession.getAllProjects()) {
            MavenProject theOldProject = theSession.getCurrentProject();

            log.info("Invoking PITest for {}:{}", theProject.getGroupId(), theProject.getArtifact());

            theSession.setCurrentProject(theProject);

            String theCurrentVersion = getClass().getPackage().getImplementationVersion();

            List<Dependency> theDependencies = new ArrayList<>();
            theDependencies.add(dependency("de.mirkosertic.mavensonarsputnik", "pitest-plugins", theCurrentVersion));

            try {

                executeMojo(
                        plugin(
                                groupId("org.pitest"),
                                artifactId("pitest-maven"),
                                version(properties.getProperty("pitest.pluginversion")),
                                theDependencies
                        ),
                        goal(properties.getProperty("pitest.pluginGoal")),
                        createConfiguration(),
                        executionEnvironment(
                                theProject,
                                theEnvironment.getMavenSession(),
                                theEnvironment.getBuildPluginManager()
                        )
                );
            } catch (Exception e) {
                log.warn("Error invoking PITest, but analysis will continue", e);
            } finally {
                theSession.setCurrentProject(theOldProject);
            }
        }
    }

    @Nullable @Override
    public ReviewResult process(@NotNull Review aReview) {

        try {

            invokePITest();

            final String theMutationsFileName = properties.getProperty("pitest.mutationxmlreportfilename");

            File theWorkingDir = new File(System.getProperty("user.dir"));
            log.info("Searching recursively in {} for {}", theWorkingDir, theMutationsFileName);

            Collection<File> theFiles = FileUtils.listFiles(theWorkingDir, new AbstractFileFilter() {
                @Override
                public boolean accept(File aFile) {
                    return aFile.getName().equals(theMutationsFileName);
                }
            }, DirectoryFileFilter.DIRECTORY);

            ReviewResult theResult = new ReviewResult();

            for (File theFile :theFiles) {
                try {
                    log.info("Parsing report file {}", theFile);

                    addFromMutationReportTo(aReview, theFile, theResult);
                } catch (Exception e) {
                    log.warn("Error parsing file {}", theFile, e);
                }
            }

            return theResult;
        } catch (Exception e) {
            throw new ReviewException("Error analyzing pitest reports", e);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }
}