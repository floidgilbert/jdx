package org.fgilbert.jdx;

///re-read all comments now that we support n-dimensional arrays. remove comments about matrices

/*
 * This class was written to optimize marshalling data between the JVM and R via rJava. 
 * A balance between performance and code clarity is the goal, but intuition has been sacrificed 
 * in many cases for the sake of speed. All code is designed to reduce the number of calls from R by rJava.   
 */

import java.lang.reflect.Array;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JavaToR {
	
	/*
	 * These constants are special values in R that are interpreted as NA for
	 * numeric (i.e. double) and integer types. NA_INT is used to signify NA for
	 * logical types in R, but Java booleans are bytes.
	 */
	private static final int NA_INT = Integer.MIN_VALUE;
	private static final double NA_DOUBLE = Double.longBitsToDouble(0x7ff00000000007a2L);
	
	/*
	 * rJava translates NA to FALSE for logicals (i.e. booleans).
	 */ 
	private static final boolean NA_ASSUMPTION_LOGICAL = false;
	private static final byte NA_ASSUMPTION_RAW = 0;
	
	public enum ArrayOrder {
		COLUMN_MAJOR
		, ROW_MAJOR
		, ROW_MAJOR_JAVA
	}
	
	/*
	 * All RdataTypeCode values must fit within the low 8 (0xFF) bits. These
	 * values will be combined in a composite code (see getRdataCompositeCode)
	 */
	public enum RdataTypeCode {
		NULL(0x00)
		, NUMERIC(0x01)
		, INTEGER(0x02)
		, CHARACTER(0x03)
		, LOGICAL(0x04)
		, RAW(0x05)
		, OTHER(0xFE) // Lists, data frames, user-defined, and exceptions.
		, UNSUPPORTED(0xFF) // An exception is thrown in this case.
		;
		
		final int value;
		
		RdataTypeCode(int value) {
			this.value = value;
		}
	}

	/*
	 * All RdataStructureCode values are from 0x100 to 0xFF00. These
	 * values will be combined in a composite code (see getRdataCompositeCode)
	 */
	public enum RdataStructureCode {
		SCALAR(0x000)
		, VECTOR(0x100)
		, ND_ARRAY(0X200)  
		, DATA_FRAME(0x300)
		, LIST(0x400)
		, NAMED_LIST(0x500)
		, USER_DEFINED(0xFF00)
		;
		
		final int value;
		
		RdataStructureCode(int value) {
			this.value = value;
		}
	}
	
	/*
	 * All RdataExceptionCode values must fit within 0x10000 and 0xFF0000. These
	 * values will be combined in a composite code (see getRdataCompositeCode)
	 */
	public enum RdataExceptionCode {
		NONE(0x00000)
		, EXCEPTION(0x10000)
		, WARNING_MISSING_LOGICAL_VALUES(0xA0000)
		, WARNING_MISSING_RAW_VALUES(0xB0000) 
		;
		
		final int value;
		
		RdataExceptionCode(int value) {
			this.value = value;
		}
	}
	
	/*
	 * This class is used to detect whether a collection represents a matrix.
	 */
	private class MaybeNdimensionalArray {

		private int[] dimensions;
		private boolean typeChanged = false;
		private RdataTypeCode typeCode;
		private boolean value = false;
		
		MaybeNdimensionalArray(int collectionSize, JavaToR j2r) {
			if (collectionSize == 0)
				return;
			switch (j2r.getRdataStructureCode()) {
			case VECTOR:
			case ND_ARRAY:
				value = true;
				break;
			default:
				return;
			}
			dimensions = j2r.getDimensions();
			typeCode = j2r.getRdataTypeCode();
		}
		
		int[] getDimensions() {
			return dimensions;
		}
		
		boolean getTypeChanged() {
			return typeChanged;
		}

		RdataTypeCode getTypeCode() {
			return typeCode;
		}
		
		boolean getValue() {
			return value;
		}
		
		boolean update(JavaToR j2r) {
			if (!value)
				return false;
			switch (j2r.getRdataStructureCode()) {
			case VECTOR:
			case ND_ARRAY:
				value = Arrays.equals(dimensions, j2r.dimensions);
				break;
			default:
				value = false;
				break;
			}
			if (!value)
				return false;
			
			// Check and update data typeCode.
			if (typeCode != j2r.getRdataTypeCode()) {
				if (typeCode == RdataTypeCode.NUMERIC && (j2r.getRdataTypeCode() == RdataTypeCode.INTEGER || j2r.getRdataTypeCode() == RdataTypeCode.RAW)) {
					// Do nothing. Integer and raw arrays will be coerced to numeric.
				} else if ((typeCode == RdataTypeCode.INTEGER || typeCode == RdataTypeCode.RAW) && j2r.getRdataTypeCode() == RdataTypeCode.NUMERIC) {
					// Change type to numeric. Integer and raw arrays will be coerced to numeric.
					typeCode = RdataTypeCode.NUMERIC;
					typeChanged = true;
				} else if (typeCode == RdataTypeCode.INTEGER && j2r.getRdataTypeCode() == RdataTypeCode.RAW) {
					// Do nothing. Raw arrays will be coerced to integer.
				} else if (typeCode == RdataTypeCode.RAW && j2r.getRdataTypeCode() == RdataTypeCode.INTEGER) {
					// Revert type to integer. Raw arrays will be coerced to integer.
					typeCode = RdataTypeCode.INTEGER;
					typeChanged = true;
				} else {
					value = false;
				}
			}
			return value;
		}
	}
	
	/*
	 * This class is used to detect whether a collection represents a row-major
	 * data frame.
	 */
	private class MaybeRowMajorDataFrame {
		
		final int compositeTypeScalarCharacter = RdataStructureCode.SCALAR.value | RdataTypeCode.CHARACTER.value; 
		final int compositeTypeScalarInteger = RdataStructureCode.SCALAR.value | RdataTypeCode.INTEGER.value; 
		final int compositeTypeScalarNull = RdataStructureCode.SCALAR.value | RdataTypeCode.NULL.value; 
		final int compositeTypeScalarNumeric = RdataStructureCode.SCALAR.value | RdataTypeCode.NUMERIC.value;
		final int compositeTypeScalarRaw = RdataStructureCode.SCALAR.value | RdataTypeCode.RAW.value;

		private int[] compositeTypes;
		private String[] names;
		private boolean value = false;
		
		MaybeRowMajorDataFrame(JavaToR j2r) {
			if (!j2r.isNamedListOfScalars)
				return;
			value = true;
			Object[] o = j2r.getValueObjectArray1d();
			compositeTypes = (int[]) o[0];
			compositeTypes = 
				Arrays.stream(compositeTypes)
				.map((int i) -> {
					if (i == this.compositeTypeScalarNull) return this.compositeTypeScalarCharacter; else return i;
				})
				.toArray();
			names = (String[]) o[2];
		}
		
		int[] getCompositeTypes() {
			return compositeTypes;
		}
		
		String[] getNames() {
			return names;
		}
		
		boolean getValue() {
			return value;
		}
		
		boolean update(JavaToR j2r) {
			if (!value)
				return false;
			value = false;
			if (!j2r.isNamedListOfScalars)
				return false;
			Object[] o = j2r.getValueObjectArray1d();
			int[] currentCompositeTypes = (int[]) o[0];
			if (currentCompositeTypes.length != this.compositeTypes.length)
				return false;
			for (int i = 0; i < currentCompositeTypes.length; i++) {
				if (currentCompositeTypes[i] != this.compositeTypes[i]) {
					if (this.compositeTypes[i] == this.compositeTypeScalarNumeric && (currentCompositeTypes[i] == this.compositeTypeScalarInteger || currentCompositeTypes[i] == this.compositeTypeScalarRaw)) {
						// Do nothing. Leave column type as numeric. Integer and raw values will be coerced to numeric later.
					} else if ((this.compositeTypes[i] == this.compositeTypeScalarInteger || this.compositeTypes[i] == this.compositeTypeScalarRaw) && currentCompositeTypes[i] == this.compositeTypeScalarNumeric) {
						// Change column to numeric. Integer and raw values will be coerced to numeric later.
						this.compositeTypes[i] = this.compositeTypeScalarNumeric;
					} else if (this.compositeTypes[i] == this.compositeTypeScalarInteger && currentCompositeTypes[i] == this.compositeTypeScalarRaw) {
						// Do nothing. Leave column type as integer. Raw values will be coerced to numeric later.
					} else if (this.compositeTypes[i] == this.compositeTypeScalarRaw && currentCompositeTypes[i] == this.compositeTypeScalarInteger) {
						// Change column to integer. Raw values will be coerced to integer later.
						this.compositeTypes[i] = this.compositeTypeScalarInteger;
					} else if (this.compositeTypes[i] == this.compositeTypeScalarCharacter && currentCompositeTypes[i] == this.compositeTypeScalarNull) {
						/*
						 * Do nothing. Columns and values of null will be
						 * converted to character with NA_character_ values.
						 * Note that in the constructor method of this class,
						 * all column types of compositeTypeScalarNull are
						 * replaced with compositeTypeScalarCharacter.
						 */
					} else {
						// The types are not compatible. The structure cannot be interpreted a row-major data frame.
						return false;
					}
				}
			}
			if (!Arrays.equals(this.names, (String[]) o[2]))
				return false;
			value = true;
			return true;
		}
		
	}
	
	/*
	 * IMPORTANT! Any new module-level variables must be added to methods
	 * `initialize` and `initializeFrom`. This is kludgey to be sure, but these
	 * methods are used to improve performance on the R side by minimizing
	 * expensive calls to create new object references in rJava.
	 */
	private ArrayOrder arrayOrder;
	private int[] dimensions;
	private boolean isNamedListOfScalars; // Used to detect row major data frames.
	private RdataExceptionCode rDataExceptionCode;
	private RdataTypeCode rDataTypeCode;
	private RdataStructureCode rDataStructureCode;
	private int rDataUserDefinedCode;
	private Object value;
	
	public JavaToR() {
		initialize(null, ArrayOrder.ROW_MAJOR);
	}
	
	public JavaToR(Object value) {
		initialize(value, ArrayOrder.ROW_MAJOR);
	}
	
	public JavaToR(Object value, ArrayOrder arrayOrder) {
		initialize(value, arrayOrder);
	}
	
	public JavaToR(Object value, int rDataUserDefinedCode) {
		initialize(value, rDataUserDefinedCode);
	}
	
	private double[] coerceArray1D(BigDecimal[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private double[] coerceArray1D(BigInteger[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private String[] coerceArray1D(char[] a) {
		if (a == null)
			return null;
		String[] b = new String[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = Character.toString(a[i]);
		return b;
	}

	private String[] coerceArray1D(Character[] a) {
		if (a == null)
			return null;
		String[] b = new String[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? null : a[i].toString();
		return b;
	}

	private double[] coerceArray1D(float[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (double) a[i];
		return b;
	}

	private double[] coerceArray1D(Float[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private double[] coerceArray1D(long[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (double) a[i];
		return b;
	}

	private double[] coerceArray1D(Long[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private int[] coerceArray1D(short[] a) {
		if (a == null)
			return null;
		int[] b = new int[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (int) a[i];
		return b;
	}

	private int[] coerceArray1D(Short[] a) {
		if (a == null)
			return null;
		int[] b = new int[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_INT : a[i].intValue();
		return b;
	}

	private boolean[] coerceCollectionToBooleanArray1D(Collection<Boolean> a) {
		if (a == null)
			return null;
		boolean[] z = new boolean[a.size()];
		Iterator<Boolean> iter = a.iterator();
		Boolean bool;
		for (int i = 0; i < z.length; i++) {
			bool = iter.next();
			if (bool == null) {
				z[i] = NA_ASSUMPTION_LOGICAL;
				this.rDataExceptionCode = RdataExceptionCode.WARNING_MISSING_LOGICAL_VALUES;
			} else {
				z[i] = bool.booleanValue();
			}
		}
		return z;
	}
	
	private byte[] coerceCollectionToByteArray1D(Collection<Number> a) {
		if (a == null)
			return null;
		byte[] b = new byte[a.size()];
		Iterator<Number> iter = a.iterator();
		Number nu;
		for (int i = 0; i < b.length; i++) {
			nu = iter.next();
			if (nu == null) {
				b[i] = NA_ASSUMPTION_RAW;
				this.rDataExceptionCode = RdataExceptionCode.WARNING_MISSING_RAW_VALUES;
			} else {
				b[i] = nu.byteValue();
			}
		}
		return b;
	}
	private double[] coerceCollectionToDoubleArray1D(Collection<Number> a) {
		if (a == null)
			return null;
		double[] b = new double[a.size()];
		Iterator<Number> iter = a.iterator();
		Number nu;
		for (int i = 0; i < b.length; i++) {
			nu = iter.next();
			b[i] = (nu == null) ? NA_DOUBLE : nu.doubleValue();
		}
		return b;
	}
	
	private int[] coerceCollectionToIntArray1D(Collection<Number> a) {
		if (a == null)
			return null;
		int[] b = new int[a.size()];
		Iterator<Number> iter = a.iterator();
		Number nu;
		for (int i = 0; i < b.length; i++) {
			nu = iter.next();
			b[i] = (nu == null ? NA_INT : nu.intValue());
		}
		return b;
	}
	
	private String[] coerceCollectionToStringArray1D(Collection<?> a) {
		if (a == null)
			return null;
		String[] b = new String[a.size()];
		Iterator<?> iter = a.iterator();
		Object o;
		for (int i = 0; i < b.length; i++) {
			o = iter.next();
			b[i] = (o == null) ? null : o.toString();
		}
		return b;
	}
	
	/*
	 * Collections are converted to vectors, n-dimensional arrays, data frames,
	 * or unnamed lists depending on the content. See convertCollectionToArray1D
	 * for rules used to convert collections to vectors. Collections are
	 * converted to matrices if it contains > 1 similarly-typed, same-length
	 * vectors. Collections are converted to data frames if they contain
	 * homogeneous named lists (i.e. Java maps). All other combinations of
	 * objects/values will be converted to unnamed lists.
	 */
	private void convertCollection() {
		///remember to set this.dimensions when converting to n-dimensional array. or even one-dimensional for that matter.
		Collection<?> col = (Collection<?>) this.value;
		if (col.isEmpty()) {
			// Return empty list.
			this.rDataTypeCode = RdataTypeCode.OTHER;
			this.rDataStructureCode = RdataStructureCode.LIST;
			this.value = new Object[] {new int[] {}, new Object[] {}};
			return;
		}
		if (convertCollectionToArray1D(col))
			return;
		
		/*
		 * At this point we know it's not empty or an array. Now we iterate
		 * through each to build an R unnamed list. Along the way, we check to
		 * see if it can instead be converted to a matrix or data frame.
		 */
		Iterator<?> iter = col.iterator();
		Object o = iter.next();
		JavaToR j2r = new JavaToR(o, this.arrayOrder);
		int[] compositeTypes = new int[col.size()];
		compositeTypes[0] = j2r.getRdataCompositeCode();
		Object[] objects = new Object[col.size()];
		objects[0] = j2r.getValueObject();

		MaybeNdimensionalArray maybeNdimensionalArray = new MaybeNdimensionalArray(col.size(), j2r);
		MaybeRowMajorDataFrame maybeRowMajorDataFrame = new MaybeRowMajorDataFrame(j2r);
		for (int i = 1; i < compositeTypes.length; i++) {
			j2r = new JavaToR(iter.next(), this.arrayOrder);
			compositeTypes[i] = j2r.getRdataCompositeCode();
			objects[i] = j2r.getValueObject();
			if (maybeNdimensionalArray.getValue()){
				maybeNdimensionalArray.update(j2r);
			} else if (maybeRowMajorDataFrame.getValue()) {
				maybeRowMajorDataFrame.update(j2r);
			}
		}
		if (maybeNdimensionalArray.getValue()) {
			convertCollectionToArrayND(maybeNdimensionalArray, objects, compositeTypes);
			return;
		} else if (maybeRowMajorDataFrame.getValue()) {
			convertCollectionToDataFrame(maybeRowMajorDataFrame, objects);
			return;
		}
		this.rDataTypeCode = RdataTypeCode.OTHER;
		this.rDataStructureCode = RdataStructureCode.LIST;
		this.value = new Object[] {compositeTypes, objects};
	}
	
	/*
	 * This function is called only from within convertCollection.
	 * 
	 * Attempts to convert a collection of scalars to a one-dimensional array.
	 * Returns false if it fails to do so. The conversions are as follows. Mix
	 * of Byte, null -> byte array. Mix of Integer, Short, Byte, null -> integer
	 * array. Mix of Double, Long, Float, BigInteger, BigDecimal and all other
	 * Number types -> double array. Mix of String, Character, null -> String
	 * array. Mix of Boolean, null -> boolean array.
	 * 
	 * Any other combinations will return false.
	 */
	@SuppressWarnings("unchecked")
	private boolean convertCollectionToArray1D(Collection<?> col) {
		boolean characterVector = false;
		boolean integerVector = false;
		boolean logicalVector = false;
		boolean numericVector = false;
		boolean rawVector = false;
		
		Iterator<?> iter = col.iterator();
		Class<?> cls = null;
		Object o = null;
		while (iter.hasNext()) {
			o = iter.next();
			if (o != null) {
				cls = o.getClass();
				if (Number.class.isAssignableFrom(cls)) {
					if (cls.equals(Double.class) || cls.equals(Long.class) || cls.equals(Float.class) || cls.equals(BigDecimal.class) || cls.equals(BigInteger.class)) {
						numericVector = true;
					} else if (cls.equals(Integer.class) || cls.equals(Short.class)) {
						integerVector = true;
					} else if (cls.equals(Byte.class)) {
						rawVector = true;
					}
					if (characterVector || logicalVector)
						return false;
				} else {
					if (cls.equals(String.class) || cls.equals(Character.class)) {
						if (numericVector || integerVector || rawVector || logicalVector)
							return false;
						characterVector = true;
					} else if (cls.equals(Boolean.class)) {
						if (numericVector || integerVector || rawVector || characterVector)
							return false;
						logicalVector = true;
					} else {
						return false;
					}
				}
			}
		}
		/*
		 * The three numeric types are not exclusive of each other and therefore
		 * must be tested in this order (from general to specific).
		 */
		if (numericVector) {
			this.dimensions = new int[] {col.size()};
			this.rDataTypeCode = RdataTypeCode.NUMERIC;
			this.rDataStructureCode = RdataStructureCode.VECTOR;
			this.value = coerceCollectionToDoubleArray1D((Collection<Number>) col);
			return true;
		}
		if (integerVector) {
			this.dimensions = new int[] {col.size()};
			this.rDataTypeCode = RdataTypeCode.INTEGER;
			this.rDataStructureCode = RdataStructureCode.VECTOR;
			this.value = coerceCollectionToIntArray1D((Collection<Number>) col);
			return true;
		}
		if (rawVector) {
			this.dimensions = new int[] {col.size()};
			this.rDataTypeCode = RdataTypeCode.RAW;
			this.rDataStructureCode = RdataStructureCode.VECTOR;
			this.value = coerceCollectionToByteArray1D((Collection<Number>) col);
			return true;
		}
		// Character and Logical vectors are exclusive (they don't mix with other types).
		if (characterVector) {
			this.dimensions = new int[] {col.size()};
			this.rDataTypeCode = RdataTypeCode.CHARACTER;
			this.rDataStructureCode = RdataStructureCode.VECTOR;
			this.value = coerceCollectionToStringArray1D(col);
			return true;
		}
		if (logicalVector) {
			this.dimensions = new int[] {col.size()};
			this.rDataTypeCode = RdataTypeCode.LOGICAL;
			this.rDataStructureCode = RdataStructureCode.VECTOR;
			this.value = coerceCollectionToBooleanArray1D((Collection<Boolean>) col);
			return true;
		}
		// The collection contains only nulls.
		return false;
	}
	
	///move this elsewhere
	private Object coerceArrayND(Object array, Class<?> componentTypeSource, Class<?> componentTypeTarget) {
		
		if (double.class.equals(componentTypeTarget)) {
			
		}
		switch(maybeNdimensionalArray.getTypeCode()) {
		case NUMERIC:
			double[][] d = new double[objects.length][];
			for (int i = 0; i < objects.length; i++) {
				/*
				 * It is possible to get a mix of double[], int[], and byte[] sub-arrays.
				 * Convert all to double[].
				 * 
				 * All arrays have been unboxed by this point.
				 */
				Class<?> ct = objects[i].getClass().getComponentType(); 
				if (ct.equals(Double.TYPE)) {
					d[i] = (double[]) objects[i];
				} else if (ct.equals(Integer.TYPE)) {
					d[i] = Arrays.stream((int[]) objects[i]).asDoubleStream().toArray();
				} else if (ct.equals(Byte.TYPE)) {
					byte[] b = (byte[]) objects[i];
					double[] subarray = new double[b.length];
					for (int j = 0; j < subarray.length; j++)
						subarray[j] = (double) b[j];
					d[i] = subarray;
				} else {
					throw new RuntimeException(String.format("Java class '%s' is not supported for matrix subarray conversion.", ct.getName()));
				}
			}
			this.value = d;
			this.rDataTypeCode = RdataTypeCode.NUMERIC;
			this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
			return;
		case INTEGER:
			int[][] n = new int[objects.length][];
			for (int i = 0; i < objects.length; i++) {
				/*
				 * It is possible to get a mix of int[], and byte[] sub-arrays.
				 * Convert all to int[].
				 * 
				 * All arrays have been unboxed by this point.
				 */
				Class<?> ct = objects[i].getClass().getComponentType(); 
				if (ct.equals(Integer.TYPE)) {
					n[i] = (int[]) objects[i];
				} else if (ct.equals(Byte.TYPE)) {
					byte[] b = (byte[]) objects[i];
					int[] subarray = new int[b.length];
					for (int j = 0; j < subarray.length; j++)
						subarray[j] = (int) b[j];
					n[i] = subarray;
				} else {
					throw new RuntimeException(String.format("Java class '%s' is not supported for matrix subarray conversion.", ct.getName()));
				}
			}
			this.value = n;
			this.rDataTypeCode = RdataTypeCode.INTEGER;
			this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
			return;
		case CHARACTER:
			String[][] s = new String[objects.length][];
			for (int i = 0; i < objects.length; i++) {
				s[i] = (String[]) objects[i];
			}
			this.value = s;
			this.rDataTypeCode = RdataTypeCode.CHARACTER;
			this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
			return;
		case LOGICAL:
			boolean[][] z = new boolean[objects.length][];
			for (int i = 0; i < objects.length; i++) {
				// All arrays have been unboxed by this point. 
				z[i] = (boolean[]) objects[i];
			}
			this.value = z;
			this.rDataTypeCode = RdataTypeCode.LOGICAL;
			this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
			return;
		case RAW:
			byte[][] b = new byte[objects.length][];
			for (int i = 0; i < objects.length; i++) {
				// All arrays have been unboxed by this point. 
				b[i] = (byte[]) objects[i];
			}
			this.value = b;
			this.rDataTypeCode = RdataTypeCode.RAW;
			this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
			return;
		default:
			throw new RuntimeException(String.format("The R data type 0x%X is not supported for an n-dimensional array.", maybeNdimensionalArray.getTypeCode().value));
		}
		return null;
	}

	// This function is called only from within convertCollection.
	private void convertCollectionToArrayND(MaybeNdimensionalArray maybeNdimensionalArray, Object[] objects, int[] compositeTypes) {
		///remove datatypecodetojavaclass stuff and just use inttypecodes.
		int[] subarrayDimensions = maybeNdimensionalArray.getDimensions();
		int[] newDimensions = new int[subarrayDimensions.length + 1];
		newDimensions[0] = objects.length;
		for (int i = 0; i < subarrayDimensions.length; i++)
			newDimensions[i + 1] = subarrayDimensions[i];
		int sourceDataTypeCodeInt;
		Class<?> sourceClass = null;
		int targetDataTypeCodeInt = maybeNdimensionalArray.getTypeCode().value;
		Class<?> targetClass = dataTypeCodeToJavaClass(maybeNdimensionalArray.getTypeCode());
		Object array = Array.newInstance(targetClass, newDimensions);
		if (maybeNdimensionalArray.getTypeChanged()) {
			for (int i = 0; i < newDimensions.length; i++) {
				sourceDataTypeCodeInt = compositeTypes[i] & 0xFF;
				if (sourceDataTypeCodeInt == targetDataTypeCodeInt) {
					Array.set(array, i, objects[i]);
				} else {
					sourceClass = dataTypeCodeIntToJavaClass(sourceDataTypeCodeInt);
					Array.set(array, i, coerceArrayND(objects[i], sourceClass, targetClass));
				}
			}
		} else {
			for (int i = 0; i < this.dimensions.length; i++)
				Array.set(array, i, objects[i]);
		}
		this.dimensions = newDimensions;
		this.value = new Object[] {this.dimensions, array};
		this.rDataTypeCode = maybeNdimensionalArray.getTypeCode();
		this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
		return;
	}
	
	/*
	 * This function is called only from within convertCollection.
	 * 
	 * Converts a collection of named lists to a data frame. The data is assumed
	 * to be validated ahead of time.
	 */
	private void convertCollectionToDataFrame(MaybeRowMajorDataFrame maybeRowMajorDataFrame, Object[] lists) {
		
		int[] compositeTypes = maybeRowMajorDataFrame.getCompositeTypes();
		String[] names = maybeRowMajorDataFrame.getNames();
		Object[] columns = new Object[compositeTypes.length];
		
		/*
		 * Allocate column arrays.
		 * 
		 * We must deal with the composite codes (RdataTypeCode combined with
		 * RdataStructureCode) instead of Java enums because each row has
		 * already been converted to R-compatible structures.
		 */
		for (int i = 0; i < compositeTypes.length; i++) {
			int typeCode = compositeTypes[i] & 0xFF;
			compositeTypes[i] = RdataStructureCode.VECTOR.value | typeCode;
			if (typeCode == RdataTypeCode.NUMERIC.value) {
				columns[i] = new double[lists.length];
			} else if (typeCode == RdataTypeCode.INTEGER.value) {
				columns[i] = new int[lists.length];
			} else if (typeCode == RdataTypeCode.CHARACTER.value) {
				columns[i] = new String[lists.length];
			} else if (typeCode == RdataTypeCode.LOGICAL.value) {
				columns[i] = new boolean[lists.length];
			} else if (typeCode == RdataTypeCode.RAW.value) {
				columns[i] = new byte[lists.length];
			} else {
				throw new RuntimeException(String.format("The R data type code 0x%X is not supported for a data frame column structure.", typeCode));
			}
		}

		// Populate columns.
		double[] d;	int[] n; boolean[] z; byte[] b; String[] s; 
		for (int i = 0; i < lists.length; i++) {
			Object[] list = (Object[]) lists[i];
			Object[] values = (Object[]) list[1];
			for (int j = 0; j < values.length; j++) {
				int typeCode = compositeTypes[j] & 0xFF; 
				if (typeCode == RdataTypeCode.NUMERIC.value) {
					d = (double[]) columns[j];
					d[i] = ((Number) values[j]).doubleValue();
				} else if (typeCode == RdataTypeCode.INTEGER.value) {
					n = (int[]) columns[j];
					n[i] = ((Number) values[j]).intValue();
				} else if (typeCode == RdataTypeCode.CHARACTER.value) {
					s = (String[]) columns[j];
					s[i] = (String) values[j];
				} else if (typeCode == RdataTypeCode.LOGICAL.value) {
					z = (boolean[]) columns[j];
					z[i] = (boolean) values[j];
				} else if (typeCode == RdataTypeCode.RAW.value) {
					b = (byte[]) columns[j];
					b[i] = (byte) values[j];
				}
			}
		}
		
		this.rDataTypeCode = RdataTypeCode.OTHER;
		this.rDataStructureCode = RdataStructureCode.DATA_FRAME;
		this.value = new Object[] {compositeTypes, columns, names};
	}

	/*
	 * Maps are converted to data frames if they contain more than one
	 * one-dimensional arrays/collections of the same length. All other contents
	 * are converted to named lists.
	 */
	private void convertMap() {
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) value;
		String[] names = new String[m.size()];
		try {
			m.keySet().toArray(names);
		} catch (ArrayStoreException e) {
			throw new RuntimeException("Map keys must be string types.");
		}
		Iterator<?> iter = m.values().iterator();
		int[] types = new int[m.size()];
		Object[] objects = new Object[m.size()];
		JavaToR j2r;
		this.isNamedListOfScalars = true;
		boolean isDataFrame = m.size() > 1;
		int vectorLength = -1;
		for (int i = 0; i < m.size(); i++) {
			j2r = new JavaToR(iter.next(), this.arrayOrder);
			types[i] = j2r.getRdataCompositeCode();
			objects[i] = j2r.getValueObject();
			if (this.isNamedListOfScalars)
				this.isNamedListOfScalars = j2r.getRdataStructureCode() == RdataStructureCode.SCALAR; 
			if (isDataFrame) {
				isDataFrame = j2r.getRdataStructureCode() == RdataStructureCode.VECTOR;
				if (isDataFrame) {
					if (vectorLength != -1) {
						isDataFrame = vectorLength == Array.getLength(objects[i]);
					} else {
						vectorLength = Array.getLength(objects[i]);
					}
				}
			}
		}
		this.rDataTypeCode = RdataTypeCode.OTHER;
		this.rDataStructureCode = isDataFrame ? RdataStructureCode.DATA_FRAME : RdataStructureCode.NAMED_LIST;
		this.isNamedListOfScalars = this.isNamedListOfScalars & !isDataFrame; 
		this.value = new Object[] {types, objects, names};
	}

	private void convertNdimensionalBooleanArray() {
		int flatLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatLength *= this.dimensions[i];
		boolean[] flatArray = new boolean[flatLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for ROW_MAJOR_JAVA
		int rowCount = this.dimensions[this.dimensions.length - 2];
		int columnCount = this.dimensions[this.dimensions.length - 1];
		int matrixIndex = 0;
		for (int i = 0; i < subarrayCount; i++) {
			Object o = this.value;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			// Coerces/unboxes array to int[].
			JavaToR j2r = new JavaToR(Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]), this.arrayOrder);
			boolean[] subarray = j2r.getValueBooleanArray1d();
			this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					/*
					 *  The offset is the same as subarrayCount because of the way the data structure fills
					 *  to mimic R's indexing scheme. Step through it to see how it works.
					 */
					flatArray[currentFlatArrayIndex + subarrayCount * j] = subarray[j];
				currentFlatArrayIndex++; 
				for (int j = 0; j < currentSubarrayIndex.length; j++) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case COLUMN_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					flatArray[currentFlatArrayIndex++] = subarray[j];
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case ROW_MAJOR_JAVA:
				for (int j = 0; j < columnCount; j++)
					flatArray[currentFlatArrayIndex + rowCount * j] = subarray[j];
				if ((i + 1) % rowCount == 0) {
					matrixIndex++;
					currentFlatArrayIndex = matrixIndex * rowCount * columnCount;
				} else {
					currentFlatArrayIndex++;
				}
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			}
		}
		// Update dimensions to R index order (i.e. row-major order).
		switch (this.arrayOrder) {
		case ROW_MAJOR:
			break;
		case COLUMN_MAJOR:
			Utility.reverseArray(this.dimensions);
			break;
		case ROW_MAJOR_JAVA:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalByteArray() {
		int flatLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatLength *= this.dimensions[i];
		byte[] flatArray = new byte[flatLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for ROW_MAJOR_JAVA
		int rowCount = this.dimensions[this.dimensions.length - 2];
		int columnCount = this.dimensions[this.dimensions.length - 1];
		int matrixIndex = 0;
		for (int i = 0; i < subarrayCount; i++) {
			Object o = this.value;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			// Coerces/unboxes array to int[].
			JavaToR j2r = new JavaToR(Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]), this.arrayOrder);
			byte[] subarray = j2r.getValueByteArray1d();			
			this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					/*
					 *  The offset is the same as subarrayCount because of the way the data structure fills
					 *  to mimic R's indexing scheme. Step through it to see how it works.
					 */
					flatArray[currentFlatArrayIndex + subarrayCount * j] = subarray[j];
				currentFlatArrayIndex++; 
				for (int j = 0; j < currentSubarrayIndex.length; j++) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case COLUMN_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					flatArray[currentFlatArrayIndex++] = subarray[j];
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case ROW_MAJOR_JAVA:
				for (int j = 0; j < columnCount; j++)
					flatArray[currentFlatArrayIndex + rowCount * j] = subarray[j];
				if ((i + 1) % rowCount == 0) {
					matrixIndex++;
					currentFlatArrayIndex = matrixIndex * rowCount * columnCount;
				} else {
					currentFlatArrayIndex++;
				}
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			}
		}
		// Update dimensions to R index order (i.e. row-major order).
		switch (this.arrayOrder) {
		case ROW_MAJOR:
			break;
		case COLUMN_MAJOR:
			Utility.reverseArray(this.dimensions);
			break;
		case ROW_MAJOR_JAVA:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalDoubleArray() {
		int flatLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatLength *= this.dimensions[i];
		double[] flatArray = new double[flatLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for ROW_MAJOR_JAVA
		int rowCount = this.dimensions[this.dimensions.length - 2];
		int columnCount = this.dimensions[this.dimensions.length - 1];
		int matrixIndex = 0;
		for (int i = 0; i < subarrayCount; i++) {
			Object o = this.value;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			// Coerces/unboxes array to int[].
			JavaToR j2r = new JavaToR(Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]), this.arrayOrder);
			double[] subarray = j2r.getValueDoubleArray1d();			
			this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					/*
					 *  The offset is the same as subarrayCount because of the way the data structure fills
					 *  to mimic R's indexing scheme. Step through it to see how it works.
					 */
					flatArray[currentFlatArrayIndex + subarrayCount * j] = subarray[j];
				currentFlatArrayIndex++; 
				for (int j = 0; j < currentSubarrayIndex.length; j++) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case COLUMN_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					flatArray[currentFlatArrayIndex++] = subarray[j];
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case ROW_MAJOR_JAVA:
				for (int j = 0; j < columnCount; j++)
					flatArray[currentFlatArrayIndex + rowCount * j] = subarray[j];
				if ((i + 1) % rowCount == 0) {
					matrixIndex++;
					currentFlatArrayIndex = matrixIndex * rowCount * columnCount;
				} else {
					currentFlatArrayIndex++;
				}
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			}
		}
		// Update dimensions to R index order (i.e. row-major order).
		switch (this.arrayOrder) {
		case ROW_MAJOR:
			break;
		case COLUMN_MAJOR:
			Utility.reverseArray(this.dimensions);
			break;
		case ROW_MAJOR_JAVA:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalIntArray() {
		int flatLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatLength *= this.dimensions[i];
		int[] flatArray = new int[flatLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for ROW_MAJOR_JAVA
		int rowCount = this.dimensions[this.dimensions.length - 2];
		int columnCount = this.dimensions[this.dimensions.length - 1];
		int matrixIndex = 0;
		for (int i = 0; i < subarrayCount; i++) {
			Object o = this.value;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			// Coerces/unboxes array to int[].
			JavaToR j2r = new JavaToR(Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]), this.arrayOrder);
			int[] subarray = j2r.getValueIntArray1d();			
			this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					/*
					 *  The offset is the same as subarrayCount because of the way the data structure fills
					 *  to mimic R's indexing scheme. Step through it to see how it works.
					 */
					flatArray[currentFlatArrayIndex + subarrayCount * j] = subarray[j];
				currentFlatArrayIndex++; 
				for (int j = 0; j < currentSubarrayIndex.length; j++) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case COLUMN_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					flatArray[currentFlatArrayIndex++] = subarray[j];
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case ROW_MAJOR_JAVA:
				for (int j = 0; j < columnCount; j++)
					flatArray[currentFlatArrayIndex + rowCount * j] = subarray[j];
				if ((i + 1) % rowCount == 0) {
					matrixIndex++;
					currentFlatArrayIndex = matrixIndex * rowCount * columnCount;
				} else {
					currentFlatArrayIndex++;
				}
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			}
		}
		// Update dimensions to R index order (i.e. row-major order).
		switch (this.arrayOrder) {
		case ROW_MAJOR:
			break;
		case COLUMN_MAJOR:
			Utility.reverseArray(this.dimensions);
			break;
		case ROW_MAJOR_JAVA:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalStringArray() {
		int flatLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatLength *= this.dimensions[i];
		String[] flatArray = new String[flatLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for ROW_MAJOR_JAVA
		int rowCount = this.dimensions[this.dimensions.length - 2];
		int columnCount = this.dimensions[this.dimensions.length - 1];
		int matrixIndex = 0;
		for (int i = 0; i < subarrayCount; i++) {
			Object o = this.value;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			// Coerces/unboxes array to int[].
			JavaToR j2r = new JavaToR(Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]), this.arrayOrder);
			String[] subarray = j2r.getValueStringArray1d();			
			this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					/*
					 *  The offset is the same as subarrayCount because of the way the data structure fills
					 *  to mimic R's indexing scheme. Step through it to see how it works.
					 */
					flatArray[currentFlatArrayIndex + subarrayCount * j] = subarray[j];
				currentFlatArrayIndex++; 
				for (int j = 0; j < currentSubarrayIndex.length; j++) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case COLUMN_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
					flatArray[currentFlatArrayIndex++] = subarray[j];
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			case ROW_MAJOR_JAVA:
				for (int j = 0; j < columnCount; j++)
					flatArray[currentFlatArrayIndex + rowCount * j] = subarray[j];
				if ((i + 1) % rowCount == 0) {
					matrixIndex++;
					currentFlatArrayIndex = matrixIndex * rowCount * columnCount;
				} else {
					currentFlatArrayIndex++;
				}
				for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
					if (currentSubarrayIndex[j] < this.dimensions[j] - 1) {
						currentSubarrayIndex[j]++;
						break;
					}
					currentSubarrayIndex[j] = 0;
				}
				break;
			}
		}
		// Update dimensions to R index order (i.e. row-major order).
		switch (this.arrayOrder) {
		case ROW_MAJOR:
			break;
		case COLUMN_MAJOR:
			Utility.reverseArray(this.dimensions);
			break;
		case ROW_MAJOR_JAVA:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	/*
	 * Nashorn JavaScript returns ScriptObjectMirror objects for anything that
	 * is not a scalar, including native JavaScript arrays. The JS arrays are
	 * converted to vectors, matrices, or lists via convertCollection().
	 */	
	private void convertScriptObjectMirror() {
		ScriptObjectMirror som = (ScriptObjectMirror) this.value;
		if (som.isArray()) {
			this.initializeFrom(new JavaToR(som.values(), this.arrayOrder));
		} else if (som.isFunction() || som.isStrictFunction()) {
			/*
			 * Because the last expression of a script is returned, a script
			 * containing a function will return a function. I could raise an
			 * error, but this is annoying and unexpected. Return R NULL
			 * instead.
			 */
			this.rDataTypeCode = RdataTypeCode.NULL;
			this.rDataStructureCode = RdataStructureCode.SCALAR;
			this.rDataExceptionCode = RdataExceptionCode.NONE;
			this.value = null;
		} else {
			convertMap();
		}
	}
	
	/*
	 * Assumes that this.rDataStructureCode has already been set. Sets
	 * this.rDataTypeCode and this.value.
	 */
	private void convertSimpleStructure(Class<?> cls) {
		if (cls.isPrimitive()) {
			if (cls.equals(Double.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Integer.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.INTEGER;
				if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalIntArray();
				return;
			}
			if (cls.equals(Boolean.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.LOGICAL;
				if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalBooleanArray();
				return;
			}
			if (cls.equals(Byte.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.RAW;
				if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalByteArray();
				return;
			}
			if (cls.equals(Float.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((float[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Long.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((long[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Short.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.INTEGER;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((short[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalIntArray();
				return;
			}
			if (cls.equals(Character.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.CHARACTER;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = Character.toString((char) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((char[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalStringArray();
				return;
			}
		}
		if (Number.class.isAssignableFrom(cls)) {
			if (cls.equals(Double.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = unboxArray1D((Double[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Integer.class)) {
				this.rDataTypeCode = RdataTypeCode.INTEGER;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = unboxArray1D((Integer[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalIntArray();
				return;
			}
			if (cls.equals(Byte.class)) {
				this.rDataTypeCode = RdataTypeCode.RAW;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = unboxArray1D((Byte[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalByteArray();
				return;
			}
			if (cls.equals(Float.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((Float) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((Float[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Long.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((Long) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((Long[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Short.class)) {
				this.rDataTypeCode = RdataTypeCode.INTEGER;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((Short) this.value).intValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((Short[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalIntArray();
				return;
			}
			if (cls.equals(BigDecimal.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((BigDecimal) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((BigDecimal[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(BigInteger.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((BigInteger) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceArray1D((BigInteger[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
		}
		if (cls.equals(String.class)) {
			this.rDataTypeCode = RdataTypeCode.CHARACTER;
			if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
				convertNdimensionalStringArray();
			return;
		}
		if (cls.equals(Boolean.class)) {
			this.rDataTypeCode = RdataTypeCode.LOGICAL;
			if (this.rDataStructureCode == RdataStructureCode.VECTOR)
				this.value = unboxArray1D((Boolean[]) this.value);
			else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
				convertNdimensionalBooleanArray();
			return;
		}
		if (cls.equals(Character.class)) {
			this.rDataTypeCode = RdataTypeCode.CHARACTER;
			if (this.rDataStructureCode == RdataStructureCode.SCALAR)
				this.value = ((Character) value).toString();
			else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
				this.value = coerceArray1D((Character[]) this.value);
			else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
				convertNdimensionalStringArray();
			return;
		}
		this.rDataTypeCode = RdataTypeCode.UNSUPPORTED;
	}
	
	private Class<?> dataTypeCodeIntToJavaClass(int value) {
		if (value == RdataTypeCode.NUMERIC.value) {
			return double.class;
		} else if (value == RdataTypeCode.INTEGER.value) {
			return int.class;
		} else if (value == RdataTypeCode.CHARACTER.value) {
			return String.class;
		} else if (value == RdataTypeCode.LOGICAL.value) {
			return boolean.class;
		} else if (value == RdataTypeCode.RAW.value) {
			return byte.class;
		} else {
			throw new RuntimeException(String.format("The R data type code 0x%X does not correspond to a Java class.", value));
		}
	}

	private Class<?> dataTypeCodeToJavaClass(RdataTypeCode value) {
		switch (value) {
		case NUMERIC:
			return double.class;
		case INTEGER:
			return int.class;
		case CHARACTER:
			return String.class;
		case LOGICAL:
			return boolean.class;
		case RAW:
			return byte.class;
		default:
			throw new RuntimeException(String.format("The R data type code 0x%X does not correspond to a Java class.", value.value));
		}
	}

	public ArrayOrder getArrayOrder() {
		return arrayOrder;
	}
	
	public Class<?> getComponentType() {
		return componentType;
	}
	
	public int[] getDimensions() {
		return dimensions;
	}
	
	public int getRdataCompositeCode() {
		return rDataTypeCode.value | rDataStructureCode.value | rDataExceptionCode.value | rDataUserDefinedCode;
	}
	
	public RdataExceptionCode getRdataExceptionCode() {
		return rDataExceptionCode;
	}
	
	public RdataStructureCode getRdataStructureCode() {
		return rDataStructureCode;
	}
	
	public RdataTypeCode getRdataTypeCode() {
		return rDataTypeCode;
	}
	
	public int getRdataUserDefinedCode() {
		return rDataUserDefinedCode;
	}

	public boolean getValueBoolean() {
		return (boolean) value;
	}
	
	public boolean[] getValueBooleanArray1d() {
		return (boolean[]) value;
	}
	
	public byte getValueByte() {
		return (byte) value;
	}
	
	public byte[] getValueByteArray1d() {
		return (byte[]) value;
	}
	
	public double getValueDouble() {
		return (double) value;
	}
	
	public double[] getValueDoubleArray1d() {
		return (double[]) value;
	}
	
	public int getValueInt() {
		return (int) value;
	}
	
	public int[] getValueIntArray1d() {
		return (int[]) value;
	}
	
	public Object getValueObject() {
		return value;
	}
	
	public Object[] getValueObjectArray1d() {
		return (Object[]) value;
	}
	
	public String getValueString() {
		return value.toString();
	}

	public String[] getValueStringArray1d() {
		return (String[]) value;
	}
	
	public int initialize(Object value) {
		return this.initialize(value, ArrayOrder.ROW_MAJOR);
	}
	
	/*
	 * Map Java objects to intermediate values that can be easily converted to R
	 * objects.
	 * 
	 * This method is provided to allow re-initializing an existing JavaToR
	 * object as opposed to providing this functionality only via the
	 * constructor. This is used for performance optimization in R: creating new
	 * objects via rJava is expensive.
	 */
	public int initialize(Object value, ArrayOrder arrayOrder) {
		this.arrayOrder = arrayOrder;
		this.dimensions = null;
		this.isNamedListOfScalars = false; // Used to detect row major data frames.
		this.rDataExceptionCode = RdataExceptionCode.NONE;
		this.rDataTypeCode = RdataTypeCode.UNSUPPORTED;
		this.rDataStructureCode = RdataStructureCode.SCALAR;
		this.rDataUserDefinedCode = 0;
		this.value = value;
		
		/*
		 * Map Java null to R NULL.
		 */
		if (value == null) {
			this.rDataTypeCode = RdataTypeCode.NULL;
			this.rDataStructureCode = RdataStructureCode.SCALAR;
			return this.getRdataCompositeCode();
		}
		Class<?> cls = value.getClass();
		
		/*
		 * Map Java 1D and 2D arrays of simple types to R vectors and matrices.
		 * Arrays of other types and ragged arrays are converted to lists
		 * of appropriate R objects, if possible.
		 */
		if (cls.isArray()) {
			/*
			 * Get dimensions of array. If it is a ragged array (i.e. not
			 * rectangular), handle as a collection.
			 */
			this.dimensions = Utility.getRectangularArrayDimensions(value);
			if (this.dimensions == null) {
				this.value = Arrays.asList((Object[]) value);
				convertCollection();
				return this.getRdataCompositeCode();
			}
			if (this.dimensions.length == 1) {
				this.rDataStructureCode = RdataStructureCode.VECTOR;
			} else {
				this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
			}
			/*
			 * convertSimpleType sets this.value and this.rDataTypeCode. If
			 * this.rDataTypeCode == RdataTypeCode.UNSUPPORTED then it is not a
			 * simple type supported by R.
			 */
			convertSimpleStructure(Utility.getArrayBaseComponentType(cls));
			if (this.rDataTypeCode != RdataTypeCode.UNSUPPORTED)
				return this.getRdataCompositeCode();
			/*
			 * Handle array as a collection.
			 */
			this.dimensions = null;
			this.value = Arrays.asList((Object[]) value);
			convertCollection();
			return this.getRdataCompositeCode();
		}
		
		/*
		 * Map scalar values of simple types to R vectors of length one.
		 * 
		 * convertSimpleType sets this.value and this.rDataTypeCode. If
		 * this.rDataTypeCode == RdataTypeCode.UNSUPPORTED then it is not a
		 * simple type.
		 */
		this.rDataStructureCode = RdataStructureCode.SCALAR;
		convertSimpleStructure(cls);
		if (this.rDataTypeCode != RdataTypeCode.UNSUPPORTED)
			return this.getRdataCompositeCode();
		
		/*
		 * Attempt to map all other supported types to their respective R
		 * objects.
		 */
		if (Map.class.isAssignableFrom(cls)) {
			if (ScriptObjectMirror.class.isAssignableFrom(cls)) {
				convertScriptObjectMirror();
			} else {
				convertMap();
			}
			return this.getRdataCompositeCode();
		}
		if (Collection.class.isAssignableFrom(cls)) {
			convertCollection();
			return this.getRdataCompositeCode();
		}
		// Use getValueString() to retrieve error message.
		if (Throwable.class.isAssignableFrom(cls)) {
			this.rDataTypeCode = RdataTypeCode.OTHER;
			this.rDataExceptionCode = RdataExceptionCode.EXCEPTION;
			return this.getRdataCompositeCode();
		}
		
		/*
		 * At this point this.rDataTypeCode is RdataTypeCode.UNSUPPORTED.
		 * Throw an exception.
		 */
		throw new RuntimeException(String.format("Java class '%s' cannot be converted to an R object.", cls.getName()));
	}
	
	/*
	 * This method is provided to allow user-defined types to be set. Note that rDataUserDefinedCode
	 * must be between 0x01000000 and 0x7FFFFFFF because it will be combined with other values in
	 * getRdataCompositeCode(). 0x7FFFFFFF is used as the upper-bound to be compatible with R integer
	 * values.
	 */
	public int initialize(Object value, int rDataUserDefinedCode) {
		if (rDataUserDefinedCode < 0x01000000)
			throw new RuntimeException("User defined data codes are between 0x01000000 and 0x7FFFFFFF.");
		this.arrayOrder = ArrayOrder.ROW_MAJOR;
		this.dimensions = null;
		this.isNamedListOfScalars = false;
		this.rDataExceptionCode = RdataExceptionCode.NONE;
		this.rDataTypeCode = RdataTypeCode.OTHER;
		this.rDataStructureCode = RdataStructureCode.USER_DEFINED;
		this.rDataUserDefinedCode = rDataUserDefinedCode;
		this.value = value;
		return this.getRdataCompositeCode();
	}
	
	/*
	 * This method is provided to allow a shallow, in-place copy of the object
	 * to prevent creating new object references on the R side via rJava
	 * (an expensive operation).
	 */
	public int initializeFrom(JavaToR j2r) {
		this.arrayOrder = j2r.arrayOrder;
		this.dimensions = j2r.dimensions;
		this.isNamedListOfScalars = j2r.isNamedListOfScalars;
		this.rDataExceptionCode = j2r.rDataExceptionCode;
		this.rDataTypeCode = j2r.rDataTypeCode;
		this.rDataStructureCode = j2r.rDataStructureCode;
		this.rDataUserDefinedCode = j2r.rDataUserDefinedCode;
		this.value = j2r.value;
		return this.getRdataCompositeCode();
	}
	
	private boolean[] unboxArray1D(Boolean[] a) {
		if (a == null)
			return null;
		boolean[] b = new boolean[a.length];
		for (int i = 0; i < b.length; i++)
			if (a[i] == null) {
				b[i] = NA_ASSUMPTION_LOGICAL;
				this.rDataExceptionCode = RdataExceptionCode.WARNING_MISSING_LOGICAL_VALUES;
			} else {
				b[i] = a[i].booleanValue();
			}
		return b;
	}

	private byte[] unboxArray1D(Byte[] a) {
		if (a == null)
			return null;
		byte[] b = new byte[a.length];
		for (int i = 0; i < b.length; i++)
			if (a[i] == null) {
				b[i] = NA_ASSUMPTION_RAW;
				this.rDataExceptionCode = RdataExceptionCode.WARNING_MISSING_RAW_VALUES;
			} else {
				b[i] = a[i].byteValue();
			}
		return b;
	}

	private double[] unboxArray1D(Double[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private int[] unboxArray1D(Integer[] a) {
		if (a == null)
			return null;
		int[] b = new int[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_INT : a[i].intValue();
		return b;
	}

}
