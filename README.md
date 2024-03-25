## Running the Jakarta 11 TCK with Tomcat 11

### Overview

This is a Maven project that can be used to run the refactored TCK (Jakarta 11 onwards) with Tomcat 11.

At the moment, you will need to build the TCK locally (see below).

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

### Running against a local build of the TCK

1. Checkout the tckrefactor branch of the jakarta-tck repository to `$TCK`

1. `cd $TCK`

1. Install javatest into the local maven repository
   `mvn -e -ntp install:install-file -Dfile=./lib/javatest.jar -DgroupId=javatest -DartifactId=javatest -Dversion=5.0 -Dpackaging=jar`
   
1. Install the top-level project Jakarta TCK POM
   `mvn --non-recursive install`

1. Install the various libraries required by the TCKs Tomcat tests against
```
   cd $TCK/libutil
   mvn install
   
   cd $TCK/runtime
   mvn install
   
   cd $TCK/common
   mvn install

   cd $TCK/assembly
   mvn install

   cd $TCK/webartifacts/servlet
   mvn install

   cd $TCK/webartifacts/jsp
   mvn install

   cd $TCK/signaturetest
   mvn install

   cd $TCK/user_guides
   mvn install
```

6. Install the TCKs Tomcat tests against
```
   cd $TCK/el
   mvn install
   
   cd $TCK/servlet
   mvn install
   
   cd $TCK/jsp
   mvn install
   
   cd $TCK/websocket
   mvn install
```

7. Review the component TCK and Tomcat versions in `$TCK_TOMCAT/pom.xml` and edit as required. In particular, you'll
need to configure the TCKs to use the SNAPSHOT versions you built in the previous steps.
