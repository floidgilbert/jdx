\name{jdx-package}
\alias{jdx-package}
\alias{jdx}

\docType{package}

\title{
Java Data Exchange for R and rJava
}

\description{
The \CRANpkg{jdx} package builds on the \CRANpkg{rJava} package to simplify and extend data exchange between R and Java.
}

\details{
The \pkg{jdx} package works in conjunction with \pkg{rJava} to provide a low-level interface for the Java platform. The \pkg{jdx} package was originally developed to provide data exchange functionality for \CRANpkg{jsr223}, the high-level scripting interface for the Java platform. We provide \pkg{jdx} to developers who may want to extend existing \pkg{rJava} solutions. Developers of new applications are encouraged to use \pkg{jsr223} for rapid application development with a relatively low learning curve.

The \pkg{jdx} package converts R data structures to generic Java objects and vice versa. In particular, R vectors, n-dimensional arrays, factors, data frames, tables, environments, and lists are converted to Java objects. Java scalars and n-dimensional arrays are converted to R vectors and n-dimensional arrays. Java maps and collections are converted to R lists, data frames, vectors, or n-dimensional arrays depending on content. Several options are available for data conversion including row-major vs. column-major ordering for arrays and data frames.

For sites that plan on designing and distributing packages that depend on \pkg{jdx}, it may be helpful to know that the \pkg{jdx} package does not use or load rJava's companion package \pkg{JRI} (the Java/R Interface).

For best results, please refer to the vignette \href{../doc/Introduction.html}{Introduction to jdx: Java Data Exchange for R and rJava}.
}

\author{
Floid R. Gilbert \email{floid.r.gilbert@gmail.com}, David B. Dahl \email{dahl@stat.byu.edu}
}

\seealso{
  \code{\link{convertToJava}}
  , \code{\link{convertToR}}
  , \code{\link{getJavaClassName}}
}

\examples{
library("jdx")

# Convert matrix using column-major index ordering
m <- matrix(1:4, 2, 2)
o = convertToJava(m, array.order = "column-major")
getJavaClassName(o)
identical(m, convertToR(o, array.order = "column-major"))

# Convert 4-dimensional array using row-major index ordering
dimensions <- c(3, 2, 2, 2)
a = array(1:prod(dimensions), dimensions)
o = convertToJava(a, array.order = "row-major")
getJavaClassName(o)
identical(a, convertToR(o, array.order = "row-major"))

# Convert data frame
identical(iris, convertToR(convertToJava(iris)))

# Demonstrate exact double precision
identical(pi, convertToR(convertToJava(pi, scalars.as.objects = TRUE)))
}

\keyword{ package }
