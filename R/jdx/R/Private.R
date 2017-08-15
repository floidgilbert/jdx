#///mention jsr223 in documentation. tell them to use it instead of rJava.///
#///write up release notes...what to do. download latest R build, build both jar files, build R, run tests, etc.
#///consider implementing major and minor indexing for arrays.
#///devtools::install(build_vignettes = TRUE); library(jdx); ?jdx
#///add the array tests in temp to unit testing and add jsonlite to recommends? no. just leave in outer testing folder. but do that after you have added zero-length stuff.
#///still need to handle arrays coming from jdx to R.
# Constants ---------------------------------------------------------------

# Type codes used to determine return value types coming from Java. While
# handling each return type explicitly is cumbersome, but it is required to
# reduce expensive calls (like class inference and casting) via rJava. These
# codes are combined with other codes. See processCompositeDataCode().
TC_NULL <- 0x00L
TC_NUMERIC <- 0x01L
TC_INTEGER <- 0x02L
TC_CHARACTER <- 0x03L
TC_LOGICAL <- 0x04L
TC_RAW <- 0x05L
TC_OTHER <- 0xFEL # Lists, data frames, user-defined, and exceptions.
TC_UNSUPPORTED <- 0xFFL

# Structure codes. See processCompositeDataCode().
SC_SCALAR <- 0x000L
SC_VECTOR <- 0x100L
SC_ND_ARRAY <- 0x200L # n-dimensional array
SC_DATA_FRAME <- 0x300L
SC_LIST <- 0x400L
SC_NAMED_LIST <- 0x500L
SC_USER_DEFINED <- 0xFF00L #///how is this going to work. Or is it? remember, I added it to the Java side also.

# Exception codes. See processCompositeDataCode().
EC_NONE <- 0x00000L
EC_EXCEPTION <- 0x10000L
EC_WARNING_MISSING_LOGICAL_VALUES <- 0xA0000L
EC_WARNING_MISSING_RAW_VALUES <- 0xB0000L

# Logical NA cannot be represented in primitive Java boolean types. Logical NA 
# will be coerced to FALSE to align with rJava. TRUE was chosen for
# compatibility with rscala and Rcpp. When logical NAs are coerced, a warning is
# thrown. Java null values in java.lang.Boolean boxed types are also coerced
# with a warning.
NA_ASSUMPTION_LOGICAL <- FALSE
MSG_WARNING_MISSING_LOGICAL_VALUES <- sprintf("Missing values encountered during logical (boolean) data coercion have been replaced with '%s'.", NA_ASSUMPTION_LOGICAL)

# Used on the Java side when Java null is found in java.lang.Byte arrays. null
# values are changed to 0x00 with a warning.
NA_ASSUMPTION_RAW <- as.raw(0L)
MSG_WARNING_MISSING_RAW_VALUES <- sprintf("Missing values encountered during raw (byte) data coercion have been replaced with '0x%s'.", NA_ASSUMPTION_RAW)


# Global Variables --------------------------------------------------------

# These are Java objects that are initialized during .onLoad. Creating these objects
# via rJava is slow, so instantiate them only and re-use them to improve
# performance.
array.order.values <- NULL;
jdx.utility <- NULL;
jdx.j2r <- NULL;


# Functions ---------------------------------------------------------------

allCombinations <- function(value, unique = TRUE) {
  if (!is.atomic(value) || !is.null(dim(value)))
    stop("Only vectors are supported.")
  if (unique)
    value <- unique(value)
  l <- list()
  for (i in 1:length(value)) {
    l <- c(l, utils::combn(value, i, simplify = FALSE))
  }
  l
}

# coerceCharacterToX - Convert a character vector to numeric, integer, or 
# logical.
# 
# NA values are retained. However, string values "NA" will not be converted to
# NA (that is, "NA" will cause conversion to fail). This latter requirement is
# necessary because the function returns the original vector when conversion
# fails. Consider these examples.
# 
# c("a", "NA") - Can't be converted to a narrower type, so the original vector
# will be returned.
# 
# c("1", "NA") - Could be converted to integer vector c(1, NA). If it did, the 
# previous vector should be converted to c("a", NA) to be consistent. But this 
# is not intuitive; the original vector should be returned upon failure.
# Remember, the function is designed to convert to numeric, integer, or logical,
# not character to character. Furthermore, it makes sense that any NA in the
# character vector should be represented by NA_character_ not "NA".
# 
# Ignoring "NA" and type casting is somewhat complicated in R. I will list 
# relevant problems below. However, the short story is that extra code must be 
# written to detect when "NA" is converted to NA. I could have used a mixture of
# as.*() and scan() functions, but the behavior of scan() might change in the 
# future depending on how R developers respond to my bug report on scan().
# Finally, scan() is probably inappropriate for this function in any case
# because it will parse c("1 2", "3") as c(1, 2, 3).
# 
# Issues/considerations for type casting in R:
# 
# as.numeric() throws a warning when non-numeric values are coerced to NA (good).
# 
# as.integer() truncates "1.2" to 1 instead of throwing an error, so it is not
# helpful for this application.
# 
# strtoi() does not truncate, but converts non-integer values to NA without
# warning. It coerces "" to 0 without warning.
# 
# as.logical() coerces non-logical values to NA without warning.
# 
# scan() throws an error when an inconsistent value is encountered (good).
# However, handling of "NA" and NA are inconsistent for different target types
# when na.strings = NULL. See the following two lines for examples. This bug has
# been reported.
# 
# This line throws an error (good).
# scan(text = c("1 NA"), what = integer(), na.strings = NULL, quiet = TRUE)
# 
# This line does not throw an error (bad). It coerces the "NA" to NA anyway.
# scan(text = c("1 NA"), what = double(), na.strings = NULL, quiet = TRUE)
# 
coerceCharacterToX <- function(value) {
  if (is.null(value))
    return(NULL)
  if (!is.character(value) || !is.null(dim(value)))
    stop("Only character vectors are supported.")
  if(length(value) == 0)
    return(value)
  
  # Find current number of NA values.
  number.of.NA <- length(value[is.na(value)])
  if(length(value) == number.of.NA)
    return(value)
  
  # Integer
  r <- strtoi(value)
  if (length(r[is.na(r)]) == number.of.NA) {
    # strtoi also coerces "" to 0 without warning.
    if (!any(value == "", na.rm = TRUE))
      return(r)
  }
  
  # Numeric
  tryCatch(
    {
      # as.numeric() throws a warning when coercion fails. This is good.
      r <- as.numeric(value)
      # as.numeric() converts "" to NA without warning. Not so good.
      # Note that is.na() returns true for NaN.
      if (length(which(is.na(r))) - length(which(is.nan(r))) == number.of.NA)
        return(r)
    }
    , warning = function(w) {}
  )
  
  # Logical
  r <- as.logical(value)
  if (length(which(is.na(r))) == number.of.NA)
    return(r)
  
  # If all conversions fail, return original vector.
  value
}

# R does not support converting raw vectors to factors, so this function will
# never yield a raw vector. See comments for coerceCharacterToX for information
# about return values and coercion.
coerceFactor <- function(value) {
  if (is.null(value))
    return(NULL)
  levels <- levels(value)
  if (is.null(levels))
    stop("A factor is required.")
  coerceCharacterToX(levels)[value]
}

# I deliberately chose not to make coerceLogicalNaValues* S3 methods -- we gain
# nothing by forcing a method-lookup whereas we always know the context/type. 
# These functions must execute quickly, and they are private, so there is no 
# parameter checking.

# Assumes 'value' is a logical vector or matrix.
# IMPORTANT: This function throws warnings! If any warning handler is in
# place, execution will be interrupted when a warning is
# propagated.
coerceLogicalNaValues <- function(value) {
  which <- which(is.na(value), FALSE, FALSE)
  if (length(which) == 0)
    return(value)
  value[which] <- NA_ASSUMPTION_LOGICAL
  warning(MSG_WARNING_MISSING_LOGICAL_VALUES, call. = FALSE)
  return(value)
}

# Converts jdx data codes to JNI strings. See processCompositeDataCode().
dataCodeToJNI <- function(data.code) {
  
  if (data.code[2] == SC_VECTOR) {
    if (data.code[1] == TC_NUMERIC)
      return("[D")
    if (data.code[1] == TC_INTEGER)
      return("[I")
    if (data.code[1] == TC_CHARACTER)
      return("[Ljava/lang/String;")
    if (data.code[1] == TC_LOGICAL)
      return("[Z")
    if (data.code[1] == TC_RAW)
      return("[B")
  }

  # No longer used. SC_ND_ARRAY replaced SC_MATRIX. SC_ND_ARRAY
  # always transfers data from the JVM as a vector.
  # 
  # if (data.code[2] == SC_MATRIX) {
  #   if (data.code[1] == TC_NUMERIC)
  #     return("[[D")
  #   if (data.code[1] == TC_INTEGER)
  #     return("[[I")
  #   if (data.code[1] == TC_CHARACTER)
  #     return("[[Ljava/lang/String;")
  #   if (data.code[1] == TC_LOGICAL)
  #     return("[[Z")
  #   if (data.code[1] == TC_RAW)
  #     return("[[B")
  # }
  # 
  if (data.code[2] == SC_SCALAR) {
    if (data.code[1] == TC_NUMERIC)
      return("D")
    if (data.code[1] == TC_INTEGER)
      return("I")
    if (data.code[1] == TC_CHARACTER)
      return("Ljava/lang/String;")
    if (data.code[1] == TC_LOGICAL)
      return("Z")
    if (data.code[1] == TC_RAW)
      return("B")
  }
  
  throwUnsupportedDataCodeException(data.code)
}

throwUnsupportedDataCodeException <- function(data.code) {
  stop(sprintf("Unsupported data type (type:0x%X, structure:0x%X).", data.code[1], data.code[2]))
}

throwUnsupportedRtypeException <- function(class) {
  stop(sprintf("Values of class '%s' are not supported.", class[1]))
}

validateNames <- function(names) {
  if (length(names) == 0 || !identical(names, unique(names)))
    stop("Data frames and named lists are required to have unique names for each column or member.")
}

