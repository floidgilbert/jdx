.onLoad <- function(libname, pkgname) {
  rJava::.jpackage(pkgname, lib.loc = libname)
  assign("jdx.utility", rJava::.jnew("org/fgilbert/jdx/Utility"), inherits = TRUE)
  assign("jdx.j2r", createJavaToRobject(), inherits = TRUE)
}
