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
The functions listed here are the low-level interface for \CRANpkg{jdx} and are primarily used behind the scenes in \CRANpkg{jsr223}, the high-level integration package for Java. However, these functions may be used by \CRANpkg{rJava} developers interested in a thread-safe alternative to \code{\link{convertToR}}. See the code examples for a brief outline. If multi-threaded access is not required, please use the high-level \pkg{jdx} interface \code{\link{convertToJava}}.
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
An \pkg{rJava} object reference to a \code{org.fgilbert.jdx.JavaToR} object. Use \code{createJavaToRobject} to create an instance.
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
See the sample code below for a thread-safe alternative to \code{\link{javaToR}}. The details of each function are not included here whereas the high-level interface is recommended for most applications.
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
j2r <- jdx::createJavaToRobject()

# Pass the Java object to be converted to the initialize 
# method of the JavaToR object. Note that the Java object
# must be cast as java.lang.Object. The initialize method
# returns an integer value known as a composite data code.
composite.data.code <- rJava::.jcall(
  j2r
  , "I"
  , "initialize"
  , rJava::.jcast(convertToJava(iris))
)

# Process the resulting composite data code to get a data
# code vector. This step also raises errors/warnings.
data.code <- processCompositeDataCode(j2r, composite.data.code)

# Pass the JavaToR object and the data code to
# convertToRlowLevel to get the resulting R object.
convertToRlowLevel(j2r, data.code, strings.as.factors = FALSE)

# When converting n-dimensional arrays, pass an array 
# ordering constant to the initialize method.
array.order.constants <- jdxConstants()$ARRAY_ORDER
composite.data.code <- rJava::.jcall(
  j2r
  , "I"
  , "initialize"
  , rJava::.jcast(convertToJava(as.matrix(iris[1:4]), array.order = "column-major"))
  , array.order.constants[["column-major"]]
)
data.code <- processCompositeDataCode(j2r, composite.data.code)
convertToRlowLevel(j2r, data.code)
}

\keyword{ interface }

