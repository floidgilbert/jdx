\name{getJavaClassName}
\alias{getJavaClassName}

\title{
Get a Java Object's Class Name
}

\description{
The function \code{\link{getJavaClassName}} returns the class name for the Java object behind an \CRANpkg{rJava} object reference.
}

\usage{
getJavaClassName(value)
}

\arguments{
  \item{value}{
An \pkg{rJava} object reference.
}
}

\value{
A character vector of length one containing the class name of the Java object.
}

\seealso{
  \code{\link{convertToJava}}
  , \code{\link{convertToR}}
}

\examples{
library("jdx")
getJavaClassName(convertToJava(matrix(1:4, 2, 2)))
getJavaClassName(convertToJava(iris))
}

\keyword{ interface }
