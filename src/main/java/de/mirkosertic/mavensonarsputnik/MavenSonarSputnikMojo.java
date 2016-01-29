package de.mirkosertic.mavensonarsputnik;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import pl.touk.sputnik.configuration.CliOption;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationBuilder;
import pl.touk.sputnik.configuration.GeneralOption;
import pl.touk.sputnik.connector.ConnectorFacade;
import pl.touk.sputnik.connector.ConnectorFacadeFactory;
import pl.touk.sputnik.connector.ConnectorType;
import pl.touk.sputnik.engine.Engine;

/**
 * Execute Sputnik on the project and report the issues to Gerrit.
 */
@Mojo(name = "sputnik", defaultPhase = LifecyclePhase.INSTALL, requiresProject = false)
public class MavenSonarSputnikMojo extends AbstractMojo {

    @Component
    private MavenSession mavenSession;

    /**
     * The gerrit change id.
     */
    @Parameter(defaultValue = "${gerritChangeId}", required = true)
    private String gerritChangeId;

    /**
     * The gerrit revision id.
     */
    @Parameter(defaultValue = "${gerritRevision}", required = true)
    private String gerritRevision;

    /**
     * The Sputnik configuration property file.
     */
    @Parameter(defaultValue = "${sputnikConfiguration}", required = true)
    private File sputnikConfiguration;

    /**
     * The Sonar configuration property file.
     */
    @Parameter(defaultValue = "${sonarConfiguration}", required = true)
    private File sonarConfiguration;

    /**
     * Ignore generated sources located inside the project output directory /target/*.
     */
    @Parameter(defaultValue = "true", required = true)
    private boolean ignoreGeneratedCode;

    private Properties sputnikProperties;

    private Properties sonarProperties;

    private boolean isIgnored(MavenProject aProject, File aFile) {
        if (ignoreGeneratedCode) {
            return aFile.toString().startsWith(aProject.getBuild().getDirectory());
        }
        return false;
    }

    private void listModulesFor(String aPrefix, MavenProject aProject, int aRecursionLevel, List<MavenProject> aProjectList)
            throws IOException {
        StringBuilder theBuilder = new StringBuilder();
        for (int i=0;i<aRecursionLevel;i++) {
            theBuilder.append(" ");
        }

        List<String> theModules = new ArrayList<String>();

        for (MavenProject theProject : aProjectList) {
            if (theProject.getParent() == aProject) {
                listModulesFor(theProject.getArtifactId() + ".sonar" , theProject, aRecursionLevel + 1, aProjectList);
                theModules.add(theProject.getArtifactId());
            }
        }

        if (!theModules.isEmpty()) {
            StringBuilder theModulesConfig = new StringBuilder();
            for (String theData : theModules) {
                if (theModulesConfig.length() > 0) {
                    theModulesConfig.append(",");
                }
                theModulesConfig.append(theData);
            }
            sonarProperties.put(aPrefix + ".modules" , theModulesConfig.toString());
        } else {

            StringBuilder theSourceRoots = new StringBuilder();
            for (String theSourceRoot : aProject.getCompileSourceRoots()) {
                File theFile = new File(theSourceRoot);
                if (theFile.exists() && !isIgnored(aProject, theFile)) {
                    String theFullName = theSourceRoot;
                    theFullName = theFullName.substring(aProject.getBasedir().toString().length() + 1);

                    if (theSourceRoots.length() > 0) {
                        theSourceRoots.append(",");
                    }
                    theSourceRoots.append(theFullName);
                }
            }

            StringBuilder theTestRoots = new StringBuilder();
            for (String theSourceRoot : aProject.getTestCompileSourceRoots()) {
                File theFile = new File(theSourceRoot);
                if (theFile.exists() && !isIgnored(aProject, theFile)) {
                    String theFullName = theSourceRoot;
                    theFullName = theFullName.substring(aProject.getBasedir().toString().length() + 1);

                    if (theTestRoots.length() > 0) {
                        theTestRoots.append(",");
                    }
                    theTestRoots.append(theFullName);
                }
            }

            sonarProperties.put(aPrefix + ".sources" , theSourceRoots.toString());
            sonarProperties.put(aPrefix + ".tests" , theTestRoots.toString());

            File theOutputDirectory = new File(aProject.getBuild().getOutputDirectory());
            if (theOutputDirectory.exists()) {
                sonarProperties.put(aPrefix + ".java.binaries" , theOutputDirectory.toString());
            }

            File theTestOutputDirectory = new File(aProject.getBuild().getTestOutputDirectory());
            if (theTestOutputDirectory.exists()) {
                sonarProperties.put(aPrefix + ".test.binaries" , theTestOutputDirectory.toString());
            }
        }

        String theBaseDir = aProject.getBasedir().toString();
        sonarProperties.put(aPrefix + ".projectBaseDir", theBaseDir);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            MavenProject theProject = mavenSession.getCurrentProject();

            sputnikProperties = new Properties();
            sputnikProperties.load(getClass().getResourceAsStream("/default-sputnik.properties"));

            try (InputStream theStream = new FileInputStream(sputnikConfiguration)) {
                sputnikProperties.load(theStream);
            }

            sputnikProperties.setProperty(CliOption.CHANGE_ID.getKey(), gerritChangeId);
            sputnikProperties.setProperty(CliOption.REVISION_ID.getKey(), gerritRevision);

            sonarProperties = new Properties();
            sonarProperties.load(getClass().getResourceAsStream("/default-sonar.properties"));
            sonarProperties.put("sonar.projectName", theProject.getName());
            sonarProperties.put("sonar.projectVersion", theProject.getVersion());
            sonarProperties.put("sonar.projectKey", theProject.getGroupId() + ":" + theProject.getArtifactId());

            try (InputStream theStream = new FileInputStream(sonarConfiguration)) {
                sonarProperties.load(theStream);
            }

            if (mavenSession.getTopLevelProject() == theProject) {
                // Run only once at the end of maven execution
                List<MavenProject> theProjects = mavenSession.getProjects();
                listModulesFor("sonar", mavenSession.getTopLevelProject(), 0, theProjects);

                File theTempFile = File.createTempFile("sputnik-sonar", ".properties");
                try (FileOutputStream theStream = new FileOutputStream(theTempFile)) {
                    sonarProperties.store(theStream, "");;
                }
                sputnikProperties.setProperty(GeneralOption.SONAR_PROPERTIES.getKey(), theTempFile.toString());

                Configuration theConfiguration = ConfigurationBuilder.initFromProperties(sputnikProperties);

                ConnectorFacade facade = getConnectorFacade(theConfiguration);
                new Engine(facade, theConfiguration).run();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error invoking sputnik", e);
        }
    }

    private static ConnectorFacade getConnectorFacade(Configuration configuration) {
        ConnectorType theConnectorType = ConnectorType
                .getValidConnectorType(configuration.getProperty(GeneralOption.CONNECTOR_TYPE));
        ConnectorFacade theFacade = ConnectorFacadeFactory.INSTANCE.build(theConnectorType, configuration);
        theFacade.validate(configuration);
        return theFacade;
    }
}