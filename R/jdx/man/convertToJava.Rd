\name{convertToJava}
\alias{convertToJava}

\title{
Convert R Objects to Java Objects
}

\description{
The function \code{\link{convertToJava}} converts R Objects to generic Java Objects for use with the \pkg{rJava} package. This function simplifies and extends data exchange for \pkg{rJava}. The function \code{\link{convertToR}} is the inverse of \code{\link{convertToJava}}. Detailed data conversion rules are included in the vignette ///.
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
An \R vector, matrix, array, table, factor, data frame, or list. Nested lists are supported. Supported data types: numeric, integer, character, logical, and raw.

}
  \item{length.one.vector.as.array}{
A logical vector of length one. See \strong{Scalars and Length-one Vectors} for details.
}
  \item{scalars.as.objects}{
A logical vector of length one. Used in conjunction with \code{length.one.vector.as.array}. See \strong{Scalars and Length-one Vectors} for details.
}
  \item{array.order}{
A character vector of length one specifying the indexing order for matrices and n-dimensional arrays. Valid values are \code{"row-major"}, \code{"column-major"}, \code{"row-major-java"}. See \strong{Array Order} for a description of each type.
}
  \item{data.frame.row.major}{
A logical vector of length one. When \code{TRUE} (the default), a data frame is converted to a list of map objects (specifically, \code{List<Map>}) that represent rows. When \code{FALSE}, a data frame is converted to a map of arrays (\code{Map<String, Object>}) that represent columns. Conversion for row-major order is slower than column-major order. See \strong{Data Frames} for more information.
}
  \item{coerce.factors}{
A logical vector of length one. When \code{TRUE} (the default), an attempt is made to coerce the character values backing factors to integer, numeric, or logical vectors. When \code{FALSE}, the factor is converted to a character vector. This parameter affects standalone factors as well as factors present in data frames and lists. See \strong{Factors} for more information.
}
}

\details{
The \code{convertToJava} function is used to create objects that can be used as method/function parameters in the \pkg{rJava} package. \R vectors, matrices, n-dimensional arrays, tables, factors, data frames, lists, named lists, and nested lists are supported as well as data types numeric, integer, logical, character and raw.

See the ///vignette for a table that details \R to Java conversion rules.

The \code{jdx} interface provides a single function, \code{convertToJava}, to handle all \R objects instead of providing separate functions for vectors, arrays, data frames, etc. This design facilitates simplified, dynamically-typed programming. Consider, for example, an overloaded Java method \code{setValue}, that has different method signatures for a wide variety of data types and structures. If we use \code{rJava::.jcall(object, "V", "setValue", convertToJava(value))}, the correct Java method is automatically selected based on the return value of \code{convertToJava}.///add to vignette
}

\value{
A Java object reference or an \R vector. See the details below and the conversion tables in the vignette for more information. ///remove conversion tables references if we don't use conversion tables. ///Probably write the vignette.
}

\section{Scalars and Length-one Vectors}{
The parameters \code{length.one.vector.as.array} and \code{scalars.as.objects} control how \R vectors of length one are converted to Java objects. If \code{length.one.vector.as.array} is \code{TRUE}, length-one vectors are converted to Java arrays. This is equivalent to passing an \R vector to \code{rJava::.jarray}. The "As Is" \R function, \code{base::I()}, can also be used to indicate that a vector should be converted to a Java array. For example, \code{convertToJava(I(1))} produces a Java array even though the default value for \code{length.one.vector.as.array} is \code{FALSE}. If other structures are wrapped in ///why is this helpful? for single function parameters.

If \code{length.one.vector.as.array} is \code{FALSE} (the default), length-one vectors are assumed to be scalar values. If \code{scalars.as.objects} is also \code{FALSE} (the default), \code{convertToJava} will return the original \R vector instead of creating a Java object for numeric, integer, and logical length-one vectors. This is because \pkg{rJava} requires these values for primitive Java double, int, and boolean method parameters. In the case of 
}

\section{Raw Values}{
The \pkg{jdx} package differs from \pkg{rJava} when handling \R raw values. \pkg{rJava} converts raw vectors to Java byte arrays, and it provides a function \code{.jbyte} to mark length-one vectors.  specify Java byte scalars. However, \pkg{rJava} returns an integer when 
\R raw values are converted to Java bytes. Note that \R raw values range from 0-255 while Java bytes range from -128 to 127. Values are copied bitwise.
null behavior.
///put this in 
}


\section{Code Evaluation}{
The notation \code{engine \%~\% snippet} evaluates the expression \var{snippet} and returns the value of the last expression in the code snippet. The method \code{evalScriptFile} is a wrapper for \code{\%~\%} to facilitate loading script from a file.

\subsection{Errors}{
Syntax errors and run-time errors that occur in the script engine are propagated to the \R session.
}

\subsection{String Interpolation}{
When the property \code{engine$interpolate} is set to \code{TRUE} (the default), string interpolation of code snippets is enabled. That is, \R code bracketed in '@\{\}' is evaluated and replaced before the snippet is evaluated by the script engine. For example, the JavaScript code snippet

\preformatted{
  "customFunction(@{qnorm(.005)})"
}

will be evaluated as

\preformatted{
  "customFunction(-2.575829)"
}

by the script engine. This is a simple way to update code with dynamic values and call functions with parameters.
}

\subsection{Console Output}{
Be default, any text written to standard output by the script engine is captured and printed to the \R console after code execution completes. When the property \code{engine$quiet} is set to \code{TRUE}, output will not be printed in the console.

\pkg{jsr223} creates the script engine environment in an instance of the Java Virtual Machine (JVM). The JVM process is created using the base \R function \code{system2}. Though \code{system2} provides a feature to capture standard output from a child process in real time, it does not work on all platforms. Consequently, \pkg{jsr223} manages standard output using its own platform-independent handler. This handler simply buffers all text written to the standard output stream until a call to evaluate a code snippet returns. All text in the buffer is then printed in the \R console. Under most circumstances, this approach is sufficient.

In the unusual scenario that a script engine produces standard output outside of synchronous code execution (as in multi-threaded execution), the text will be buffered until \pkg{jsr223} evaluates another code snippet. The buffered text is then printed along with any text produced by the most recent code snippet. This may result in unexpected output. \code{\link{clearEngineOutput}} or \code{\link{getEngineOutput}} may be called before evaluating a code snippet to clear the standard output buffer.

As of this writing, the Scala JSR-223 implementation does not support the ability to redirect standard output. This has two primary consequences. First, quiet mode (\code{engine$quiet <- TRUE}) has no effect for a Scala instance. Second, a Scala script engine's standard output will not be printed at all when run on the Windows platform due to the limitations in the base \R function \code{system2} noted above.
}

\subsection{Timeouts and Interrupting Script Execution}{
\pkg{jsr223} executes code snippets synchronously. That is, it waits until code execution is complete before returning control to the \R session. Unfortunately, the Java Scripting API does not provide a facility to interrupt long-running or runaway code. The only way to stop code execution is to interrupt \R (i.e., return control to the command interpreter).

If the connection timeout set in \code{\link{startEngine}} expires before execution completes, control will return to the \R session and the script engine will be closed automatically.

There are a few very unusual scenarios where the script engine process does not always terminate properly. One such case is when an \R variable referencing a script engine instance is re-used before calling \code{close(engine)}. To terminate the related JVM process manually, close all other script engine instances then locate and terminate the \file{jjs} process using your operating system's process manager.
}
}

\section{Callbacks}{
When \code{\link{startEngine}} creates a new script engine instance, it automatically creates a global object named \code{R} in the script engine environment. The \code{R} object provides methods used for callbacks into the current \R session:

\preformatted{
eval(snippet)
get(identifier)
put(identifier, value)
}

The \code{eval} method executes \R code contained in the string \var{snippet} and returns the value of the last statement if applicable. Recursive calls between \R and the scripting environment are supported.

The \code{get} method returns the value of the \R variable identified in the string \var{identifier}.

The \code{put} method creates or updates the \R variable identified in the string \var{identifier} with \var{value}.

Callbacks are supported only in code that is evaluated synchronously by \code{\link{\%~\%}} or \code{\link{evalScriptFile}}. The behavior is undefined if a script executes callback methods asynchronously as in a multi-threading.

The \code{R} object is not thread-safe.

Callback code samples are provided in the \dQuote{Examples} section.
}

\section{Data Marshaling}{
\dfn{Marshaling} is the process of encoding/decoding objects and transferring them between processes or nodes. \pkg{jsr223} uses JSON to marshal data in a predictable and intuitive way. In the \R session, \pkg{jsr223} uses the \pkg{jsonlite} package developed by Jeroen Ooms for JSON handling. In the JVM that hosts the script engine environment, JSON is managed by \href{https://github.com/FasterXML/jackson-jr}{FasterXML's jackson-jr}. This section contains recommendations, technical details, and limitations relating to marshaling data using JSON.

\subsection{General Recommendations}{
If you intend to share data structures like matrices and data frames between \R and the script engine, it is recommended that you load data into \R first then pass it into the script engine environment. This ensures that all data are represented with compatible objects that can be efficiently marshaled back to \R. See the subsections \dQuote{Passing Data from \R to the Script Engine} and \dQuote{Passing Data from the Script Engine to \R} for more information regarding data conversion.

Marshaling large amounts of data contained in native JavaScript arrays, Python lists, or Python tuples to \R can be slow or memory intensive because these specific structures require an additional copy operation before they are serialized to JSON. To avoid this overhead, simply use another simple data structure, or use Java arrays/lists. See \dQuote{Using Java Classes} for examples.

Transferring data between \R and the script engine environment using \pkg{jsr223} set/get operators is very convenient and intuitive. It preserves data types and object structures without any effort on behalf of the coder. However, when transferring very large data objects this method may be slow and memory intensive. There are two primary reasons for this. First, JSON is a text format designed for maximum interoperability at the expense of efficiency. Second, two complete copies of the data are held in memory: one copy is in the \R session, another copy is in the script engine. For these reasons, it may be more efficient to use disk files when transferring large data objects between environments. For example: 1) write the file to disk using a standard format; 2) unload the data object from memory; 3) pass the file name to the receiving environment using a simple \pkg{jsr223} variable assignment; and 4) read the data from the file in the receiving environment.
}

\subsection{Passing Data from R to the Script Engine}{
\pkg{jsr223} uses \pkg{jsonlite} to serialize \R objects to JSON for marshaling. This section details relevant behaviors of \pkg{jsonlite}. For further information, see the \pkg{jsonlite} documentation and the associated paper \href{https://cran.r-project.org/web/packages/jsonlite/vignettes/json-mapping.pdf}{The jsonlite Package: A Practical and Consistent Mapping Between JSON Data and R Objects}.

Classes vector, list, matrix and data frame are supported. Data types numeric, integer, and character are also supported. (See \dQuote{Floating Point Precision} for details regarding numeric precision.) Other data types such as dates, times, factors, and complex numbers are coerced to strings.

\R vectors are converted to arrays in the script engine environment. Matrices are converted to arrays of equal-length sub-arrays. Row and column names are not preserved in either case. \pkg{jsonlite} describes matrices in row major order. For example, \code{matrix(1:10, nrow = 2)} will be converted to an array containing two sub-arrays that are each five elements long. The following code demonstrates creating a variable named \var{m} that represents a matrix with two rows and five columns. The first element of the first array is printed using JavaScript syntax.

\preformatted{
  engine$m <- matrix(1:10, nrow = 2)
  engine \%~\% "print(m[0][0]);"
}

Unnamed \R lists are converted to arrays containing arrays and/or objects. Named lists are converted to objects with properties representing the list members. When using named lists, be certain that all members are named. Unnamed members will be assigned a name corresponding to the member's index. This numeric name is not considered a valid identifier in most programming languages.

Data frames are converted to arrays of row objects. Each row object contains properties corresponding to the data frame's columns.

Empty objects like \code{integer(0)} are encoded as empty arrays (\code{[]}).

\pkg{jsonlite} encodes missing values (\code{NA}) present in logical and character vectors using the JSON literal \code{null}. For numeric vectors, \code{NA}, \code{NaN}, \code{Inf}, and \code{-Inf} are encoded as strings. This enables \pkg{jsonlite} to correctly interpret missing and special values consistently for each data type.

\pkg{jsonlite} encodes the standalone \R literal \code{NULL} to an empty object (\code{\{\}}). \pkg{jsr223} overrides this behavior by mapping the \R literal \code{NULL} to the JSON literal \code{null}. This approach is more intuitive for most purposes.

All character vectors are encoded as UTF-8. JSON supports escaping of unicode characters using a backslash followed by four hexadecimal characters. For example, \code{\\u0020} denotes the common space character. The definition of UTF-8 does not use characters between \code{\\uD800} and \code{\\uDFFF} (these are reserved for UTF-16 encoding). As of this writing, \pkg{jsonlite} does not correctly escape control characters \code{\\u0000} through \code{\\u001F}.
}

\subsection{Passing Data from the Script Engine to R}{
As noted in "General Recommendations," to achieve maximum interoperability and data safety, load data in the \R session first and then pass it to the script engine. This ensures that data are encoded in compatible formats. That said, there are certainly cases when it is desirable to load or generate new data in the script engine before passing it to \R. For the most part, basic data types such as strings, integers, and floating point decimals will be interoperable with \R. If you encounter an incompatible type, simply convert the data to a simpler type or use a Java class. Basic data structures such as arrays and simple Java objects are supported.

When decoding a JSON string, \pkg{jsonlite} sets the data type of the target data structure to \code{integer} if only integer values are present. For example, setting \code{engine$a <- c(5.0, 6.0)} will result in \code{class(engine$a)} returning \code{"integer"} instead of \code{"numeric"}.
}

\subsection{Floating Point Precision}{
Because JSON is a text format, numeric data are encoded as literal decimal values when marshaled between \R and the script engine environment. The script engine environments tend to preserve more significant digits than does \R when converting numeric values to strings. The precision in \R is between 14-15 significant digits which is sufficient for practically any application. However, the precision discrepency between environments does present a problem when testing values for exact equality. The code below demonstrates the problem as well as two common workarounds.

\preformatted{
  engine$p <- pi
  engine$p == pi  # Returns FALSE due to differences in precision.

  # First workaround: Use all.equal()
  all.equal(engine$p, pi, tolerance = 1e-10)  # Returns TRUE

  # Second workaround: Use round()
  p <- round(pi, 10)
  engine$p <- p
  engine$p == p  # Returns TRUE
}
}
}

\section{Using Java Classes}{
One of the most powerful features of the JSR-223 specification is the ability to access Java libraries using the scripting language of your choice. This section provides simple Java examples for each language.

To use Java libraries not included in the standard Java runtime, include the related JAR files in the \var{class.path} parameter of the \code{\link{startEngine}} function used to create the script engine instance.

\subsection{Groovy}{
Groovy is very similar to the Java language, so creating and using Java classes is very straightforward. The main caveat is that variables created in Groovy code snippets are not global by default. That is, they will not persist in the script engine environment unless they are specifically declared as global variables. Global variables are defined by omitting the type definition and the \code{def} keyword. For example \code{myVar = 42} will create a global variable. A simple code example is provided below.

\preformatted{
  # Create an object from a Java class. The variable 'r' is
  # declared without a type or the 'def' keyword. This marks 'r'
  # as global so it will persist between code snippets.
  engine \%~\% "r = new java.util.Random(10)"
  engine \%~\% "r.nextDouble()" # 0.7304303
}
}

\subsection{JavaScript}{
In addition to the benefit of using Java classes in general, it important to know how to use Java arrays or lists in place of native JavaScript arrays. As noted previously, marshaling large data from native JavaScript arrays to \R is inefficient because it requires an extra copy of the data. The code below includes examples to create and use Java arrays and ArrayList objects.

For more details on using Java classes in JavaScript, see the \href{https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/api.html}{The Nashorn Java API}.

\preformatted{
  # Create an object from a Java class using Java.type (the
  # recommended method).
  engine \%~\% "
    var RandomClass = Java.type('java.util.Random');
    var r = new RandomClass(10);
  "
  engine \%~\% "r.nextDouble();" # 0.7304303

  # Create an object from a Java class directly. This method
  # is easier to read, but it does incur some additional
  # overhead.
  engine \%~\% "var r = new java.util.Random(10);"
  engine \%~\% "r.nextDouble();" # 0.7304303

  # Create a Java integer array of size 5 and initialize each
  # member with a loop.
  engine \%~\% "
    var IntegerArrayType = Java.type('int[]');
    var myArray = new IntegerArrayType(5);
    for (i = 0; i < myArray.length; i++) {
      myArray[i] = i;
    }
  "
  engine$myArray    # 0 1 2 3 4
  engine$myArray[1] # 0

  # Create a Java ArrayList and populate using a loop.
  engine \%~\% "
    var ArrayListClass = Java.type('java.util.ArrayList');
    var myArrayList = new ArrayListClass();
    for (i = 0; i < 5; i++) {
      myArrayList.add(i);
    }
  "
  engine$myArrayList    # 0 1 2 3 4
  engine$myArrayList[1] # 0
}
}

\subsection{JRuby (Ruby)}{
Using Java classes in Ruby is straightforward. For more details about Java in Ruby, see \href{https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby}{Calling Java from JRuby}.

To reuse a variable between Ruby code snippets, declare the variable as global using the dollar sign notation as shown below.

\preformatted{
  # Create an object from a Java class.
  engine \%~\% "
    require 'java'
    $r = java.util.Random.new(10)
  "
  engine \%~\% "$r.nextDouble" # 0.7304303
}
}

\subsection{Jython (Python)}{
As mentioned earlier in this document, marshaling large data from Python lists and tuples to \R is inefficient because it requires an extra copy of the data. The code below includes examples to create and use Java arrays and ArrayList objects in Python in place of Python lists and tuples.

For more details on using Java classes in Python, see the \href{https://wiki.python.org/jython/NewUsersGuide#interaction-with-java-packages}{Jython Wiki}.

\preformatted{
  # Create an object from a Java class.
  engine \%~\% "
  from java.util import Random
  r = Random(10)
  "
  engine \%~\% "r.nextDouble()" # 0.7304303

  # Create a Java integer array of size 5 and initialize each
  # member using a loop.
  engine \%~\% "
  from jarray import zeros
  myArray = zeros(5, 'i')
  for i in range(myArray.__len__()):
    myArray[i] = i
  "
  engine$myArray    # 0 1 2 3 4
  engine$myArray[1] # 0

  # Create a Java ArrayList and populate using a loop.
  engine \%~\% "
  from java.util import ArrayList
  myArrayList = ArrayList()
  for i in range(5):
    myArrayList.add(i)
  "
  engine$myArrayList    # 0 1 2 3 4
  engine$myArrayList[1] # 0
}
}

\subsection{Scala}{
Scala is designed to be interoperable with Java, so creating and using Java classes is simple as shown in the code example below.

\preformatted{
  # Create an object from a Java class.
  engine \%~\% "val r = new java.util.Random(10)"
  engine \%~\% "r.nextDouble()" # 0.7304303
}
}
}

\section{Script Engine Idiosyncrasies}{
In general, all JSR-223 compatible script engines work very well in conjunction with \R. However, there are a few discrepencies worth noting.

Some scripting languages have one or more data types or structures that cannot be marshaled to \R as-is. To avoid compatibility issues, load the data in the \R session and pass it to the script engine environment for further manipulation. This will ensure that all data are represented with compatible objects that can be marshaled back to \R. Another solution is to use base Java types in script as described in the section \dQuote{Using Java Classes}.

Language-specific issues are listed below.

\subsection{Groovy}{
If the last statement in a code snippet sets a value, that value will be returned. For example,
\preformatted{
  engine \%~\% "a = 1"
} will return the value \code{1}.

Global variables are defined by omitting the type definition and the \code{def} keyword. For example \code{myVar = 42} will create a global variable. The \code{@Field} notation cannot be used. To specify a data type for a global variable, use a constructor (\code{myVar = new Integer(42)}) or a type suffix (\code{myVar = 42L}).
}

\subsection{JavaScript}{
Passing native JavaScript arrays to \R can be slow and memory intensive because the operation requires an additional temporary copy of the data. If native JavaScript arrays are not required, use a Java array instead as described in the section \dQuote{Using Java Classes}. Or, just load the data in \R and pass it to the script engine where a Java object will be created automatically.

If the last statement in a code snippet sets a value, that value will be returned. For example,
\preformatted{
  engine \%~\% "a = 1;"
} will return the value \code{1}. However, if \code{var} is used to declare a variable in the last statement (\code{var a = 1;}), a value is not returned.

A script engine JAR file is not required in the \code{\link{startEngine}} function's \var{class.path} parameter to create an instance of a JavaScript engine. The JavaScript engine is included in the Java Runtime Environment (JRE).
}

\subsection{JRuby (Ruby)}{
If the last statement in a code snippet sets a value, that value will be returned. For example,
\preformatted{
  engine \%~\% "$a = 1"
} will return the value \code{1}.

Setting a variable using \code{engine$myValue <- 10.1} creates a global variable in the script engine environment. To refer to the global variable in a Ruby code snippet, prefix the variable name with a dollar sign: \code{$myValue * 2}.

As of this writing it has been observed infrequently that when the JRuby encounters an error, the engine may continue to throw errors incorrectly in response to subsequent requests. If this happens, simply restart the engine.
}

\subsection{Jython (Python)}{
Passing large Python lists and tuples to \R can be slow and memory intensive because the operation requires an additional temporary copy of the data. Use a Java array or list instead as described in the section \dQuote{Using Java Classes}. Or, just load the data in \R and pass it to the script engine where a Java object will be created automatically.
}

\subsection{Scala}{
If the last statement in a code snippet sets a value, that value will be returned. For example,
\preformatted{
  engine \%~\% "val a = 1"
} will return the value \code{1}.

A full installation of Scala is required. To create an instance of a Scala engine, do not specify the script engine JAR files in the \code{\link{startEngine}} function's \var{class.path} parameter. For example, \code{engine <- startEngine("scala")} is sufficient.
}
}

\author{
David B. Dahl \email{dahl@stat.byu.edu}, Floid R. Gilbert \email{floid.r.gilbert@gmail.com}
}

\seealso{
  \code{\link{startEngine}},
  \code{\link{clearEngineOutput}},
  \code{\link{getEngineOutput}},
  \code{\link{getJavaType}}
}

\examples{
library("jsr223")

# Create an instance of a JavaScript engine. Note that the
# script engine's JAR file is not required for the class.path
# parameter because JavaScript is included with JRE 8.
engine <- startEngine(engine.name = "javascript")

# Evaluate arbitrary code. Multiline code is allowed.
engine \%~\% "var a = Math.PI;"

# Retrieve the value of a global JavaScript variable.
cat("The value of 'a' is ", engine$a, ".", sep = "")

# Set the value of a global variable. If the variable does
# not exist in the engine environment, it will be created.
engine$a <- 10
cat("The value of 'a' is now ", engine$a, ".", sep = "")

# Example of string interpolation. R code bracketed in @{}
# will be evaluated and replaced before the snippet is evaluated
# by the script engine. This is a convenient way to pass simple
# parameters to functions. The result will be the value of the
# last expression in the snippet.
result <- engine \%~\% "Math.max(@{rnorm(10)})"

# Use callbacks to set values, get values, and execute R code
# in the current R session via the global R object.
engine \%~\% "R.put('myValue', 10);"
engine \%~\% "var myValue = R.get('myValue');"
engine \%~\% "var randomNormal = R.eval('rnorm(15)');"

# Enable property to convert length one vectors to arrays
# instead of scalar values.
engine$length.one.vector.as.array <- TRUE
engine$c <- 1
engine \%~\% "c[0]" # Returns 1

# Suppress console output.
engine$quiet <- TRUE
engine \%~\% "print('Hello');"

# Always close the engine.
close(engine)

\dontrun{

# Create a JRuby engine by specifying the class.path for the
# engine JAR together with the engine name. The JAR file path
# will be different on your system.
engine <- startEngine(
  engine.name = "jruby"
  , class.path = "../engines/jruby-complete-9.0.0.0.jar"
)

# Assign a value to a variable. This will create a global
# variable in the Ruby environment.
engine$c <- pi

# Reference the previous value in code snippet. Note that Ruby
# requires a "$" designator for global variables.
engine \%~\% "3 * $c"

# Execute script contained in a file.
evalScriptFile(engine, "./my_script.rb")

# Always close the engine.
close(engine)
}
}

\keyword{ interface }
