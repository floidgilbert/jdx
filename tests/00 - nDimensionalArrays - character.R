# Use jsonlite to ensure multi-dimensional arrays are converted to Java correctly.

# Initialize --------------------------------------------------------------

library("jdx")
library("jsonlite")
library("testthat")

# Row-major ---------------------------------------------------------------

# One-dimensional
for (i in 0:5) {
  a <- array(as.character(1:i), c(i))
  o <- convertToJava(a, length.one.vector.as.array = TRUE)
  o <- rJava::.jcast(o, "[Ljava/lang/Object;")
  s1 <- rJava::.jcall("java/util/Arrays", "S", "toString", o)
  s1 <- gsub(" ", "", s1)
  s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
  s2 <- as.character(toJSON(a))
  # cat(s1, "\n")
  # cat(s2, "\n\n")
  expect_identical(s1, s2)
  expect_identical(convertToR(o), as.vector(a))
  if (i != 0) {
    p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
    expect_identical(convertToR(p), as.vector(a))
  }
}

# Two-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    a <- array(as.character(1:(i * j)), c(i, j))
    o <- convertToJava(a)
    o <- rJava::.jcast(o, "[Ljava/lang/Object;")
    s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
    s1 <- gsub(" ", "", s1)
    s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
    s2 <- as.character(toJSON(a))
    # cat(s1, "\n")
    # cat(s2, "\n\n")
    expect_identical(s1, s2)
    if (i == 0) {
      expect_identical(convertToR(o), array(character(0), c(0, 0)))
    } else {
      expect_identical(convertToR(o), a)
      if (j != 0) {
        p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
        expect_identical(convertToR(p), a)
      }
    }
  }
}

# Three-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      a <- array(as.character(1:(i * j * k)), c(i, j, k))
      o <- convertToJava(a)
      o <- rJava::.jcast(o, "[Ljava/lang/Object;")
      s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
      s1 <- gsub(" ", "", s1)
      s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
      s2 <- as.character(toJSON(a))
      # cat(s1, "\n")
      # cat(s2, "\n\n")
      expect_identical(s1, s2)
      if (i == 0) {
        expect_identical(convertToR(o), array(character(0), c(0, 0, 0)))
      } else if (i > 0 & j == 0) {
        expect_identical(convertToR(o), array(character(0), c(i, 0, 0)))
      } else {
        expect_identical(convertToR(o), a)
        if (j * k != 0) {
          p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
          expect_identical(convertToR(p), a)
        }
      }
    }
  }
}

# Four-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      for (l in 0:5) {
        a <- array(as.character(1:(i * j * k * l)), c(i, j, k, l))
        o <- convertToJava(a)
        o <- rJava::.jcast(o, "[Ljava/lang/Object;")
        s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
        s1 <- gsub(" ", "", s1)
        s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
        s2 <- as.character(toJSON(a))
        # cat(s1, "\n")
        # cat(s2, "\n\n")
        expect_identical(s1, s2)
        if (i * j * k != 0) {
          expect_identical(convertToR(o), a)
          if (l != 0) {
            p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
            expect_identical(convertToR(p), a)
          }
        }
      }
    }
  }
}
a <- array("a", c(1, 1, 0, 0))
expect_identical(convertToR(convertToJava(a)), a)
a <- array("a", c(1, 1, 1, 0))
expect_identical(convertToR(convertToJava(a)), a)
a <- array("a", c(1, 0, 1, 1))
expect_identical(convertToR(convertToJava(a)), array("a", c(1, 0, 0, 0)))
a <- array("a", c(1, 0, 0, 1))
expect_identical(convertToR(convertToJava(a)), array("a", c(1, 0, 0, 0)))
a <- array("a", c(1, 1, 0, 1))
expect_identical(convertToR(convertToJava(a)), array("a", c(1, 1, 0, 0)))

# Five-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      for (l in 0:5) {
        for (m in 0:5) {
          a <- array(as.character(1:(i * j * k * l * m)), c(i, j, k, l, m))
          o <- convertToJava(a)
          o <- rJava::.jcast(o, "[Ljava/lang/Object;")
          s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
          s1 <- gsub(" ", "", s1)
          s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
          s2 <- as.character(toJSON(a))
          # cat(s1, "\n")
          # cat(s2, "\n\n")
          expect_identical(s1, s2)
          if (i * j * k * l != 0) {
            expect_identical(convertToR(o), a)
            if (m != 0) {
              p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
              expect_identical(convertToR(p), a)
            }
          }
        }
      }
    }
  }
}
a <- array("a", c(1, 1, 0, 0, 0))
expect_identical(convertToR(convertToJava(a)), a)
a <- array("a", c(1, 1, 1, 0, 0))
expect_identical(convertToR(convertToJava(a)), a)
a <- array("a", c(1, 1, 1, 1, 0))
expect_identical(convertToR(convertToJava(a)), a)
a <- array("a", c(1, 0, 1, 1, 0))
expect_identical(convertToR(convertToJava(a)), array("a", c(1, 0, 0, 0, 0)))
a <- array("a", c(1, 0, 0, 1, 0))
expect_identical(convertToR(convertToJava(a)), array("a", c(1, 0, 0, 0, 0)))
a <- array("a", c(1, 1, 0, 1, 0))
expect_identical(convertToR(convertToJava(a)), array("a", c(1, 1, 0, 0, 0)))


# Column-major ------------------------------------------------------------

# One-dimensional
for (i in 0:5) {
  a <- array(as.character(1:i), c(i))
  o <- convertToJava(a, length.one.vector.as.array = TRUE, array.order = "column-major")
  o <- rJava::.jcast(o, "[Ljava/lang/Object;")
  s1 <- rJava::.jcall("java/util/Arrays", "S", "toString", o)
  s1 <- gsub(" ", "", s1)
  s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
  s2 <- as.character(toJSON(a, matrix = "columnmajor"))
  # cat(s1, "\n")
  # cat(s2, "\n\n")
  expect_identical(s1, s2)
  expect_identical(convertToR(o, array.order = "column-major"), as.vector(a))
  if (i != 0) {
    p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
    expect_identical(convertToR(p, array.order = "column-major"), as.vector(a))
  }
}

# Two-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    a <- array(as.character(1:(i * j)), c(i, j))
    o <- convertToJava(a, array.order = "column-major")
    o <- rJava::.jcast(o, "[Ljava/lang/Object;")
    s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
    s1 <- gsub(" ", "", s1)
    s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
    s2 <- as.character(toJSON(a, matrix = "columnmajor"))
    # cat(s1, "\n")
    # cat(s2, "\n\n")
    expect_identical(s1, s2)
    if (j == 0) {
      expect_identical(convertToR(o, array.order = "column-major"), array(character(0), c(0, 0)))
    } else {
      expect_identical(convertToR(o, array.order = "column-major"), a)
      if (i != 0) {
        p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
        expect_identical(convertToR(p, array.order = "column-major"), a)
      }
    }
  }
}

# Three-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      a <- array(as.character(1:(i * j * k)), c(i, j, k))
      o <- convertToJava(a, array.order = "column-major")
      o <- rJava::.jcast(o, "[Ljava/lang/Object;")
      s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
      s1 <- gsub(" ", "", s1)
      s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
      s2 <- as.character(toJSON(a, matrix = "columnmajor"))
      # cat(s1, "\n")
      # cat(s2, "\n\n")
      expect_identical(s1, s2)
      if (k == 0) {
        expect_identical(convertToR(o, array.order = "column-major"), array(character(0), c(0, 0, 0)))
      } else if (k > 0 & j == 0) {
        expect_identical(convertToR(o, array.order = "column-major"), array(character(0), c(0, 0, k)))
      } else {
        expect_identical(convertToR(o, array.order = "column-major"), a)
        if (i * j * k != 0) {
          p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
          expect_identical(convertToR(p, array.order = "column-major"), a)
        }
      }
    }
  }
}

# Four-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      for (l in 0:5) {
        a <- array(as.character(1:(i * j * k * l)), c(i, j, k, l))
        o <- convertToJava(a, array.order = "column-major")
        o <- rJava::.jcast(o, "[Ljava/lang/Object;")
        s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
        s1 <- gsub(" ", "", s1)
        s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
        s2 <- as.character(toJSON(a, matrix = "columnmajor"))
        # cat(s1, "\n")
        # cat(s2, "\n\n")
        expect_identical(s1, s2)
        if (j * k * l != 0) {
          expect_identical(convertToR(o, array.order = "column-major"), a)
          if (i * l != 0) {
            p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
            expect_identical(convertToR(p, array.order = "column-major"), a)
          }
        }
      }
    }
  }
}
a <- array("a", c(0, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), a)
a <- array("a", c(0, 1, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), a)
a <- array("a", c(1, 1, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), array("a", c(0, 0, 0, 1)))
a <- array("a", c(1, 0, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), array("a", c(0, 0, 0, 1)))
a <- array("a", c(1, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), array("a", c(0, 0, 1, 1)))

# Five-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      for (l in 0:5) {
        for (m in 0:5) {
          a <- array(as.character(1:(i * j * k * l * m)), c(i, j, k, l, m))
          o <- convertToJava(a, array.order = "column-major")
          o <- rJava::.jcast(o, "[Ljava/lang/Object;")
          s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
          s1 <- gsub(" ", "", s1)
          s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
          s2 <- as.character(toJSON(a, matrix = "columnmajor"))
          # cat(s1, "\n")
          # cat(s2, "\n\n")
          expect_identical(s1, s2)
          if (j * k * l * m != 0) {
            expect_identical(convertToR(o, array.order = "column-major"), a)
            if (i != 0) {
              p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
              expect_identical(convertToR(p, array.order = "column-major"), a)
            }
          }
        }
      }
    }
  }
}
a <- array("a", c(0, 0, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), a)
a <- array("a", c(0, 0, 1, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), a)
a <- array("a", c(0, 1, 1, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), a)
a <- array("a", c(0, 1, 1, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), array("a", c(0, 0, 0, 0, 1)))
a <- array("a", c(0, 1, 0, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), array("a", c(0, 0, 0, 0, 1)))
a <- array("a", c(0, 1, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-major"), array.order = "column-major"), array("a", c(0, 0, 0, 1, 1)))



# Column-minor ------------------------------------------------------------

# One-dimensional
for (i in 0:5) {
  a <- array(as.character(1:i), c(i))
  o <- convertToJava(a, length.one.vector.as.array = TRUE, array.order = "column-minor")
  o <- rJava::.jcast(o, "[Ljava/lang/Object;")
  s1 <- rJava::.jcall("java/util/Arrays", "S", "toString", o)
  s1 <- gsub(" ", "", s1)
  s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
  s2 <- as.character(toJSON(a))
  # cat(s1, "\n")
  # cat(s2, "\n\n")
  expect_identical(s1, s2)
  expect_identical(convertToR(o, array.order = "column-minor"), as.vector(a))
  if (i != 0) {
    p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
    expect_identical(convertToR(p, array.order = "column-minor"), as.vector(a))
  }
}

# Two-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    a <- array(as.character(1:(i * j)), c(i, j))
    o <- convertToJava(a, array.order = "column-minor")
    o <- rJava::.jcast(o, "[Ljava/lang/Object;")
    s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
    s1 <- gsub(" ", "", s1)
    s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
    s2 <- as.character(toJSON(t(a), matrix = "columnmajor"))
    # cat(s1, "\n")
    # cat(s2, "\n\n")
    expect_identical(s1, s2)
    if (i == 0) {
      expect_identical(convertToR(o, array.order = "column-minor"), array(character(0), c(0, 0)))
    } else {
      expect_identical(convertToR(o, array.order = "column-minor"), a)
      if (j != 0) {
        p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
        expect_identical(convertToR(p, array.order = "column-minor"), a)
      }
    }
  }
}

# Three-dimensional
# i <- 4; j <- 3; k <- 2
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      a <- array(as.character(1:(i * j * k)), c(i, j, k))
      b <- array("a", c(j, i, k))
      if (length(b)) {
        for (q in 1:k)
          b[, , q] <- t(a[, , q])
      }
      o <- convertToJava(a, array.order = "column-minor")
      o <- rJava::.jcast(o, "[Ljava/lang/Object;")
      s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
      s1 <- gsub(" ", "", s1)
      s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
      s2 <- as.character(toJSON(b, matrix = "columnmajor"))
      # cat(s1, "\n")
      # cat(s2, "\n\n")
      expect_identical(s1, s2)
      if (k == 0) {
        expect_identical(convertToR(o, array.order = "column-minor"), array(character(0), c(0, 0, 0)))
      } else if (k > 0 & i == 0) {
        expect_identical(convertToR(o, array.order = "column-minor"), array(character(0), c(0, 0, k)))
      } else {
        expect_identical(convertToR(o, array.order = "column-minor"), a)
        if (i * j * k != 0) {
          p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
          expect_identical(convertToR(p, array.order = "column-minor"), a)
        }
      }
    }
  }
}

# Four-dimensional
# i <- 4; j <- 3; k <- 2; l <- 2
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      for (l in 0:5) {
        a <- array(as.character(1:(i * j * k * l)), c(i, j, k, l))
        b <- array("a", c(j, i, k, l))
        if (length(b)) {
          for (q in 1:k) {
            for (r in 1:l) {
              b[, , q, r] <- t(a[, , q, r])
            }
          }
        }
        o <- convertToJava(a, array.order = "column-minor")
        o <- rJava::.jcast(o, "[Ljava/lang/Object;")
        s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
        s1 <- gsub(" ", "", s1)
        s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
        s2 <- as.character(toJSON(b, matrix = "columnmajor"))
        # cat(s1, "\n")
        # cat(s2, "\n\n")
        expect_identical(s1, s2)
        if (i * k * l != 0) {
          expect_identical(convertToR(o, array.order = "column-minor"), a)
          if (j != 0) {
            p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
            expect_identical(convertToR(p, array.order = "column-minor"), a)
          }
        }
      }
    }
  }
}
a <- array("a", c(0, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), a)
a <- array("a", c(0, 1, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 1, 1)))
a <- array("a", c(1, 1, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 0, 1)))
a <- array("a", c(1, 0, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 0, 1)))
a <- array("a", c(1, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(1, 0, 1, 1)))

# Five-dimensional
for (i in 0:5) {
  for (j in 0:5) {
    for (k in 0:5) {
      for (l in 0:5) {
        for (m in 0:5) {
          a <- array(as.character(1:(i * j * k * l * m)), c(i, j, k, l, m))
          b <- array("a", c(j, i, k, l, m))
          if (length(b)) {
            for (q in 1:k) {
              for (r in 1:l) {
                for (s in 1:m)
                  b[, , q, r, s] <- t(a[, , q, r, s])
              }
            }
          }
          o <- convertToJava(a, array.order = "column-minor")
          o <- rJava::.jcast(o, "[Ljava/lang/Object;")
          s1 <- rJava::.jcall("java/util/Arrays", "S", "deepToString", o)
          s1 <- gsub(" ", "", s1)
          s1 <- gsub("([0-9]+)", "\"\\1\"", s1)
          s2 <- as.character(toJSON(b, matrix = "columnmajor"))
          # cat(s1, "\n")
          # cat(s2, "\n\n")
          expect_identical(s1, s2)
          if (i * k * l * m != 0) {
            expect_identical(convertToR(o, array.order = "column-minor"), a)
            if (j != 0) {
              p <- rJava::.jcall(jdx:::jdx.utility, "Ljava/util/List;", "deepAsList", rJava::.jcast(o))
              expect_identical(convertToR(p, array.order = "column-minor"), a)
            }
          }
        }
      }
    }
  }
}
a <- array("a", c(0, 0, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), a)
a <- array("a", c(0, 0, 1, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), a)
a <- array("a", c(0, 1, 1, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 1, 1, 1)))
a <- array("a", c(0, 1, 1, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 0, 0, 1)))
a <- array("a", c(0, 1, 0, 0, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 0, 0, 1)))
a <- array("a", c(0, 1, 0, 1, 1))
expect_identical(convertToR(convertToJava(a, array.order = "column-minor"), array.order = "column-minor"), array("a", c(0, 0, 0, 1, 1)))

