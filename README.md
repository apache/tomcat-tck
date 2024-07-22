## Running the Jakarta 11 TCK with Tomcat 11

### Overview

This is a Maven project that can be used to run the refactored TCK (Jakarta 11 onwards) with Tomcat 11.

### Running the Annotations TCK

1. Review the component TCK and Tomcat versions in `$TCK_TOMCAT/pom.xml` and edit as required.

1. `cd $TOMCAT_TCK\annotations-tck`

1. `mvn verify`

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
