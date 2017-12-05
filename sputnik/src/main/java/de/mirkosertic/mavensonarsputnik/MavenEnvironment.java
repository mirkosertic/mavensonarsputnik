package de.mirkosertic.mavensonarsputnik;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import javax.annotation.Nullable;
import java.io.File;

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
    private final RuntimeInformation runtimeInformation;
    private final MojoExecution mojoExecution;

    public static void initialize(MavenSession aMavenSession, BuildPluginManager aBuildPluginManager, Log aLog,
            DependencyTreeBuilder aDependencyTreeBuilder, ArtifactRepository aLocalRepository,
            SecDispatcher aSecurityDispatcher, MavenProjectBuilder aProjectBuilder,
            LifecycleExecutor aLifecycleExecutor, ArtifactFactory aArtifactFactory,
            ArtifactMetadataSource aArtifactMetadataSource, ArtifactCollector aArtifactCollector, RuntimeInformation aRuntimeInformation, MojoExecution aExecution) {
        ENVIRONMENT.set(new MavenEnvironment(aMavenSession, aBuildPluginManager, aLog,
                aDependencyTreeBuilder, aLocalRepository,
                aSecurityDispatcher, aProjectBuilder,
                aLifecycleExecutor, aArtifactFactory,
                aArtifactMetadataSource, aArtifactCollector, aRuntimeInformation, aExecution));
    }

    public static MavenEnvironment get() {
        return ENVIRONMENT.get();
    }

    public static void cleanUp() {
        ENVIRONMENT.remove();
    }

    public MavenEnvironment(MavenSession aMavenSession, BuildPluginManager aBuildPluginManager, Log aLog,
            DependencyTreeBuilder aDependencyTreeBuilder, ArtifactRepository aLocalRepository,
            SecDispatcher aSecurityDispatcher, MavenProjectBuilder aProjectBuilder,
            LifecycleExecutor aLifecycleExecutor, ArtifactFactory aArtifactFactory,
            ArtifactMetadataSource aArtifactMetadataSource, ArtifactCollector aArtifactCollector, RuntimeInformation aRuntimeInformation,
            MojoExecution aExecution) {
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
        runtimeInformation = aRuntimeInformation;
        mojoExecution = aExecution;
    }

    public MojoExecution getMojoExecution() {
        return this.mojoExecution;
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

    public static File getSonarWorkDir(MavenProject pom) {
        return new File(getBuildDir(pom), "sonar");
    }

    private static File getBuildDir(MavenProject pom) {
        return resolvePath(pom.getBuild().getDirectory(), pom.getBasedir());
    }

    static File resolvePath(@Nullable String path, File basedir) {
        if (path != null) {
            File file = new File(StringUtils.trim(path));
            if (!file.isAbsolute()) {
                file = (new File(basedir, path)).getAbsoluteFile();
            }

            return file;
        } else {
            return null;
        }
    }
}