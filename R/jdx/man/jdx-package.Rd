\name{jdx-package}
\alias{jdx-package}
\alias{jdx}

\docType{package}

\title{
Java Data Exchange for R and rJava
}

\description{
The \CRANpkg{jdx} package builds on the \CRANpkg{rJava} package to simplify and extend data exchange between \R and Java.
}

\details{
The \pkg{jdx} package works in conjunction with \pkg{rJava} to provide a potent low-level interface for the Java platform. The \pkg{jdx} package was originally developed to provide data exchange functionality for \CRANpkg{jsr223}, the high-level scripting interface for the Java platform. We provide jdx to developers who may want to extend existing rJava solutions. Developers of new applications are encouraged to use \pkg{jsr223} for rapid application development with a relatively low learning curve.

The \pkg{jdx} package converts R data structures to generic Java objects and vice versa. In particular, R vectors, matrices, n-dimensional arrays, factors, data frames, tables, environments, and lists are converted to Java objects. Java objects implementing common Java classes are converted to R vectors, matrices, n-dimensional arrays, data frames, and lists. Several options are available for data conversion including row-major vs. column-major ordering for arrays and data frames.

For sites that plan on designing and distributing packages that depend on jdx, it may be helpful to know that the jdx package does not use or load rJava's companion package \pkg{JRI} (the Java/R Interface).

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
# Convert matrix using column-major index ordering
m <- matrix(1:4, 2, 2)
o = convertToJava(m, array.order = "column-major")
getJavaClassName(o)
identical(m, convertToR(o, array.order = "column-major"))

# Convert data frame
identical(iris, convertToR(convertToJava(iris)))

# Demonstrate exact double precision
identical(pi, convertToR(convertToJava(pi, scalars.as.objects = TRUE)))
}

\keyword{ package }
