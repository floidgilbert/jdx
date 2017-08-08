\name{jdx-package}
\alias{jdx-package}
\alias{jdx}

\docType{package}

\title{
Java Data Exchange for R and rJava
}

\description{
The \pkg{jdx} package builds on the \pkg{rJava} package to simplify and extend data exchange between \R and Java.
}

\details{
The \pkg{jdx} package works in conjunction with \pkg{rJava} to provide a potent low-level interface for the Java platform. The \pkg{jdx} package was originally developed to provide data exchange for \pkg{jsr223}, the high-level scripting interface for the Java platform. Developers interested in rapid application development in \R for Java should consider \pkg{jsr223}.

The \pkg{jdx} package converts R data structures (vectors, matrices, n-dimensional arrays, factors, data frames, tables, environments, lists, named lists, and nested lists) to generic Java objects. Generic Java objects are likewise converted to R objects using intuitive rules. Several options are available for data conversion such as row-major vs. column-major ordering for arrays and data frames. The details of data conversion are outlined in the vignette: ///.///also put vignette at the bottom.

The functionality of \pkg{jdx} is similar to that of \pkg{jsonlite} (Ooms///) except that 
///mention jsonlite and say that it is faster and more precise, binary. less overhead.
}

\author{
Floid R. Gilbert \email{floid.r.gilbert@gmail.com}, David B. Dahl \email{dahl@stat.byu.edu}
}

\seealso{
  \code{\link{convertToJava}}
  , \code{\link{convertToR}}
}

\examples{
m <- matrix(1:4, 2, 2)
o = convertToJava(m, array.order = "column-major")
convertToR(o, array.order = "column-major")
convertToR(o, array.order = "row-major")
getJavaClassName(o)
}

\keyword{ package }
