FROM maven as builder
WORKDIR /fn-build/local-jars

# Copy in local jars from either another image or local filesystem
COPY ice-cream-1.0-SNAPSHOT.jar .

# Install each local jar into maven repository
RUN mvn install:install-file -Dfile=ice-cream-1.0-SNAPSHOT.jar \
  -DgroupId=com.oracle.emeatechnology -DartifactId=icecream \
  -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

# Copy in project files
WORKDIR /fn-build
COPY src src
COPY pom.xml .
# **Note:**
# The pom.xml file needs to reference the local jars as dependencies
# using the groupId, artifactId and version specified in the `mvn install`

# Run the maven build
RUN ["mvn", "package", "dependency:copy-dependencies", \
     "-DincludeScope=runtime", "-Dmdep.prependGroupId=true",\
     "-DoutputDirectory=target", "--fail-never"]

# Source for runtime jars and native library (libfnunixsocket.so)
FROM fnproject/fn-java-fdk:jre11-1.0.104 as runtime-source

FROM openjdk:14.0.1-jdk-oraclelinux7
WORKDIR /function

# Copy in the runtime folder from the Fn Java base image
COPY --from=runtime-source /function/runtime/ /function/runtime

# Copy in the app and dependencies
COPY --from=builder /fn-build/target/*.jar /function/app/

# Set the entrypoint for the image, making sure to include:
# -Djava.library.path (points to libfnunixsocket.so)
# -cp (classpath) entries for
#  /function/app/*
#  /function/runtime/*
ENTRYPOINT [ "java", \
     "-XX:-UsePerfData", \
     "-XX:+UseSerialGC", "-Xshare:on", \
     "-Djava.awt.headless=true" , \
     "-Djava.library.path=/function/runtime/lib", \
     "-cp", "/function/app/*:/function/runtime/*", \
     "com.fnproject.fn.runtime.EntryPoint" ]

CMD ["com.example.fn.OnlineOrder::handleRequest"]
