package de.mirkosertic.mavensonarsputnik;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

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

    @Component
    private BuildPluginManager pluginManager;

    /**
     * The gerrit change id.
     */
    @Parameter(defaultValue = "${gerritChangeId}")
    private String gerritChangeId;

    /**
     * The gerrit revision id.
     */
    @Parameter(defaultValue = "${gerritRevision}")
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

            String theChangeID = gerritChangeId;
            if (StringUtils.isEmpty(theChangeID)) {
                theChangeID = System.getProperty("GERRIT_CHANGE_ID");
            }
            if (StringUtils.isEmpty(theChangeID)) {
                theChangeID = System.getenv("GERRIT_CHANGE_ID");
            }
            if (StringUtils.isEmpty(theChangeID)) {
                getLog().info("Disabling Plugin as no GERRIT_CHANGE_ID was set in the environment, as a system property or the plugin configuration (gerritChangeId).");
                return;
            }

            String theRevision = gerritRevision;
            if (StringUtils.isEmpty(theRevision)) {
                theRevision = System.getProperty("GERRIT_PATCHSET_REVISION");
            }
            if (StringUtils.isEmpty(theRevision)) {
                theRevision = System.getenv("GERRIT_PATCHSET_REVISION");
            }
            if (StringUtils.isEmpty(theRevision)) {
                getLog().info("Disabling Plugin as no GERRIT_PATCHSET_REVISION was set in the environment, as a system property or the plugin configuration (gerritRevision).");
                return;
            }

            theSputnikProperties.setProperty(CliOption.CHANGE_ID.getKey(), theChangeID);
            theSputnikProperties.setProperty(CliOption.REVISION_ID.getKey(), theRevision);

            MavenEnvironment.initialize(mavenSession, pluginManager, getLog(),
                    dependencyTreeBuilder, localRepository,
                    securityDispatcher, projectBuilder,
                    lifecycleExecutor, artifactFactory,
                    artifactMetadataSource, artifactCollector, runtimeInformation);

            Configuration theConfiguration = ConfigurationBuilder.initFromProperties(theSputnikProperties);

            ConnectorFacade facade = getConnectorFacade(theConfiguration);
            new Engine(facade, theConfiguration).run();
        } catch (Exception e) {
            throw new MojoExecutionException("Error invoking sputnik", e);
        }
    }

    private static ConnectorFacade getConnectorFacade(Configuration aConfiguration) {
        ConnectorType theConnectorType = ConnectorType
                .getValidConnectorType(aConfiguration.getProperty(GeneralOption.CONNECTOR_TYPE));
        ConnectorFacade theFacade = ConnectorFacadeFactory.INSTANCE.build(theConnectorType, aConfiguration);
        theFacade.validate(aConfiguration);
        return theFacade;
    }
}