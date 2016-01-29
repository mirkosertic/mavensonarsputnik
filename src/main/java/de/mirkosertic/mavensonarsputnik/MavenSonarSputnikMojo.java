package de.mirkosertic.mavensonarsputnik;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

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

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepositories;

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

    /**
     * Include maven dependencies into analysis.
     */
    @Parameter(defaultValue = "true", required = true)
    private boolean includeLibraries;

    private Properties sputnikProperties;

    private Properties sonarProperties;

    private boolean isIgnored(MavenProject aProject, File aFile) {
        if (ignoreGeneratedCode) {
            return aFile.toString().startsWith(aProject.getBuild().getDirectory());
        }
        return false;
    }

    private void resolveArtifacts(MavenProject aProject, StringBuilder aStringBuilder, String aScope) {
        Set<File> theAlreadyProcessed = new HashSet<>();
        for (Dependency theDependency : aProject.getDependencies()) {

            try {
                CollectRequest theCollectRequest = new CollectRequest();
                theCollectRequest.setRoot(new org.eclipse.aether.graph.Dependency(
                        new DefaultArtifact(theDependency.getGroupId(), theDependency.getArtifactId(),
                                theDependency.getClassifier(), theDependency.getType(), theDependency.getVersion()), aScope));
                for (RemoteRepository theRepository : remoteRepositories) {
                    theCollectRequest.addRepository(theRepository);
                }

                DependencyFilter theDependencyFilter = DependencyFilterUtils.classpathFilter(aScope);
                DependencyRequest theDependencyRequest = new DependencyRequest(theCollectRequest, theDependencyFilter);

                DependencyResult theDependencyResult = repositorySystem
                        .resolveDependencies(repositorySystemSession, theDependencyRequest);

                for (ArtifactResult theArtifactResult : theDependencyResult.getArtifactResults()) {
                    Artifact theResolved = theArtifactResult.getArtifact();

                    File theFile = theResolved.getFile();
                    if (theFile != null && theAlreadyProcessed.add(theFile)) {
                        if (aStringBuilder.length() > 0) {
                            aStringBuilder.append(",");
                        }
                        aStringBuilder.append(theFile);
                    }
                }
            } catch (Exception e) {
                getLog().warn("Error resolving " + theDependency, e);
            }
        }
    }

    private void listModulesFor(String aPrefix, MavenProject aProject, int aRecursionLevel, List<MavenProject> aProjectList) {
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
                sonarProperties.put(aPrefix + ".java.test.binaries" , theTestOutputDirectory.toString());
            }

            if (includeLibraries) {
                StringBuilder theLibraries = new StringBuilder();
                resolveArtifacts(aProject, theLibraries, JavaScopes.COMPILE);
                if (theLibraries.length() > 0) {
                    sonarProperties.put(aPrefix + ".java.libraries", theLibraries.toString());
                }

                StringBuilder theTestLibraries = new StringBuilder();
                resolveArtifacts(aProject, theTestLibraries, JavaScopes.TEST);
                if (theTestLibraries.length() > 0) {
                    sonarProperties.put(aPrefix + ".java.test.libraries", theTestLibraries.toString());
                }
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