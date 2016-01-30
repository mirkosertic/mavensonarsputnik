package de.mirkosertic.mavensonarsputnik;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

import org.sonar.runner.api.EmbeddedRunner;
import org.sonarsource.scanner.maven.DependencyCollector;
import org.sonarsource.scanner.maven.ExtensionsFactory;
import org.sonarsource.scanner.maven.bootstrap.*;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import pl.touk.sputnik.configuration.CliOption;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationBuilder;
import pl.touk.sputnik.configuration.GeneralOption;
import pl.touk.sputnik.connector.ConnectorFacade;
import pl.touk.sputnik.connector.ConnectorFacadeFactory;
import pl.touk.sputnik.connector.ConnectorType;
import pl.touk.sputnik.engine.Engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Execute Sputnik on the project and report the issues to Gerrit.
 */
@Mojo(name = "sputnik", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.INSTALL, aggregator = true)
public class MavenSonarSputnikMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private LifecycleExecutor lifecycleExecutor;

    @Component
    private RuntimeInformation runtimeInformation;

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

    @Component
    private ArtifactFactory artifactFactory;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Component
    private MavenProjectBuilder projectBuilder;

    @Component
    private ArtifactMetadataSource artifactMetadataSource;

    @Component
    private ArtifactCollector artifactCollector;

    @Component(hint = "mng-4384")
    private SecDispatcher securityDispatcher;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            Properties theSputnikProperties = new Properties();
            theSputnikProperties.load(getClass().getResourceAsStream("/default-sputnik.properties"));

            try (InputStream theStream = new FileInputStream(sputnikConfiguration)) {
                theSputnikProperties.load(theStream);
            }

            theSputnikProperties.setProperty(CliOption.CHANGE_ID.getKey(), gerritChangeId);
            theSputnikProperties.setProperty(CliOption.REVISION_ID.getKey(), gerritRevision);

            SonarExecutor theExecutor = new SonarExecutor() {
                @Override
                public File executeSonar() throws Exception {
                    ExtensionsFactory extensionsFactory = new ExtensionsFactory(getLog(), mavenSession, lifecycleExecutor, artifactFactory, localRepository, artifactMetadataSource, artifactCollector,
                            dependencyTreeBuilder, projectBuilder);
                    DependencyCollector dependencyCollector = new DependencyCollector(dependencyTreeBuilder, localRepository);
                    MavenProjectConverter mavenProjectConverter = new MavenProjectConverter(getLog(), dependencyCollector);
                    LogHandler logHandler = new LogHandler(getLog());

                    PropertyDecryptor propertyDecryptor = new PropertyDecryptor(getLog(), securityDispatcher);

                    RunnerFactory runnerFactory = new RunnerFactory(logHandler, getLog().isDebugEnabled(), runtimeInformation, mavenSession, propertyDecryptor);

                    EmbeddedRunner runner = runnerFactory.create();

                    runner.properties().setProperty("sonar.analysis.mode", "incremental");

                    new RunnerBootstrapper(getLog(), mavenSession, runner, mavenProjectConverter, extensionsFactory, propertyDecryptor).execute();

                    return new File(MavenProjectConverter.getSonarWorkDir(mavenSession.getCurrentProject()), "sonar-report.json");
                };
            };

            SonarExecutorHelper.set(theExecutor);

            Configuration theConfiguration = ConfigurationBuilder.initFromProperties(theSputnikProperties);

            ConnectorFacade facade = getConnectorFacade(theConfiguration);
            new Engine(facade, theConfiguration).run();
        } catch (Exception e) {
            throw new MojoExecutionException("Error invoking sputnik", e);
        } finally {
            SonarExecutorHelper.remove();
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