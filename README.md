# hand-made

This is an example to show how to add a local jar (i.e. not in a central Maven repository) to a Java Fn function.

To do this you need to do a Docker Multi - Stage build.

At the build stage, you will:
- add the local jar(s),
- install them into the Maven repository within the build container
- update the pom.xml
- run the Maven build

For the production stage, you copy the output of the build stage into the new image and update the `CMD`

This method only copies the runtime libraries from the `fn-java-fdk` image and so should work for jars
that have been compiled using a later version of the JDK than is used in the FDK images.

## Dockerfile

There is an example `Dockerfile` for each of the two functions in the repository.

They are very similar, so let's walk through the [`Dockerfile`](./online-order/Dockerfile) for `online-order`:

### Builder


We need to run a Maven build, so start with:
```
FROM maven as builder
```

### Local jar(s)
Set `WORKDIR`, copy in local jars and install them into the local Maven repository:
```
WORKDIR /fn-build/local-jars

# Copy in local jars from either another image or local filesystem
COPY ice-cream-1.0-SNAPSHOT.jar .

# Install each local jar into maven repository
RUN mvn install:install-file -Dfile=ice-cream-1.0-SNAPSHOT.jar \
  -DgroupId=com.oracle.emeatechnology -DartifactId=icecream \
  -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
```

### Project files

Copy in the source and the `pom.xml` files for the project.
The `pom.xml` must contain a dependency for each local jar.
```
# Copy in project files
WORKDIR /fn-build
COPY src src
COPY pom.xml .
# **Note:**
# The pom.xml file needs to reference the local jars as dependencies
# using the groupId, artifactId and version specified in the `mvn install`
```

### Maven build
Then run the maven build in the `builder` stage.
```
# Run the maven build
RUN ["mvn", "package", "dependency:copy-dependencies", \
     "-DincludeScope=runtime", "-Dmdep.prependGroupId=true",\
     "-DoutputDirectory=target", "--fail-never"]
```

### Runtime source
We need some libraries from the Fn Java FDK, and the easiest way to get them is from the `fn-java-fdk` image.
```
# Source for runtime jars and native library (libfnunixsocket.so)
FROM fnproject/fn-java-fdk as runtime-source
```

### Runtime image
We need an up to date runtime image.  In this case, we're using JDK14 on Oracle Linux.

We copy in the runtime libraries from the `runtime-source` stage, and the app and dependencies from the `builder`.
```
FROM openjdk:14.0.1-jdk-oraclelinux7
WORKDIR /function

# Copy in the runtime folder from the Fn Java base image
COPY --from=runtime-source /function/runtime/ /function/runtime

# Copy in the app and dependencies
COPY --from=builder /fn-build/target/*.jar /function/app/
```

### Entrypoint
Set the `ENTRYPOINT` for the container, taking particular note of the `java.library.path` and classpath (`-cp`).
```
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
```

### Command (`CMD`)
Set the command to indicate the class and method that should be executed for the function.
```
CMD ["com.example.fn.OnlineOrder::handleRequest"]
```

## Create a function
You want Fn to build the function using your custom Dockerfile.
To do this run `fn init`:
```
$ fn init                                             [15:12:18]
Dockerfile found. Using runtime 'docker'.
func.yaml created.
$ cat func.yaml                                       [15:13:12]
schema_version: 20180708
name: online-order
version: 0.0.1
runtime: docker
```

Check that you can build from the Dockerfile:
`fn build --verbose`

## Deploy the function
if `fn build --verbose` works, then run `fn deploy`, e.g.
```
fn deploy --app hand-made
$ fn invoke hand-made online-order < bubblegum.json
14.3
```
Sadly, you don't get an ice cream :-(


## Example

For a working example, see the code in this repository.
