# Standard Interface ------------------------------------------------------
# Most developers should use the standard interface.

#///record the fact that when scalars.as.objects = FALSE, a scalar byte will be returned as rJava::.jbyte(value)
#///also note that convertToJava and convertToR are not exactly inverses in every case because of the behavior of Java. Sometimes scalars are returned as R objects because that's what rJava wants.
#///you've forgotten to validate the parameters in every case here.
#' @export
convertToJava <- function(value, length.one.vector.as.array = FALSE, scalars.as.objects = FALSE, array.order = "row-major", data.frame.row.major = TRUE, coerce.factors = TRUE) {
  
  # The class AsIs (set via the function I()) can be used to indicate that
  # length one vectors/arrays/factors should be converted to arrays, not
  # scalars. It is ignored for all other structures.
  value.is.as.is <- inherits(value, "AsIs")
  length.one.vector.as.array <- length.one.vector.as.array || value.is.as.is
  
  # IMPORTANT: is.vector() returns TRUE for lists. I override this behavior
  # here.
  value.is.list <- is.list(value)
  value.is.vector <- is.vector(value) && !value.is.list

  # IMPORTANT: is.vector() returns FALSE for vectors of class AsIs. This code
  # overrides this behavior. `is.atomic()` is FALSE for all but vectors, arrays,
  # and factors. `is.null(dim(value))` is TRUE for both vectors *and* factors.
  # Hence, the final check is required: `!is.factor(value)`
  if (!value.is.vector && value.is.as.is)
    value.is.vector <- is.atomic(value) && is.null(dim(value)) && !is.factor(value)

  if (value.is.vector) {
    if (is.logical(value)) {
      value <- coerceLogicalNaValues(value)
    } else if (is.complex(value)) {
      throwUnsupportedRtypeException("complex")
    }
    if (length(value) != 1 || length.one.vector.as.array)
      return(rJava::.jarray(value))
    # At this point, we know to create a scalar.
    if (!scalars.as.objects) {
      # From the rJava::.jbyte documentation: ".jbyte is used when a scalar byte
      # is to be passed to Java." In other words, a raw vector of length
      # one will not be interpreted as a scalar byte value by rJava unless it is 
      # wrapped in a special class. This will be non-intuitive for the user when
      # length.one.vector.as.array = FALSE and scalars.as.objects = FALSE because 
      # the returned value will not be the same as the value passed in, yet it is 
      # not a Java object; it is an R object wrapped in a custom class. By contrast,
      # when length.one.vector.as.array = FALSE and scalars.as.objects = TRUE, the
      # returned value is a reference to a java.lang.Byte object.
      if (is.raw(value))
        return(rJava::.jbyte(value))
      return(value)
    }
    # The Java documentation suggests using the 'valueOf' static method instead
    # of creating new instances for performance reasons. But .jcall() is slower
    # than .jnew() in this case.
    if (is.double(value))
      return(rJava::.jnew("java/lang/Double", value, check = FALSE))
    if (is.integer(value))
      return(rJava::.jnew("java/lang/Integer", value, check = FALSE))
    if (is.character(value)) {
      # rJava treats scalar NA_character_ as null. It assigns the other NA_*
      # types a reserved numeric value.
      if (is.na(value))
        return(rJava::.jnull())
      return(rJava::.jnew("java/lang/String", value, check = FALSE))
    }
    if (is.logical(value))
      return(rJava::.jnew("java/lang/Boolean", value, check = FALSE))
    if (is.raw(value))
      return(rJava::.jnew("java/lang/Byte", rJava::.jbyte(value), check = FALSE))
    throwUnsupportedRtypeException(class(value))
  }

  if (is.array(value)) {
    if (length(dim(value)) == 1)
      return(convertToJava(as.vector(value), length.one.vector.as.array = length.one.vector.as.array, scalars.as.objects = scalars.as.objects))
    if (array.order == "row-major")
      return(
        rJava::.jcall(
          jdx.utility
          , "Ljava/lang/Object;"
          , "createNdimensionalArrayRowMajor"
          , rJava::.jarray(value, dispatch = FALSE)
          , dim(value)
          , check = TRUE
        )
      )
    if (array.order == "row-major-java")
      return(
        rJava::.jcall(
          jdx.utility
          , "Ljava/lang/Object;"
          , "createNdimensionalArrayRowMajorJava"
          , rJava::.jarray(value, dispatch = FALSE)
          , dim(value)
          , check = TRUE
        )
      )
    return(
      rJava::.jcall(
        jdx.utility
        , "Ljava/lang/Object;"
        , "createNdimensionalArrayColumnMajor"
        , rJava::.jarray(value, dispatch = FALSE)
        , dim(value)
        , check = TRUE
      )
    )
  }
  
  if (is.factor(value)) {
    if (coerce.factors)
      return(convertToJava(coerceFactor(value), length.one.vector.as.array = length.one.vector.as.array, scalars.as.objects = scalars.as.objects))
    return(convertToJava(as.character(value), length.one.vector.as.array = length.one.vector.as.array, scalars.as.objects = scalars.as.objects))
  }
  
  #///remove and replace with nd-arrays. make sure the same behavior is replicated. then remove this code.
  # if (is.matrix(value)) {
  #   if (is.logical(value)) {
  #     value <- coerceLogicalNaValues(value)
  #   } else if (is.complex(value)) {
  #     throwUnsupportedRtypeException("complex")
  #   }
  # 
  #   #///document somewhere.
  #   # By default, rJava converts matrices as row-major. The default behavior for
  #   # zero-length matrices is as follows, assuming the variable names is
  #   # 'value'.
  #   # 
  #   #     matrix(0, 0, 0) becomes double[][] value = {};
  #   #     matrix(0, 0, 1) becomes double[][] value = {};
  #   #     matrix(0, 1, 0) becomes double[][] value = {{}};
  #   #     
  #   # jdx supports column-major matrices. It mimics the zero-length
  #   # row-major behavior as follows.
  #   # 
  #   #     matrix(0, 0, 0) becomes double[][] value = {};
  #   #     matrix(0, 0, 1) becomes double[][] value = {{}};
  #   #     matrix(0, 1, 0) becomes double[][] value = {};
  #   
  #   if (row.major)
  #     return(.jarrayTemp(value, dispatch = TRUE))
  #   
  #   # row.major = FALSE, i.e. the matrix is column-major
  #   # 
  #   # This block handles the zero-length cases as noted above in the
  #   # column-major context.
  #   if (0 %in% dim(value))
  #     return(.jarrayTemp(t(value), dispatch = TRUE))
  #   
  #   contents.class <- {
  #     if (is.double(value))
  #       "[D"
  #     else if (is.character(value))
  #       "[Ljava/lang/String;"
  #     else if (is.integer(value))
  #       "[I"
  #     else if (is.logical(value))
  #       "[Z"
  #     else if (is.raw(value))
  #       "[B"
  #     else
  #       throwUnsupportedRtypeException(class(value))
  #   }
  #   
  #   return(.jarrayTemp(apply(value, 2, rJava::.jarray), contents.class = contents.class))
  # }

  if (is.null(value))
    return(rJava::.jnull())
  
  if (is.data.frame(value)) {
    names <- names(value)
    if (is.null(names)) # It is possible to set names(data.frame) to NULL
      names <- character()
    if (ncol(value)) {
      validateNames(names)
    }
    # Notice that length.one.vector.as.array = TRUE here. Hence, the setting for scalars.as.objects is irrelevant.
    if (data.frame.row.major)
      return(
        rJava::.jcall(
          jdx.utility
          , "Ljava/util/List;"
          , "createListOfRecords"
          , rJava::.jarray(names)
          , rJava::.jarray(lapply(value, convertToJava, length.one.vector.as.array = TRUE, coerce.factors = coerce.factors))
          , check = TRUE
        )
      )
    return(
      rJava::.jcall(
        jdx.utility
        , "Ljava/util/Map;"
        , "createMap"
        , rJava::.jarray(names)
        , rJava::.jarray(lapply(value, convertToJava, length.one.vector.as.array = TRUE, coerce.factors = coerce.factors))
        , check = FALSE
      )
    )
  }

  # Always place after test for data frame because is.list() will return TRUE for data frames.
  if (value.is.list || is.environment(value)) {
    # Catch POSIXlt/POSIXt. They can be detected as lists.
    if (inherits(value, "POSIXlt"))
      throwUnsupportedRtypeException("POSIXlt")
    names <- names(value)
    if (length(value)) {
      if (!is.null(names)) # names will be NULL for unnamed lists.
        validateNames(names)
    }
    if (is.null(names))
      return(
        rJava::.jcall(
          jdx.utility
          , "Ljava/util/List;"
          , "createList"
          , rJava::.jarray(lapply(value, convertToJava, length.one.vector.as.array = length.one.vector.as.array, scalars.as.objects = TRUE, array.order = array.order, data.frame.row.major = data.frame.row.major, coerce.factors = coerce.factors))
          , check = FALSE
        )
      )
    return(
      rJava::.jcall(
        jdx.utility
        , "Ljava/util/Map;"
        , "createMap"
        , rJava::.jarray(names(value))
        , rJava::.jarray(lapply(value, convertToJava, length.one.vector.as.array = length.one.vector.as.array, scalars.as.objects = TRUE, array.order = array.order, data.frame.row.major = data.frame.row.major, coerce.factors = coerce.factors))
        , check = FALSE
      )
    )
  }

  throwUnsupportedRtypeException(class(value))
}

#///explain why we use jdx.j2r
#///this is not thread-safe, correct? if you want something thread-safe, probably use low-level interface.
#' @export
convertToR <- function(value, strings.as.factors = NULL, array.order = "row-major") {
  composite.data.code <- rJava::.jcall(jdx.j2r, "I", "initialize", rJava::.jcast(value, new.class = "java/lang/Object", check = FALSE, convert.array = FALSE), array.order.values[[array.order]])
  data.code <- processCompositeDataCode(jdx.j2r, composite.data.code)
  convertToRlowLevel(jdx.j2r, data.code, strings.as.factors)
}

#' @export
getJavaClassName <- function(value) {
  rJava::.jcall(rJava::.jcall(o, "Ljava/lang/Class;", "getClass"), "S", "getName")
}

# ConvertToR Low-level Interface ------------------------------------------

# These functions are used by the high-level interface. They can also be used in
# Java integrations (such as the jsr223 project) to avoid expensive rJava calls 
# during conversion that create new objects or obtain references to objects
# (non-primitives).

#' @export
arrayOrderToString <- function(value) {
  if (rJava::.jequals(value, array.order.values$`row-major`))
    return("row-major")
  if (rJava::.jequals(value, array.order.values$`column-major`))
    return("column-major")
  if (rJava::.jequals(value, array.order.values$`row-major-java`))
    return("row-major-java")
  NULL
}

# IMPORTANT: Any logic added to convertToRlowLevel must usually be repeated in
# the nested function createList.
#' @export
convertToRlowLevel <- function(j2r, data.code = NULL, strings.as.factors = NULL) {

  createDataFrame <- function(x) {

    #///make sure that wherever we have rJava::... that we specify the expected type. This is much faster.
    evalArray <- function(i) {
      return(rJava::.jevalArray(arrays[[i]], rawJNIRefSignature = dataCodeToJNI(processCompositeDataCode(j2r, types[i]))))
    }

    types <- rJava::.jevalArray(x[[1]], rawJNIRefSignature = "[I")
    if (length(types) == 0)
      return(data.frame())
    arrays <- rJava::.jevalArray(x[[2]], rawJNIRefSignature = "[Ljava/lang/Object;")
    df <- data.frame(
      lapply(1:(length(types)), evalArray)
      , stringsAsFactors = ifelse(is.null(strings.as.factors), default.stringsAsFactors(), strings.as.factors)
      , check.names = FALSE
      , fix.empty.names = FALSE
    )
    names(df) <- rJava::.jevalArray(x[[3]], rawJNIRefSignature = "[Ljava/lang/String;")
    return(df)
  }

  createList <- function(x, data.code) {

    evalObject <- function(i) {
      
      data.code <- processCompositeDataCode(j2r, types[i])

      if (data.code[1] == TC_NULL)
        return(NULL)
      
      if (data.code[2] == SC_SCALAR) {
        if (data.code[1] == TC_RAW)
          return(as.raw(bitwAnd(rJava::.jsimplify(objects[[i]]), 0xff))) #///testbyte
        return(rJava::.jsimplify(objects[[i]]))
      }

      if (data.code[2] == SC_VECTOR)
        return(rJava::.jevalArray(objects[[i]], rawJNIRefSignature = dataCodeToJNI(data.code)))

      # Replaced by SC_ND_ARRAY code, which is more generalized and faster. That
      # isn't to say that the Java code has been optimized for performance. It could
      # be made to be even faster.
      # 
      # if (data.code[2] == SC_MATRIX) {
      #   r <- rJava::.jevalArray(objects[[i]], rawJNIRefSignature = dataCodeToJNI(data.code), simplify = TRUE)
      #   if (row.major)
      #     return(r)
      #   return(t(r))
      # }

      #///make sure rowmajor settings work correctly in this scenario
      if (data.code[2] == SC_ND_ARRAY)
        return(createNdimensionalArray(rJava::.jevalArray(objects[[i]])))
      
      if (data.code[2] == SC_DATA_FRAME)
        return(createDataFrame(rJava::.jevalArray(objects[[i]])))

      if (data.code[2] == SC_LIST || data.code[2] == SC_NAMED_LIST)
        return(createList(rJava::.jevalArray(objects[[i]]), data.code))

      throwUnsupportedDataCodeException(data.code)
    }

    types <- rJava::.jevalArray(x[[1]], rawJNIRefSignature = "[I")
    if (length(types) == 0)
      return(list())
    objects <- rJava::.jevalArray(x[[2]], rawJNIRefSignature = "[Ljava/lang/Object;")
    lst <- lapply(1:(length(types)), evalObject)
    if (data.code[2] == SC_NAMED_LIST)
      names(lst) <- rJava::.jevalArray(x[[3]], rawJNIRefSignature = "[Ljava/lang/String;")
    return(lst)
  }
  
  createNdimensionalArray <- function(x) {
    dimensions <- rJava::.jevalArray(x[[1]], rawJNIRefSignature = "[I")
    # Providing `rawJNIRefSignature` is about 1/3 times faster than not.
    if (data.code[1] == TC_NUMERIC)
      return(array(rJava::.jevalArray(x[[2]], "[D"), dimensions))
    if (data.code[1] == TC_INTEGER)
      return(array(rJava::.jevalArray(x[[2]], "[I"), dimensions))
    if (data.code[1] == TC_CHARACTER)
      return(array(rJava::.jevalArray(x[[2]], "[S"), dimensions))
    if (data.code[1] == TC_LOGICAL)
      return(array(rJava::.jevalArray(x[[2]], "[Z"), dimensions))
    if (data.code[1] == TC_RAW) {
      return(array(rJava::.jevalArray(x[[2]], "[B"), dimensions))
    }
    throwUnsupportedDataCodeException(data.code)
  }

  # If a data.code is not provided, retrieve and process it.
  if (is.null(data.code)) {
    composite.data.code <- rJava::.jcall(j2r, "I", "getRdataCompositeCode")
    data.code <- processCompositeDataCode(j2r, composite.data.code)
  }

  if (data.code[1] == TC_NULL)
    return(NULL)

  if (data.code[2] == SC_SCALAR) {
    if (data.code[1] == TC_NUMERIC)
      return(rJava::.jcall(j2r, "D", "getValueDouble", check = FALSE))
    if (data.code[1] == TC_INTEGER)
      return(rJava::.jcall(j2r, "I", "getValueInt", check = FALSE))
    if (data.code[1] == TC_CHARACTER)
      return(rJava::.jcall(j2r, "S", "getValueString", check = FALSE))
    if (data.code[1] == TC_LOGICAL)
      return(rJava::.jcall(j2r, "Z", "getValueBoolean", check = FALSE))
    if (data.code[1] == TC_RAW) {
      # Convert to raw manually. Unfortunately, rJava returns an integer vector 
      # in this scenario. This is understandable because Java bytes range from 
      # -128 to 127 whereas R raw values range from 0 to 255. However, this 
      # behavior is inconsistent because rJava converts byte arrays to raw 
      # values without hesitation and bitwise. So Java -1 maps to R 0xff. So,
      # that leaves me in a quandry. I have decided to make the behavior between
      # the scalars and the arrays consistent. ///document this fact.
      return(as.raw(bitwAnd(rJava::.jcall(j2r, "B", "getValueByte", check = FALSE), 0xff))) #///testbyte
    }
    throwUnsupportedDataCodeException(data.code)
  }

  if (data.code[2] == SC_VECTOR) {
    if (data.code[1] == TC_NUMERIC)
      return(rJava::.jcall(j2r, "[D", "getValueDoubleArray1d", check = FALSE))
    if (data.code[1] == TC_INTEGER)
      return(rJava::.jcall(j2r, "[I", "getValueIntArray1d", check = FALSE))
    if (data.code[1] == TC_CHARACTER)
      return(rJava::.jcall(j2r, "[Ljava/lang/String;", "getValueStringArray1d", check = FALSE))
    if (data.code[1] == TC_LOGICAL)
      return(rJava::.jcall(j2r, "[Z", "getValueBooleanArray1d", check = FALSE))
    if (data.code[1] == TC_RAW)
      return(rJava::.jcall(j2r, "[B", "getValueByteArray1d", check = FALSE)) #///testbyte
    throwUnsupportedDataCodeException(data.code)
  }

  # Replaced by SC_ND_ARRAY code, which is both generalized and faster. That
  # isn't to say that the Java code has been optimized for performance. It could
  # be made to be even faster.
  # 
  # if (data.code[2] == SC_MATRIX) {
  #   r <- NULL
  #   if (data.code[1] == TC_NUMERIC) {
  #     r <- rJava::.jcall(j2r, "[[D", "getValueDoubleArray2d", check = FALSE, simplify = TRUE)
  #   } else if (data.code[1] == TC_INTEGER) {
  #     r <- rJava::.jcall(j2r, "[[I", "getValueIntArray2d", check = FALSE, simplify = TRUE)
  #   } else if (data.code[1] == TC_CHARACTER) {
  #     r <- rJava::.jcall(j2r, "[[Ljava/lang/String;", "getValueStringArray2d", check = FALSE, simplify = TRUE)
  #   } else if (data.code[1] == TC_LOGICAL) {
  #     r <- rJava::.jcall(j2r, "[[Z", "getValueBooleanArray2d", check = FALSE, simplify = TRUE)
  #   } else if (data.code[1] == TC_RAW) {
  #     r <- rJava::.jcall(j2r, "[[B", "getValueByteArray2d", check = FALSE, simplify = TRUE) #///testbyte
  #   } else {
  #     throwUnsupportedDataCodeException(data.code)
  #   }
  #   if (row.major)
  #     return(r)
  #   return(t(r))
  # }

  if (data.code[2] == SC_ND_ARRAY)
    return(createNdimensionalArray(rJava::.jcall(j2r, "[Ljava/lang/Object;", "getValueObjectArray1d", check = FALSE)))
  
  if (data.code[2] == SC_DATA_FRAME)
    return(createDataFrame(rJava::.jcall(j2r, "[Ljava/lang/Object;", "getValueObjectArray1d", check = FALSE)))

  if (data.code[2] == SC_LIST || data.code[2] == SC_NAMED_LIST)
    return(createList(rJava::.jcall(j2r, "[Ljava/lang/Object;", "getValueObjectArray1d", check = FALSE), data.code))

  throwUnsupportedDataCodeException(data.code)
}

#///document what's going on. if data.code provided, we assume we don't need to check it or anything
#data.code needs to correspond to j2r. this is a time-saving device.
#' @export
createJavaToRobject <- function() {
  rJava::.jnew("org/fgilbert/jdx/JavaToR")
}

#' @export
jdxConstants <- function() {
  list(
    ARRAY_ORDER = array.order.values
    
    , EC_NONE = EC_NONE
    , EC_EXCEPTION = EC_EXCEPTION
    , EC_WARNING_MISSING_LOGICAL_VALUES = EC_WARNING_MISSING_LOGICAL_VALUES
    , EC_WARNING_MISSING_RAW_VALUES = EC_WARNING_MISSING_RAW_VALUES
    
    , MSG_WARNING_MISSING_LOGICAL_VALUES = MSG_WARNING_MISSING_LOGICAL_VALUES
    , MSG_WARNING_MISSING_RAW_VALUES = MSG_WARNING_MISSING_RAW_VALUES
    
    , NA_ASSUMPTION_LOGICAL = NA_ASSUMPTION_LOGICAL
    , NA_ASSUMPTION_RAW = NA_ASSUMPTION_RAW
    
    , SC_SCALAR = SC_SCALAR
    , SC_VECTOR = SC_VECTOR
    # , SC_MATRIX = SC_MATRIX
    , SC_ND_ARRAY = SC_ND_ARRAY
    , SC_DATA_FRAME = SC_DATA_FRAME
    , SC_LIST = SC_LIST
    , SC_NAMED_LIST = SC_NAMED_LIST
    , SC_USER_DEFINED = SC_USER_DEFINED
    
    , TC_NULL = TC_NULL
    , TC_NUMERIC = TC_NUMERIC
    , TC_INTEGER = TC_INTEGER
    , TC_CHARACTER = TC_CHARACTER
    , TC_LOGICAL = TC_LOGICAL
    , TC_RAW = TC_RAW
    , TC_OTHER = TC_OTHER
    , TC_UNSUPPORTED = TC_UNSUPPORTED
  )
}

# IMPORTANT: This function throws warnings! If a warning handler is in
# place, execution will be interrupted when a warning is propagated.
#' @export
processCompositeDataCode <- function(j2r, composite.data.code, throw.exceptions = TRUE, warn.missing.logical = TRUE, warn.missing.raw = TRUE) {
  result <- c(
    bitwAnd(composite.data.code, 0xFFL)          # Data type code
    , bitwAnd(composite.data.code, 0xFF00L)      # Data structure code
    , bitwAnd(composite.data.code, 0xFF0000L)    # Exception code
    , bitwAnd(composite.data.code, 0x7F000000L)  # User defined code. Note that 0xFF000000 is a numeric (i.e., double) in R.
  )
  if (result[3] == EC_NONE) {
    # Do nothing
  } else if (result[3] == EC_EXCEPTION && throw.exceptions) {
    stop(rJava::.jcall(j2r, "S", "getValueString", check = FALSE))
  } else if (result[3] == EC_WARNING_MISSING_LOGICAL_VALUES && warn.missing.logical) {
    warning(MSG_WARNING_MISSING_LOGICAL_VALUES, call. = FALSE)
  } else if (result[3] == EC_WARNING_MISSING_RAW_VALUES && warn.missing.raw) {
    warning(MSG_WARNING_MISSING_RAW_VALUES, call. = FALSE)
  } else if (result[1] == TC_UNSUPPORTED) {
    # This should never happen. The JavaToR class should raise this error on the Java side.
    # If it doesn't the developer has broken something...
    stop("The Java data type could not be converted to an R object. This exception is unexpected at this location. Please report this error as a bug with relevant code.")
  }
  return(result)
}

