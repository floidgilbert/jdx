# jdx - Java Data Exchange for R and rJava

The [jdx](https://cran.r-project.org/package=jdx) package builds on Simon Urbanek's [rJava](https://cran.r-project.org/package=rJava) package to simplify and extend data exchange between R and Java. The jdx package was originally developed to provide data exchange functionality for the [jsr223](https://cran.r-project.org/package=jsr223) package, a high-level scripting interface for the Java platform. We provide jdx to developers who may want to extend existing rJava solutions. Developers of new applications are encouraged to use jsr223 for rapid application development with a relatively low learning curve.

The jdx package converts R data structures to generic Java objects and vice versa. In particular, R vectors, n-dimensional arrays, factors, data frames, tables, environments, and lists are converted to Java objects. Java scalars and n-dimensional arrays are converted to R vectors and n-dimensional arrays. Java maps and collections are converted to R lists, data frames, vectors, or n-dimensional arrays depending on content. Several options are available for data conversion including various ordering schemes for arrays and data frames.

For full documentation, [see the vignette](https://cran.r-project.org/web/packages/jdx/vignettes/Introduction.html) included with the R package.

