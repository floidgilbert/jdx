.onLoad <- function(libname, pkgname) {
  rJava::.jpackage(pkgname, lib.loc = libname)
  assign("jdx.utility", rJava::.jnew("org/fgilbert/jdx/Utility"), inherits = TRUE)
  assign("jdx.j2r", createJavaToRobject(), inherits = TRUE)
  assign(
    "array.order.values"
    , list(
      `column-major` = rJava::.jfield("org.fgilbert.jdx.JavaToR$ArrayOrder", sig = NULL, "COLUMN_MAJOR")
      , `row-major` = rJava::.jfield("org.fgilbert.jdx.JavaToR$ArrayOrder", sig = NULL, "ROW_MAJOR")
      , `row-major-java` = rJava::.jfield("org.fgilbert.jdx.JavaToR$ArrayOrder", sig = NULL, "ROW_MAJOR_JAVA")
    )
    , inherits = TRUE
  )
}
