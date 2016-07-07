package de.mirkosertic.mavensonarsputnik.processor.owasp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OWASPDependencyCheckProcessorTest {

    @Test
    public void testMavenIdentifier() {
        OWASPDependencyCheckProcessor.MavenIdentifier theIdentifier = new OWASPDependencyCheckProcessor.MavenIdentifier("(org.springframework.security.oauth:spring-security-oauth2:2.0.4.RELEASE)");
        assertEquals("org.springframework.security.oauth", theIdentifier.getGroupId());
        assertEquals("spring-security-oauth2", theIdentifier.getArtifactId());
        assertEquals("2.0.4.RELEASE", theIdentifier.getVersion());
    }

}