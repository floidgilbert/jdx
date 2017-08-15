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
The \CRANpkg{jdx} package was originally developed as part of \CRANpkg{jsr223}, a high-level integration package for Java. The functions listed here are exposed for compatibility with \pkg{jsr223} only; they are not meant to be used by other packages. Instead, use \code{\link{convertToJava}}, \code{\link{convertToR}}, and \code{\link{getJavaClassName}}.
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
An \pkg{rJava} object reference to a \code{org.fgilbert.jdx.JavaToR} object.
}
  \item{data.code}{
A \pkg{jdx} data code value.
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

\keyword{ interface }
