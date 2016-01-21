# Maven Sonar Sputnik integration

Maven Plugin for Sputnik with Multi-Module Support.

Together with Sputnik, Jenkins and Gerrit you can easily setup a pretested commit infrastructure for semi-automatic Code Reviews.

[![Build Status](https://travis-ci.org/mirkosertic/mavensonarsputnik.svg?branch=master)](https://travis-ci.org/mirkosertic/mavensonarsputnik)

## Usage

The following command can be used in conjunction with the Gerrit Trigger on Jenkins:

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

