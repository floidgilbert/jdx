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
The functions listed here are part of the low-level interface for \CRANpkg{jdx} and are primarily used behind the scenes in \CRANpkg{jsr223}, the high-level integration package for Java. Java developers are encouraged to use \pkg{jsr223} or the high-level \pkg{jdx} functions \code{\link{convertToJava}}, \code{\link{convertToR}}, and \code{\link{getJavaClassName}}. ///However, the low-level interface may be of interest to \CRANpkg{rJava} developers interested in thr \pkg{} ///provide/edit simple example of thread-safe version.
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
An \pkg{rJava} object reference to a \code{org.fgilbert.jdx.JavaToR} enumeration value.
}
  \item{j2r}{
An \pkg{rJava} object reference to a \code{org.fgilbert.jdx.JavaToR} object. Use \code{createJavaToRobject} to create an instance.
}
  \item{data.code}{
A \pkg{jdx} data code value created with \code{processCompositeDataCode}.
}
  \item{strings.as.factors}{
See \code{\link{convertToJava}}.
}
  \item{composite.data.code}{
A \pkg{jdx} composite data code value.
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

\seealso{
  \code{\link{convertToJava}}
  , \code{\link{convertToR}}
  , \code{\link{getJavaClassName}}
}

\examples{
library("jdx")

#///describe what's going on.
j2r <- jdx::createJavaToRobject() #///create one object per thread. or create it from your java package and return a reference.
composite.data.code <- rJava::.jcall(
  j2r
  , "I"
  , "initialize"
  , rJava::.jcast(convertToJava(iris))
  , jdx::jdxConstants()$ARRAY_ORDER[['column-major']] #///always required...or is it? ///make this faster by caching
)
data.code <- processCompositeDataCode(j2r, composite.data.code)
convertToRlowLevel(j2r, data.code)

}

\keyword{ interface }

