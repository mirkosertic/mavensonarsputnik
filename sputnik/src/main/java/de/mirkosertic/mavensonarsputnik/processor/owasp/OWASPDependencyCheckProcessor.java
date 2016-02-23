package de.mirkosertic.mavensonarsputnik.processor.owasp;

import de.mirkosertic.mavensonarsputnik.MavenEnvironment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

@Slf4j
public class OWASPDependencyCheckProcessor implements ReviewProcessor {

    public final static ConfigurationOption OWASPDEPENDENCYCHECK_ENABLED = new ConfigurationOption() {
        @Override
        public String getKey() {
            return "owaspdependencycheck.enabled";
        }

        @Override
        public String getDescription() {
            return "OWASP Dependency Check enabled";
        }

        @Override
        public String getDefaultValue() {
            return "true";
        }
    };

    public final static ConfigurationOption OWASPDEPENDENCYCHECK_CONFIGURATION = new ConfigurationOption() {
        @Override
        public String getKey() {
            return "owaspdependencycheck.configurationFile";
        }

        @Override
        public String getDescription() {
            return "OWASP Dependency check configuration file";
        }

        @Override
        public String getDefaultValue() {
            return "";
        }
    };

    public static class MavenIdentifier {

        private final String groupId;
        private final String artifactId;
        private final String version;

        public MavenIdentifier(String aIdentifierString) {
            aIdentifierString = aIdentifierString.substring(1, aIdentifierString.length() - 1);
            int p = aIdentifierString.indexOf(":");
            groupId = aIdentifierString.substring(0, p);
            aIdentifierString = aIdentifierString.substring(p+1);
            p = aIdentifierString.indexOf(":");
            artifactId = aIdentifierString.substring(0, p);
            aIdentifierString = aIdentifierString.substring(p+1);
            version = aIdentifierString;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }
    }

    private final Configuration configuration;
    private final Properties properties;
    private final Severity severity;
    private final boolean report;
    private final boolean reportTransitive;

    public OWASPDependencyCheckProcessor(Configuration aConfiguration) {
        configuration = aConfiguration;

        properties = new Properties();
        try (InputStream theStream = getClass().getResourceAsStream("/default-owaspdependencycheck.properties")) {
            properties.load(theStream);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing OWASPDependencyCheck", e);
        }

        String theConfiguration = aConfiguration.getProperty(OWASPDEPENDENCYCHECK_CONFIGURATION);
        if (!StringUtils.isEmpty(theConfiguration)) {
            try (InputStream theStream = new FileInputStream(theConfiguration)) {
                properties.load(theStream);
            } catch (Exception e) {
                throw new RuntimeException("Error initializing OWASP Dependency Check", e);
            }
        }

        severity = Severity.valueOf(properties.getProperty("owaspdependencycheck.severity"));
        report = Boolean.parseBoolean(properties.getProperty("owaspdependencycheck.report"));
        reportTransitive = Boolean.parseBoolean(properties.getProperty("owaspdependencycheck.reporttransitive"));
    }

    @NotNull @Override
    public String getName() {
        return "OWASP Dependency Check";
    }

    @Nullable @Override
    public ReviewResult process(@NotNull Review aReview) {
        ReviewResult theResult = new ReviewResult();
        for (ReviewFile theFile : aReview.getFiles()) {
            File theIOFile = theFile.getIoFile();
            if (theIOFile != null && theIOFile.exists() && theIOFile.isFile() && theIOFile.getName().equals("pom.xml")) {
                try {
                    processChangedMavenModule(theFile, theIOFile.getAbsoluteFile(), aReview, theResult);
                } catch (Exception e) {
                    throw new ReviewException("Error invoking OWASP Dependency Checker for " + theIOFile);
                }
            } else {
                log.debug("Ignoring {}", theIOFile);
            }
        }
        return theResult;
    }

    public static Xpp3Dom plainTextConfigurationFrom(String aXML) throws IOException, XmlPullParserException {
        return Xpp3DomBuilder.build(new StringReader(aXML));
    }

    private void processChangedMavenModule(ReviewFile aReviewFile, File aPomXMLFile, Review aReview, ReviewResult aReviewResult)
            throws IOException, XmlPullParserException, MojoExecutionException, ParserConfigurationException, SAXException {

        log.info("Processing changed maven module {}", aPomXMLFile);

        MavenEnvironment theEnvironment = MavenEnvironment.get();

        MavenProject theProject = null;
        for (MavenProject theSingleProject : theEnvironment.getMavenSession().getAllProjects()) {
            if ((theSingleProject.getFile() != null) && (theSingleProject.getFile().equals(aPomXMLFile))) {
                theProject = theSingleProject;
            }
        }

        if (theProject == null) {
            throw new IllegalStateException("Cannot find Maven project for " + aPomXMLFile);
        }

        File theOutputDirectory = new File(new File(aPomXMLFile.getParent(), "target"), "owasp-dependency-check");
        log.info("Writing reports to {}", theOutputDirectory);

        theOutputDirectory.mkdirs();

        MavenProject theOldCurrent = theEnvironment.getMavenSession().getCurrentProject();
        try {
            theEnvironment.getMavenSession().setCurrentProject(theProject);

            executeMojo(
                    plugin(
                            groupId("org.owasp"),
                            artifactId("dependency-check-maven"),
                            version(properties.getProperty("owaspdependencycheck.pluginversion"))
                    ),
                    goal("check"),
                    plainTextConfigurationFrom("<configuration><autoUpdate>true</autoUpdate><format>ALL</format><outputDirectory>"
                            + theOutputDirectory + "</outputDirectory></configuration>"),
                    executionEnvironment(
                            theProject,
                            theEnvironment.getMavenSession(),
                            theEnvironment.getBuildPluginManager()
                    )
            );
        } finally {
            theEnvironment.getMavenSession().setCurrentProject(theOldCurrent);
        }

        if (report) {
            File theXMLReport = new File(theOutputDirectory, "dependency-check-report.xml");
            DocumentBuilderFactory theFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder theBuilder = theFactory.newDocumentBuilder();
            Document theDocument = theBuilder.parse(theXMLReport);
            NodeList theDependencies = theDocument.getElementsByTagName("dependency");
            for (int i = 0; i < theDependencies.getLength(); i++) {
                Element theDependency = (Element) theDependencies.item(i);
                NodeList theIdentifiers = theDependency.getElementsByTagName("identifier");

                NodeList theVulnerabilities = theDependency.getElementsByTagName("vulnerability");
                if (theVulnerabilities != null && theVulnerabilities.getLength() > 0 && theIdentifiers.getLength() > 0) {
                    for (int j = 0; j < theIdentifiers.getLength(); j++) {
                        Element theIdentifier = (Element) theIdentifiers.item(j);
                        if ("maven".equals(theIdentifier.getAttribute("type"))) {
                            NodeList theNames = theIdentifier.getElementsByTagName("name");
                            for (int k = 0; k < theNames.getLength(); k++) {
                                Element theName = (Element) theNames.item(k);
                                processSingleDependency(aPomXMLFile, theVulnerabilities, aReviewResult, aReviewFile,
                                        new MavenIdentifier(theName.getTextContent()));
                            }
                        }
                    }
                }
            }
        }
    }

    private String getElementContent(Element aElement, String aTagname) {
        NodeList theChilds = aElement.getChildNodes();
        for (int i=0;i<theChilds.getLength();i++) {
            Node theChild = theChilds.item(i);
            if (aTagname.equals(theChild.getNodeName())) {
                return theChild.getTextContent();
            }
        }
        return "";
    }

    private void processSingleDependency(File aPomXML, NodeList aVulnerabilities, ReviewResult aResult, ReviewFile aReviewFile, MavenIdentifier aMavenIdentifier)
            throws IOException {
        List<String> theLines = new ArrayList<>();
        try (BufferedReader theReader = new BufferedReader(new FileReader(aPomXML))) {
            while(theReader.ready()) {
                String theLine = theReader.readLine();
                if (theLine != null) {
                    theLines.add(theLine);
                }
            }
        }

        for (int i=0;i<aVulnerabilities.getLength();i++) {
            Element theSingleElement = (Element) aVulnerabilities.item(i);
            String theDescription = getElementContent(theSingleElement, "description");
            String theCWE = getElementContent(theSingleElement, "cwe");
            String theSeverity = getElementContent(theSingleElement, "severity");
            String theName = getElementContent(theSingleElement, "name");

            StringBuilder theReviewComment = new StringBuilder(theSeverity);
            theReviewComment.append(" Severity");
            theReviewComment.append("\n\n");
            if (theCWE.length() > 0) {
                theReviewComment.append(theCWE);
                theReviewComment.append(" : ");
            }
            theReviewComment.append(theDescription);
            theReviewComment.append("\n\n");

            NodeList theReferences = theSingleElement.getElementsByTagName("reference");
            for (int k=0;k<theReferences.getLength();k++) {
                Element theReference = (Element) theReferences.item(k);
                String theSource = getElementContent(theReference, "source");
                String theURL = getElementContent(theReference, "url");
                String theReferenceName = getElementContent(theReference, "name");

                theReviewComment.append("*");
                theReviewComment.append(" ");
                theReviewComment.append(theSource);
                theReviewComment.append(" ");
                theReviewComment.append(theURL);
                theReviewComment.append("\n");
            }

            String theCompleteComment = theReviewComment.toString();

            boolean theSomethingFound = false;
            for (int k=0;k<theLines.size();k++) {
                String theSingleLine = theLines.get(k);
                if (theSingleLine.contains("<artifactId>" + aMavenIdentifier.getArtifactId() + "</artifactId")) {

                    boolean theGroupIdFound = false;
                    if (k>0) {
                        theGroupIdFound = theGroupIdFound || theLines.get(k-1).contains("<groupId>" + aMavenIdentifier.getGroupId() + "</groupId");
                    }
                    if (k<theLines.size() - 1) {
                        theGroupIdFound = theGroupIdFound || theLines.get(k+1).contains("<groupId>" + aMavenIdentifier.getGroupId() + "</groupId");
                    }
                    if (theGroupIdFound) {
                        Violation theViolation = new Violation(aReviewFile.getReviewFilename(), k + 1, theCompleteComment,
                                severity);
                        aResult.add(theViolation);
                        theSomethingFound = true;
                    }
                }
            }

            if (!theSomethingFound && reportTransitive) {
                Violation theViolation = new Violation(aReviewFile.getReviewFilename(), 1, "Transitive Dependency Warning\n\n" + theCompleteComment,
                        severity);
                aResult.add(theViolation);
            }
        }
    }
}