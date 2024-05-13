## Running the Jakarta 11 TCK with Tomcat 11

### Overview

This is a Maven project that can be used to run the refactored TCK (Jakarta 11 onwards) with Tomcat 11.

At the moment, you will need to manually install the TCKs into your local Maven repository (see below).

### Running the EL TCK

1. Review the component TCK and Tomcat versions in `$TCK_TOMCAT/pom.xml` and edit as required.

1. `cd $TOMCAT_TCK\el-tck`

1. `mvn verify`

### Running the WebSocket TCK

1. Review the component TCK and Tomcat versions in `$TCK_TOMCAT/pom.xml` and edit as required.

1. `cd $TOMCAT_TCK\websocket-tck`

1. `mvn verify`

### Running the Servlet TCK

1. Review the component TCK and Tomcat versions in `$TCK_TOMCAT/pom.xml` and edit as required.

1. `cd $TOMCAT_TCK\servlet-tck`

1. `mvn verify`

### Running the JSP TCK

1. Review the component TCK and Tomcat versions in `$TCK_TOMCAT/pom.xml` and edit as required.

1. `cd $TOMCAT_TCK\jsp-tck`

1. `mvn verify`

### Installing the TCKs

#### Expression Language TCK

1. Download the EL TCK from https://download.eclipse.org/ee4j/jakartaee-tck/jakartaee11/staged/eftl/jakarta-expression-language-tck-6.0.0.zip

1. Extract the contents

1. `cd el-tck/artifacts`

1. Install the TCK JAR into the local Maven repository
   `mvn org.apache.Maven.plugins:Maven-install-plugin:3.1.1:install-file -Dfile=jakarta-expression-language-tck-6.0.0.jar`

1. Edit the POM created in your local Maven repository to update the version for the parent (jakarta.tck:project) from 10.0.0-SNAPSHOT to 11.0.0-M1

#### WebSocket TCK

1. Download the WebSocket TCK from https://download.eclipse.org/ee4j/jakartaee-tck/jakartaee11/staged/eftl/jakarta-websocket-tck-2.2.0.zip

1. Extract the contents

1. `cd websocket-tck/artifacts`

1. Install the TCK JARs into the local Maven repository
   `artifact-install.sh`

#### Pages TCK

1. Download the Pages TCK from https://download.eclipse.org/ee4j/pages/jakartaee11/staged/eftl/jakarta-pages-tck-4.0.0.zip

1. Extract the contents

1. `cd pages-tck/artifacts`

1. Install the TCK JARs into the local Maven repository
   `mvn org.apache.Maven.plugins:Maven-install-plugin:3.1.1:install-file -Dfile=jakarta-pages-tck-4.0.0.jar`

1. Check out the https://github.com/jakartaee/platform-tck repository

1. `cd platform-tck\lib`

1. Install the javatest JAR into the local Maven repository
   `mvn install:install-file -Dfile=javatest.jar -DgroupId=javatest -DartifactId=javatest -Dversion=5.0 -Dpackaging=jar`

#### Servlet TCK

1. Download the Pages TCK from https://download.eclipse.org/ee4j/servlet/jakartaee11/staged/eftl/jakarta-servlet-tck-6.1.0.zip

1. Extract the contents

1. `cd servlet-tck/artifacts`

1. Install the TCK JARs into the local Maven repository
   `artifact-install.sh`
