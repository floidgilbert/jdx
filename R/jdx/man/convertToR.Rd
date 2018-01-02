\name{convertToR}
\alias{convertToR}

\title{
Convert Java Objects to R Objects
}

\description{
The function \code{\link{convertToR}} converts the Java objects referenced by \CRANpkg{rJava} objects to R objects. The function \code{\link{convertToR}} is the inverse of \code{\link{convertToJava}}.
}

\usage{
convertToR(
  value,
  strings.as.factors = NULL,
  array.order = "row-major"
)
}

\arguments{
  \item{value}{
An \pkg{rJava} object reference.
}
  \item{strings.as.factors}{
A logical vector of length one specifying whether string vectors are automatically converted to factors when Java objects are converted to R data frames. This parameter is discussed in the vignette under \href{../doc/Introduction.html#java_maps}{Java Maps}.
}
  \item{array.order}{
A character vector of length one specifying the indexing order for n-dimensional arrays. Valid values are \code{"row-major"}, \code{"column-major"}, and \code{"column-minor"}. See \href{../doc/Introduction.html#java_arrays}{Java One-dimensional Arrays and N-dimensional Rectangular Arrays} in the vignette.
}
}

\details{
The \code{\link{convertToR}} function is not thread-safe. Do not simultaneously call \code{\link{convertToR}} from different threads in the same process. A thread-safe alternative is presented in the R documentation for \code{\link{convertToRlowLevel}}.

The \href{../doc/Introduction.html}{vignette} contains all documentation for \code{\link{convertToR}} and its inverse function \code{\link{convertToJava}}. Note that these functions are not always perfect inverses of each other. See \href{../doc/Introduction.html#conversion_issues}{Conversion Issues} for more information.
}

\value{
An R object. See the \href{../doc/Introduction.html}{vignette} for details.
}

\seealso{
  \code{\link{convertToJava}}
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

\keyword{ interface }
