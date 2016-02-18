# Maven Sonar Sputnik Integration

Maven Plugin for Sputnik with Multi-Module Support.

Together with [Sputnik](https://github.com/TouK/sputnik), [Jenkins](https://jenkins-ci.org) and [Gerrit](https://www.gerritcodereview.com) you can easily setup a pretested commit infrastructure for semi-automatic Code Reviews.

[![codecov.io](https://codecov.io/github/mirkosertic/mavensonarsputnik/coverage.svg?branch=master)](https://codecov.io/github/mirkosertic/mavensonarsputnik?branch=master) [![Build Status](https://travis-ci.org/mirkosertic/mavensonarsputnik.svg?branch=master)](https://travis-ci.org/mirkosertic/mavensonarsputnik) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.mavensonarsputnik/sputnik/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.mavensonarsputnik/sputnik/badge.svg)

## System Requirements

* Java >=7
* Maven >= 3.2.5
* SonarQube >= 4.5

## Usage

The following command can be used in conjunction with the [Gerrit Trigger](https://wiki.jenkins-ci.org/display/JENKINS/Gerrit+Trigger) on Jenkins:

```
mvn de.mirkosertic.mavensonarsputnik:sputnik:1.6:sputnik 
   -DgerritRevision=<GERRIT_REVISION_ID> 
   -DgerritChangeId=<GERRIT_CHANGE_ID> 
   -DsputnikConfiguration=<path-to-sputnik.properties> 
```

The sputnik.properties file contains authentication information to connect to Gerrit:

```
connector.host=<Gerrit host>
connector.path=<Gerrit context>
connector.port=<Gerrit port>
connector.username=<Gerrit username>
connector.password=<Gerrit password>
sonar.enabled=true
sonar.configurationFile=<path to sonar.properties>
```

The sonar.properties file contains authentication information to connect to SonarQube:

```
# Only Required if you are not declaring SonarQube configuration in pom.xml
sonar.jdbc.url=<JDBC url to SonarQube database>
sonar.jdbc.driverClassName=<JDBC Driver>
sonar.jdbc.username=<Sonar username>
sonar.jdbc.password=<Sonar password>
sonar.host.url=<URL to Sonar Web UI>
```

## Advanced Reporting

### Mutation Testing

This plugin can integrate Mutation Testing results based on [PITest](http://pitest.org) in the review. To enable this,
PITest must be executed as part of the Maven build.

Additional goals and configuration:

```
mvn org.pitest:pitest-maven:1.1.9:scmMutationCoverage -DanalyseLastCommit=true
```

You also need to enable the PITest Reviewer in the sputnik.properties file by adding the following line:

```
pitest.enabled=true
```

### OWASP Dependency Checks

This plugin also runs a [OWASP Dependency Check](https://www.owasp.org/index.php/OWASP_Dependency_Check) in case of any changes at the Maven project configuration, hence if a pom.xml is part of the current patchset.

To enable the OWASP Dependency Reviewer in the sputnik.properties file by adding the following line:

```
owaspdependencycheck.enabled=true
```

### Automated Quality Feedback

The Maven plugin can add reports to the review comments. For instance, a SonarQube Plugin can generate a simple text file containing statistics about the submitted change and how it affects SonarQube metrics. This file is stored by the Plugin and can be read and added as a review comment.

Report embedding can be enabled by the following line in the sonar.properties file:

```
sonar.additionalReviewCommentFiles=<comma separated list name of text file to embedd as review comment>
```

The reports must be stored in the SonarRunner working directory, project-root/.sonar.

An example Report can be generated using [Sonar Delta Report Plugin](https://github.com/mirkosertic/sonardeltareport).

### Additional SonarQube Reports

SonarQube can generate HTML reports for a given PatchSet. To enable this feature, you have to

* Install the Issues Reports Plugin
* Add the following lines to your sonar.properties file:
```
# This are already the default values
sonar.issuesReport.console.enable=true
sonar.issuesReport.html.enable=true
```

SonarQube will place to files inside the .sonar/issues-report Directory of the workspace:

* issues-report-light.html contains only the new introduced and removed issues of the PatchSet
* issues-report.html contains all issues of the PatchSet

These Reports can be easily integrated using the Publish HTML Post Build Action of Jenkins
