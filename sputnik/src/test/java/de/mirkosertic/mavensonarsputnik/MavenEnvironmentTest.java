package de.mirkosertic.mavensonarsputnik;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.junit.Test;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class MavenEnvironmentTest {

    @Test
    public void testInitialize() {
        assertNull(MavenEnvironment.get());

        MavenSession aMavenSession = mock(MavenSession.class);
        BuildPluginManager aBuildPluginManager = mock(BuildPluginManager.class);
        Log aLog = mock(Log.class);
        DependencyTreeBuilder aDependencyTreeBuilder = mock(DependencyTreeBuilder.class);
        ArtifactRepository aLocalRepository = mock(ArtifactRepository.class);
        SecDispatcher aSecurityDispatcher = mock(SecDispatcher.class);
        MavenProjectBuilder aProjectBuilder = mock(MavenProjectBuilder.class);
        LifecycleExecutor aLifecycleExecutor = mock(LifecycleExecutor.class);
        ArtifactFactory aArtifactFactory = mock(ArtifactFactory.class);
        ArtifactMetadataSource aArtifactMetadataSource = mock(ArtifactMetadataSource.class);
        ArtifactCollector aArtifactCollector = mock(ArtifactCollector.class);
        RuntimeInformation aRuntimeInformation = mock(RuntimeInformation.class);
        MojoExecution theExecution = mock(MojoExecution.class);

        MavenEnvironment.initialize(aMavenSession, aBuildPluginManager, aLog,
                aDependencyTreeBuilder, aLocalRepository,
                aSecurityDispatcher, aProjectBuilder,
                aLifecycleExecutor, aArtifactFactory,
                aArtifactMetadataSource, aArtifactCollector, aRuntimeInformation, theExecution);

        MavenEnvironment theMavenEnvironment = MavenEnvironment.get();
        assertNotNull(theMavenEnvironment);

        assertSame(aMavenSession, theMavenEnvironment.getMavenSession());
        assertSame(aBuildPluginManager, theMavenEnvironment.getBuildPluginManager());
        assertSame(aLog, theMavenEnvironment.getLog());
        assertSame(aDependencyTreeBuilder, theMavenEnvironment.getDependencyTreeBuilder());
        assertSame(aLocalRepository, theMavenEnvironment.getLocalRepository());
        assertSame(aSecurityDispatcher, theMavenEnvironment.getSecurityDispatcher());
        assertSame(aProjectBuilder, theMavenEnvironment.getProjectBuilder());
        assertSame(aLifecycleExecutor, theMavenEnvironment.getLifecycleExecutor());
        assertSame(aArtifactFactory, theMavenEnvironment.getArtifactFactory());
        assertSame(aArtifactMetadataSource, theMavenEnvironment.getArtifactMetadataSource());
        assertSame(aArtifactCollector, theMavenEnvironment.getArtifactCollector());
        assertSame(aRuntimeInformation, theMavenEnvironment.getRuntimeInformation());

        MavenEnvironment.cleanUp();;
        assertNull(MavenEnvironment.get());
    }
}