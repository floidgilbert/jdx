Build and release instructions for jdx. Instructions are specific to RStudio and Eclipse IDE for Java.

- Make sure the following packages are installed/updated.

  rmarkdown
  pander
  roxygen2
  testthat

- Close all sessions using the package.

- Uninstall jdx: `remove.packages("jdx")`. Sometimes the JAR files fail to update during build in RStudio.

- In Java project...

--- If changing the major and minor (not build) version numbers, change the version numbers for JAR file names.
    Delete previous-version JAR files in the R project folder to prevent distributing multiple versions in the R package.
    NOTE: As of this writing, there is not a JAR manifest file. If you add one, update the versions in the manifest file.
    Double-click *.jardesc objects in the project explorer to change output file names.

---- Build both source and binary-only JAR files (right-click *.jardesc files in project explorer and select `Create Jar`).

- In R project...

--- Add roxygen @export* comments to any new exported functions/classes.
    The NAMESPACE file will automatically be updated when the project is rebuilt (if using RStudio and original Rproj file).

--- Update documentation (man files and vignettes). Use `devtools::install(build_vignettes = TRUE)` to preview vignette build in package.
    Test all links in the documentation.

--- Change version numbers and dependencies in DESCRIPTION file.

--- Build and test R project.
    Run standard tests using Ctrl + Shift + T.
    Run the non-distributed tests in the tests folder above the R project folder. Be sure to read any testing instructions carefully.
    Run the tests included in the jsr223 package. (The jdx package was originally part of jsr223, and writing the tests is easier with jsr223 scripting.)
    Run CRAN tests.

--- Build R project source package.

- Update release notes/news.

- ///how to submit to CRAN, etc.
