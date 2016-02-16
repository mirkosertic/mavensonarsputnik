package de.mirkosertic.mavensonarsputnik;

import java.io.File;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class MavenEnvironment {

    private final static ThreadLocal<MavenEnvironment> ENVIRONMENT = new ThreadLocal();

    private final MavenSession mavenSession;
    private final BuildPluginManager buildPluginManager;
    private final Log log;
    private final DependencyTreeBuilder dependencyTreeBuilder;
    private final ArtifactRepository localRepository;
    private final SecDispatcher securityDispatcher;
    private final MavenProjectBuilder projectBuilder;
    private final LifecycleExecutor lifecycleExecutor;
    private final ArtifactFactory artifactFactory;
    private final ArtifactMetadataSource artifactMetadataSource;
    private final ArtifactCollector artifactCollector;
    private final File sonarConfiguration;
    private final RuntimeInformation runtimeInformation;

    public static void initialize(MavenSession aMavenSession, BuildPluginManager aBuildPluginManager, Log aLog,
            DependencyTreeBuilder aDependencyTreeBuilder, ArtifactRepository aLocalRepository,
            SecDispatcher aSecurityDispatcher, MavenProjectBuilder aProjectBuilder,
            LifecycleExecutor aLifecycleExecutor, ArtifactFactory aArtifactFactory,
            ArtifactMetadataSource aArtifactMetadataSource, ArtifactCollector aArtifactCollector, File aSonarConfiguration, RuntimeInformation aRuntimeInformation) {
        ENVIRONMENT.set(new MavenEnvironment(aMavenSession, aBuildPluginManager, aLog,
                aDependencyTreeBuilder, aLocalRepository,
                aSecurityDispatcher, aProjectBuilder,
                aLifecycleExecutor, aArtifactFactory,
                aArtifactMetadataSource, aArtifactCollector, aSonarConfiguration, aRuntimeInformation));
    }

    public static MavenEnvironment get() {
        return ENVIRONMENT.get();
    }

    public MavenEnvironment(MavenSession aMavenSession, BuildPluginManager aBuildPluginManager, Log aLog,
            DependencyTreeBuilder aDependencyTreeBuilder, ArtifactRepository aLocalRepository,
            SecDispatcher aSecurityDispatcher, MavenProjectBuilder aProjectBuilder,
            LifecycleExecutor aLifecycleExecutor, ArtifactFactory aArtifactFactory,
            ArtifactMetadataSource aArtifactMetadataSource, ArtifactCollector aArtifactCollector, File aSonarConfiguration, RuntimeInformation aRuntimeInformation) {
        mavenSession = aMavenSession;
        buildPluginManager = aBuildPluginManager;
        log = aLog;
        dependencyTreeBuilder = aDependencyTreeBuilder;
        localRepository = aLocalRepository;
        securityDispatcher = aSecurityDispatcher;
        projectBuilder = aProjectBuilder;
        lifecycleExecutor = aLifecycleExecutor;
        artifactFactory = aArtifactFactory;
        artifactMetadataSource = aArtifactMetadataSource;
        artifactCollector = aArtifactCollector;
        sonarConfiguration = aSonarConfiguration;
        runtimeInformation = aRuntimeInformation;
    }

    public MavenSession getMavenSession() {
        return mavenSession;
    }

    public BuildPluginManager getBuildPluginManager() {
        return buildPluginManager;
    }

    public Log getLog() {
        return log;
    }

    public DependencyTreeBuilder getDependencyTreeBuilder() {
        return dependencyTreeBuilder;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public SecDispatcher getSecurityDispatcher() {
        return securityDispatcher;
    }

    public MavenProjectBuilder getProjectBuilder() {
        return projectBuilder;
    }

    public RuntimeInformation getRuntimeInformation() {
        return runtimeInformation;
    }

    public LifecycleExecutor getLifecycleExecutor() {
        return lifecycleExecutor;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public ArtifactMetadataSource getArtifactMetadataSource() {
        return artifactMetadataSource;
    }

    public ArtifactCollector getArtifactCollector() {
        return artifactCollector;
    }

    public File getSonarConfiguration() {
        return sonarConfiguration;
    }
}