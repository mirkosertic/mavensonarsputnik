# Maven Sonar Sputnik Integration

Maven Plugin for Sputnik with Multi-Module Support.

Together with [Sputnik](https://github.com/TouK/sputnik), [Jenkins](https://jenkins-ci.org) and [Gerrit](https://www.gerritcodereview.com) you can easily setup a pretested commit infrastructure for semi-automatic Code Reviews.

[![Build Status](https://travis-ci.org/mirkosertic/mavensonarsputnik.svg?branch=master)](https://travis-ci.org/mirkosertic/mavensonarsputnik) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.mavensonarsputnik/sputnik/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.mavensonarsputnik/sputnik/badge.svg)

## Usage

The following command can be used in conjunction with the [Gerrit Trigger](https://wiki.jenkins-ci.org/display/JENKINS/Gerrit+Trigger) on Jenkins:

```
mvn de.mirkosertic.mavensonarsputnik:sputnik:sputnik 
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
```

