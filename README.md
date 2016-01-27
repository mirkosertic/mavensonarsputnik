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
mvn de.mirkosertic.mavensonarsputnik:sputnik:1.2:sputnik 
   -DgerritRevision=<GERRIT_REVISION_ID> 
   -DgerritChangeId=<GERRIT_CHANGE_ID> 
   -DsputnikConfiguration=<path-to-sputnik.properties> 
   -DsonarConfiguration=<path-to-sonar.properties
```

The sputnik.properties file contains authentication information to connect to Gerrit:

```
connector.host=<Gerrit host>
connector.path=<Gerrit context>
connector.port=<Gerrit port>
connector.username=<Gerrit username>
connector.password=<Gerrit password>
```

The sonar.properties file contains authentication information to connect to SonarQube:

```
sonar.jdbc.url=<JDBC url to SonarQube database>
sonar.jdbc.driverClassName=<JDBC Driver>
sonar.jdbc.username=<Sonar username>
sonar.jdbc.password=<Sonar password>
sonar.host.url=<URL to Sonar Web UI>
sonar.dynamicAnalysis=reuseReports
```

## Advanced Reporting

SonarQube can generate HTML reports for a given PatchSet. To enable this feature, you have to

* Install the Issues Reports Plugin
* Add the following lines to your sonar.properties file:
```
sonar.issuesReport.console.enable=true
sonar.issuesReport.html.enable=true
```

SonarQube will place to files inside the .sonar/issues-report Directory of the workspace:

* issues-report-light.html Contains only the new introduced and removed issues of the PatchSet
* issues-report-light.html Contains all issues of the PatchSet

These Reports can be easily integrated using the Publish HTML Post Build Action of Jenkins 

 


