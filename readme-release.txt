///finish this. refer to build instructions.

Release instructions for jdx. Instructions are specific to RStudio and Eclipse IDE for Java.

- Close all sessions using the package.

- Uninstall jdx. Sometimes the JAR files fail to update during build in RStudio.
    remove.packages("jdx")
    
- In Java project...

--- If changing the major and minor (not build) version numbers, change the version numbers for JAR file names.
    Delete previous-version JAR files in the R project folder to prevent distributing multiple versions in the R package.
    NOTE: As of this writing, there is not a JAR manifest file. If you add one, update the versions in the manifest file.
    Double-click *.jardesc objects in the project to change output file names.
    Build both source and binary-only JAR files (right-click *.jardesc files and select `Create Jar`).

- In R project...

--- Add roxygen @export* comments to any new exported functions/classes.
    The NAMESPACE file will automatically be updated when the project is rebuilt (if using RStudio and original Rproj file).

--- Update documentation (*.RD man files)

--- Change version numbers and dependencies in DESCRIPTION file.

--- Build and test R project.
    Run standard tests using Ctrl + Shift + T.
    Run the non-distributed tests in the tests folder above the project folder. Be sure to read any testing instructions carefully.
    Run CRAN tests using ///update project check options.

--- Build R project source package.

- Update release notes/news.

- ///how to submit to CRAN, etc.
