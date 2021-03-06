\name{convertToJava}
\alias{convertToJava}

\title{
Convert R Objects to Java Objects
}

\description{
The function \code{\link{convertToJava}} converts R objects to generic Java objects for use with the \CRANpkg{rJava} package. This function simplifies and extends data exchange for \pkg{rJava}. The function \code{\link{convertToJava}} is the inverse of \code{\link{convertToR}}.
}

\usage{
convertToJava(
  value,
  length.one.vector.as.array = FALSE,
  scalars.as.objects = FALSE,
  array.order = "row-major",
  data.frame.row.major = TRUE,
  coerce.factors = TRUE
)
}

\arguments{
  \item{value}{
An R vector, matrix, n-dimensional array, table, factor, data frame, list, or environment. Nested lists are supported. Supported data types: numeric, integer, character, logical, and raw.

}
  \item{length.one.vector.as.array}{
A logical vector of length one. See \href{../doc/Introduction.html#r_vectors_of_length_one}{R Vectors of Length One} in the vignette.
}
  \item{scalars.as.objects}{
A logical vector of length one. See \href{../doc/Introduction.html#r_vectors_of_length_one}{R Vectors of Length One} in the vignette.
}
  \item{array.order}{
A character vector of length one specifying the order when copying R n-dimensional arrays to Java. Valid values are \code{"row-major"}, \code{"column-major"}, and \code{"column-minor"}. See \href{../doc/Introduction.html#r_n_dimensional_arrays}{R Matrices and N-dimensional Arrays} in the vignette.
}
  \item{data.frame.row.major}{
A logical vector of length one. When \code{TRUE} (the default), a data frame is converted to a list of map objects that represent rows. When \code{FALSE}, a data frame is converted to a map of arrays that represent columns. Conversion for column-major order is much faster than row-major order. See \href{../doc/Introduction.html#r_data_frames}{R Data Frames} in the vignette.
}
  \item{coerce.factors}{
A logical vector of length one. When \code{TRUE} (the default), an attempt is made to coerce the character values backing factors to integer, numeric, or logical vectors. If coercion fails, the factor is converted to a character vector. When \code{FALSE}, the factor is converted to a character vector. This parameter affects standalone factors as well as factors present in data frames and lists. See \href{../doc/Introduction.html#r_factors}{R Factors} in the vignette.
}
}

\details{
The \code{\link{convertToJava}} function is used to create objects that can be used as method parameters in the \pkg{rJava} package. R vectors, matrices, n-dimensional arrays, tables, factors, data frames, environments, lists, named lists, and nested lists are supported as well as data types numeric, integer, logical, character and raw.

The \href{../doc/Introduction.html}{vignette} contains all documentation for \code{\link{convertToJava}} and its inverse function \code{\link{convertToR}}. Note that these functions are not always perfect inverses of each other. See \href{../doc/Introduction.html#conversion_issues}{Conversion Issues} for more information.
}

\value{
A Java object reference or an R vector. See the \href{../doc/Introduction.html}{vignette} for details.
}

\seealso{
  \code{\link{convertToR}}
  , \code{\link{getJavaClassName}}
}

\examples{
library("jdx")

# Convert matrix using column-major ordering
m <- matrix(1:4, 2, 2)
o = convertToJava(m, array.order = "column-major")
getJavaClassName(o)
identical(m, convertToR(o, array.order = "column-major"))

# Convert 4-dimensional array using row-major ordering
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
