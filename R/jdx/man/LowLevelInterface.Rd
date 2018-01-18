\name{convertToRlowLevel}
\alias{arrayOrderToString}
\alias{convertToRlowLevel}
\alias{createJavaToRobject}
\alias{jdxConstants}
\alias{processCompositeDataCode}

\title{
Low-level Interface for jdx
}

\description{
The functions listed here are the low-level interface for \CRANpkg{jdx} and are primarily used behind the scenes in \pkg{jsr223}, the high-level integration package for Java. However, these functions may also be useful for \CRANpkg{rJava} developers interested in a thread-safe alternative to \code{\link{convertToR}}. See the code examples for a brief outline. If multi-threaded access is not required, please use \code{\link{convertToR}}.
}

\usage{
arrayOrderToString(value)

convertToRlowLevel(
  j2r,
  data.code = NULL,
  strings.as.factors = NULL
)

createJavaToRobject()

jdxConstants()

processCompositeDataCode(
  j2r,
  composite.data.code,
  throw.exceptions = TRUE,
  warn.missing.logical = TRUE,
  warn.missing.raw = TRUE
)
}

\arguments{
  \item{value}{
An \pkg{rJava} object reference to a \code{org.fgilbert.jdx.JavaToR$ArrayOrder} enumeration value.
}
  \item{j2r}{
An \pkg{rJava} object reference to a \code{org.fgilbert.jdx.JavaToR} object. The \code{createJavaToRobject} function creates an instance.
}
  \item{data.code}{
A \pkg{jdx} data code value created with \code{processCompositeDataCode}.
}
  \item{strings.as.factors}{
Same as in \code{\link{convertToR}}.
}
  \item{composite.data.code}{
A \pkg{jdx} composite data code returned by the \code{initialize} method of \code{org.fgilbert.jdx.JavaToR}.
}
  \item{throw.exceptions}{
A logical value indicating whether to throw exceptions.
}
  \item{warn.missing.logical}{
A logical value indicating whether to raise a specific warning.
}
  \item{warn.missing.raw}{
A logical value indicating whether to raise a specific warning.
}
}

\details{
See the code examples below for a thread-safe alternative to \code{\link{convertToR}}. The low-level functional interface presented here is awkward, but it was designed to limit type inference and the number of transactions between R and the JVM, ultimately maximizing performance for \pkg{jsr223}.
}

\seealso{
  \code{\link{convertToJava}}
  , \code{\link{convertToR}}
  , \code{\link{getJavaClassName}}
}

\examples{
library("jdx")

# Create org.fgilbert.jdx.JavaToR object used to convert 
# java objects to R objects. Create one of these objects
# per thread for thread-safe execution.
# 
# It is also possible to create and use the JavaToR
# object in Java and return a reference to R via rJava.
j2r <- createJavaToRobject()

# Pass the Java object to be converted to the initialize 
# method of the JavaToR object. Note that the Java object
# must be cast as java.lang.Object. The initialize method
# returns an integer value known as a composite data code
# that is used to derive the R structure.
composite.data.code <- rJava::.jcall(
  j2r
  , "I"
  , "initialize"
  , rJava::.jcast(convertToJava(iris))
)

# Process the resulting composite data code to get a data
# code vector. This step also raises any applicable 
# errors/warnings.
data.code <- processCompositeDataCode(j2r, composite.data.code)

# Pass the JavaToR object and the data code to
# convertToRlowLevel to get the R object.
convertToRlowLevel(j2r, data.code, strings.as.factors = FALSE)

# When converting n-dimensional arrays, pass an array 
# ordering constant to the initialize method.
array.order.constants <- jdxConstants()$ARRAY_ORDER
array <- convertToJava(as.matrix(iris[1:4]), array.order = "column-major")
composite.data.code <- rJava::.jcall(
  j2r
  , "I"
  , "initialize"
  , rJava::.jcast(array)
  , array.order.constants[["column-major"]]
)
data.code <- processCompositeDataCode(j2r, composite.data.code)
convertToRlowLevel(j2r, data.code)
}

\keyword{ interface }

