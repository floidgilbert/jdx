.onLoad <- function(libname, pkgname) {
  rJava::.jpackage(pkgname, lib.loc = libname)
  
  # Check Java version. 
  # See https://cran.r-project.org/doc/manuals/r-release/R-exts.html#Writing-portable-packages
  v <- rJava::.jcall("java/lang/System", "S", "getProperty", "java.runtime.version")
  v <- as.numeric(paste0(strsplit(v, "[.]")[[1L]][1:2], collapse = "."))
  if(v < 1.8) stop("Java 8 is required for this package.")
  
  # Creating these objects via rJava is slow, so instantiate them only once and
  # re-use them to improve performance.
  assign("jdx.utility", rJava::.jnew("org/fgilbert/jdx/Utility"), inherits = TRUE)
  assign("jdx.j2r", createJavaToRobject(), inherits = TRUE)
  
  assign(
    "array.order.values"
    , list(
      `column-major` = rJava::.jfield("org.fgilbert.jdx.JavaToR$ArrayOrder", sig = NULL, "COLUMN_MAJOR")
      , `row-major` = rJava::.jfield("org.fgilbert.jdx.JavaToR$ArrayOrder", sig = NULL, "ROW_MAJOR")
      , `column-minor` = rJava::.jfield("org.fgilbert.jdx.JavaToR$ArrayOrder", sig = NULL, "COLUMN_MINOR")
    )
    , inherits = TRUE
  )
}
