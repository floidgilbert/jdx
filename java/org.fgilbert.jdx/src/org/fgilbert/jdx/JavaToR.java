package org.fgilbert.jdx;

/*
 * This class was written to optimize exchanging data between the JVM and R via rJava. 
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
		, COLUMN_MINOR
		, ROW_MAJOR
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
	 * This class is used to detect whether a collection represents an n-dimensional array.
	 */
	private class MaybeNdimensionalArray {

		private int[] subarrayDimensions;
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
			subarrayDimensions = j2r.getDimensions();
			typeCode = j2r.getRdataTypeCode();
		}
		
		int[] getSubarrayDimensions() {
			return subarrayDimensions;
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
				value = Arrays.equals(subarrayDimensions, j2r.getDimensions());
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
				} else if (typeCode == RdataTypeCode.INTEGER && j2r.getRdataTypeCode() == RdataTypeCode.RAW) {
					// Do nothing. Raw arrays will be coerced to integer.
				} else if (typeCode == RdataTypeCode.RAW && j2r.getRdataTypeCode() == RdataTypeCode.INTEGER) {
					// Revert type to integer. Raw arrays will be coerced to integer.
					typeCode = RdataTypeCode.INTEGER;
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
	 * `initialize` and `initializeFrom`. This is kludgy to be sure, but these
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
	
	private double[] coerceToDoubleArray1D(BigDecimal[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private double[] coerceToDoubleArray1D(BigInteger[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private double[] coerceToDoubleArray1D(float[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (double) a[i];
		return b;
	}

	private double[] coerceToDoubleArray1D(Float[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private double[] coerceToDoubleArray1D(long[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (double) a[i];
		return b;
	}

	private double[] coerceToDoubleArray1D(Long[] a) {
		if (a == null)
			return null;
		double[] b = new double[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_DOUBLE : a[i].doubleValue();
		return b;
	}

	private int[] coerceToIntegerArray1D(short[] a) {
		if (a == null)
			return null;
		int[] b = new int[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (int) a[i];
		return b;
	}

	private int[] coerceToIntegerArray1D(Short[] a) {
		if (a == null)
			return null;
		int[] b = new int[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? NA_INT : a[i].intValue();
		return b;
	}

	private String[] coerceToStringArray1D(char[] a) {
		if (a == null)
			return null;
		String[] b = new String[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = Character.toString(a[i]);
		return b;
	}

	private String[] coerceToStringArray1D(Character[] a) {
		if (a == null)
			return null;
		String[] b = new String[a.length];
		for (int i = 0; i < b.length; i++)
			b[i] = (a[i] == null) ? null : a[i].toString();
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
	 * converted to n-dimensional arrays if they contain similarly-typed,
	 * same-dimensional objects. Collections are converted to data frames if
	 * they contain homogeneous named lists (i.e. Java maps). All other
	 * combinations of objects/values will be converted to unnamed lists.
	 */
	private void convertCollection() {
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
		 * see if it can instead be converted to an n-dimensional array or data frame.
		 */
		Iterator<?> iter = col.iterator();
		Object o = iter.next();
		JavaToR j2r = new JavaToR(o, this.arrayOrder);
		if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
			this.rDataExceptionCode = j2r.rDataExceptionCode;
		int[] compositeTypes = new int[col.size()];
		compositeTypes[0] = j2r.getRdataCompositeCode();
		Object[] objects = new Object[col.size()];
		objects[0] = j2r.getValueObject();

		MaybeNdimensionalArray maybeNdimensionalArray = new MaybeNdimensionalArray(col.size(), j2r);
		MaybeRowMajorDataFrame maybeRowMajorDataFrame = new MaybeRowMajorDataFrame(j2r);
		for (int i = 1; i < compositeTypes.length; i++) {
			j2r = new JavaToR(iter.next(), this.arrayOrder);
			if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
				this.rDataExceptionCode = j2r.rDataExceptionCode;
			compositeTypes[i] = j2r.getRdataCompositeCode();
			objects[i] = j2r.getValueObject();
			if (maybeNdimensionalArray.getValue()) {
				maybeNdimensionalArray.update(j2r);
			} else if (maybeRowMajorDataFrame.getValue()) {
				maybeRowMajorDataFrame.update(j2r);
			}
		}
		if (maybeNdimensionalArray.getValue()) {
			if (maybeNdimensionalArray.getSubarrayDimensions().length == 1) {
				convertCollectionToArray2D(maybeNdimensionalArray, objects, compositeTypes);				
			} else {
				convertCollectionToArrayND(maybeNdimensionalArray, objects, compositeTypes);				
			}
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

	/* 
	 * This function is called only from within convertCollection. Mix of number data types are coerced to
	 * the most general type. 
	 */
	private void convertCollectionToArray2D(MaybeNdimensionalArray maybeNdimensionalArray, Object[] objects, int[] compositeTypes) {
		/*
		 * Set matrix dimensions.
		 */
		int subarrayLength = maybeNdimensionalArray.getSubarrayDimensions()[0];
		switch (this.arrayOrder) {
		case ROW_MAJOR:
		case COLUMN_MINOR:
			this.dimensions = new int[] {objects.length, subarrayLength};
			break;
		case COLUMN_MAJOR:
			this.dimensions = new int[] {subarrayLength, objects.length};
			break;
		}
		
		/*
		 * Initialize common variables used in conversion.
		 */
		Object flatArray = null;
		int flatArrayLength = objects.length * subarrayLength;
		int dataTypeCodeInt;
		int[] subarrayInt; double[] subarrayDouble; byte[] subarrayByte;
		boolean[] subarrayBoolean; String[] subarrayString;
		
		/*
		 * Convert collection based on the target matrix type.
		 */
		switch (maybeNdimensionalArray.getTypeCode()) {
		case NUMERIC:
			double[] flatArrayDouble = new double[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
			case COLUMN_MINOR:
				int rows = objects.length;
				for (int i = 0; i < rows; i++) {
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.NUMERIC.value) {
						subarrayDouble = (double[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayDouble[i + j * rows] = subarrayDouble[j];
					} else if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						subarrayInt = (int[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayDouble[i + j * rows] = (double) subarrayInt[j];
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						subarrayByte = (byte[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayDouble[i + j * rows] = (double) subarrayByte[j];
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to a numeric matrix.", dataTypeCodeInt));
					}
				}
				break;
			case COLUMN_MAJOR:
				int flatArrayIndex = 0;
				for (int i = 0; i < objects.length; i++) {
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.NUMERIC.value) {
						subarrayDouble = (double[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayDouble[flatArrayIndex++] = subarrayDouble[j];
					} else if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						subarrayInt = (int[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayDouble[flatArrayIndex++] = (double) subarrayInt[j];
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						subarrayByte = (byte[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayDouble[flatArrayIndex++] = (double) subarrayByte[j];
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to a numeric matrix.", dataTypeCodeInt));
					}
				}
				break;
			}
			flatArray = flatArrayDouble;
			break;
		case INTEGER:
			int[] flatArrayInt = new int[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
			case COLUMN_MINOR:
				int rows = objects.length;
				for (int i = 0; i < rows; i++) {
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						subarrayInt = (int[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayInt[i + j * rows] = subarrayInt[j];
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						subarrayByte = (byte[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayInt[i + j * rows] = (int) subarrayByte[j];
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to an integer matrix.", dataTypeCodeInt));
					}
				}
				break;
			case COLUMN_MAJOR:
				int flatArrayIndex = 0;
				for (int i = 0; i < objects.length; i++) {
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						subarrayInt = (int[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayInt[flatArrayIndex++] = subarrayInt[j];
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						subarrayByte = (byte[]) objects[i];
						for (int j = 0; j < subarrayLength; j++)
							flatArrayInt[flatArrayIndex++] = (int) subarrayByte[j];
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to an integer matrix.", dataTypeCodeInt));
					}
				}
				break;
			}
			flatArray = flatArrayInt;
			break;
		case CHARACTER:
			String[] flatArrayString = new String[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
			case COLUMN_MINOR:
				int rows = objects.length;
				for (int i = 0; i < rows; i++) {
					subarrayString = (String[]) objects[i];
					for (int j = 0; j < subarrayLength; j++)
						flatArrayString[i + j * rows] = subarrayString[j];
				}
				break;
			case COLUMN_MAJOR:
				int flatArrayIndex = 0;
				for (int i = 0; i < objects.length; i++) {
					subarrayString = (String[]) objects[i];
					for (int j = 0; j < subarrayLength; j++)
						flatArrayString[flatArrayIndex++] = subarrayString[j];
				}
				break;
			}
			flatArray = flatArrayString;
			break;
		case LOGICAL:
			boolean[] flatArrayBoolean = new boolean[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
			case COLUMN_MINOR:
				int rows = objects.length;
				for (int i = 0; i < rows; i++) {
					subarrayBoolean = (boolean[]) objects[i];
					for (int j = 0; j < subarrayLength; j++)
						flatArrayBoolean[i + j * rows] = subarrayBoolean[j];
				}
				break;
			case COLUMN_MAJOR:
				int flatArrayIndex = 0;
				for (int i = 0; i < objects.length; i++) {
					subarrayBoolean = (boolean[]) objects[i];
					for (int j = 0; j < subarrayLength; j++)
						flatArrayBoolean[flatArrayIndex++] = subarrayBoolean[j];
				}
				break;
			}
			flatArray = flatArrayBoolean;
			break;
		case RAW:
			byte[] flatArrayByte = new byte[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
			case COLUMN_MINOR:
				int rows = objects.length;
				for (int i = 0; i < rows; i++) {
					subarrayByte = (byte[]) objects[i];
					for (int j = 0; j < subarrayLength; j++)
						flatArrayByte[i + j * rows] = subarrayByte[j];
				}
				break;
			case COLUMN_MAJOR:
				int flatArrayIndex = 0;
				for (int i = 0; i < objects.length; i++) {
					subarrayByte = (byte[]) objects[i];
					for (int j = 0; j < subarrayLength; j++)
						flatArrayByte[flatArrayIndex++] = subarrayByte[j];
				}
				break;
			}
			flatArray = flatArrayByte;
			break;
		default:
			throw new RuntimeException(String.format("The R data type code %s is unsupported when converting collections to matrices.", maybeNdimensionalArray.getTypeCode()));
		}
		this.value = new Object[] {this.dimensions, flatArray};
		this.rDataTypeCode = maybeNdimensionalArray.getTypeCode();
		this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
		return;
	}
	
	/*
	 * This function is called only from within convertCollection when
	 * dimensions > 2. Mix of number data types are coerced to the most general
	 * type.
	 */
	private void convertCollectionToArrayND(MaybeNdimensionalArray maybeNdimensionalArray, Object[] objects, int[] compositeTypes) {
		/*
		 * Set matrix dimensions. R dimensions are always [row, column, matrix, cube, ...]
		 */
		int[] subarrayDimensions = maybeNdimensionalArray.getSubarrayDimensions();
		switch (this.arrayOrder) {
		case ROW_MAJOR:
			this.dimensions = new int[subarrayDimensions.length + 1];
			this.dimensions[0] = objects.length;
			for (int i = 0; i < subarrayDimensions.length; i++)
				this.dimensions[i + 1] = subarrayDimensions[i]; 
			break;
		case COLUMN_MINOR:
		case COLUMN_MAJOR:
			this.dimensions = Arrays.copyOf(subarrayDimensions, subarrayDimensions.length + 1);
			this.dimensions[this.dimensions.length - 1] = objects.length;
			break;
		}
		
		/*
		 * Initialize common variables used in conversion.
		 */
		Object flatArray = null;
		int flatArrayIndex = 0;
		int flatArrayLength = objects.length;
		for (int i = 0; i < subarrayDimensions.length; i++)
			flatArrayLength *= subarrayDimensions[i];
		int dataTypeCodeInt;
		int[] arrayDataInt; double[] arrayDataDouble; byte[] arrayDataByte;
		boolean[] arrayDataBoolean; String[] arrayDataString;
		
		/*
		 * Convert collection based on the target data type.
		 */
		switch (maybeNdimensionalArray.getTypeCode()) {
		case NUMERIC:
			double[] flatArrayDouble = new double[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.NUMERIC.value) {
						arrayDataDouble = (double[]) ndObject[1];
						for (int j = 0; j < arrayDataDouble.length; j++)
							flatArrayDouble[flatArrayIndex + j * objects.length] = arrayDataDouble[j];
						flatArrayIndex++;
					} else if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						arrayDataInt = (int[]) ndObject[1];
						for (int j = 0; j < arrayDataInt.length; j++)
							flatArrayDouble[flatArrayIndex + j * objects.length] = (double) arrayDataInt[j];
						flatArrayIndex++;
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						arrayDataByte = (byte[]) ndObject[1];
						for (int j = 0; j < arrayDataByte.length; j++)
							flatArrayDouble[flatArrayIndex + j * objects.length] = (double) arrayDataByte[j];
						flatArrayIndex++;
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to a numeric n-dimensional array.", dataTypeCodeInt));
					}
				}
				break;
			case COLUMN_MAJOR:
			case COLUMN_MINOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.NUMERIC.value) {
						arrayDataDouble = (double[]) ndObject[1];
						for (int j = 0; j < arrayDataDouble.length; j++)
							flatArrayDouble[flatArrayIndex++] = arrayDataDouble[j];
					} else if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						arrayDataInt = (int[]) ndObject[1];
						for (int j = 0; j < arrayDataInt.length; j++)
							flatArrayDouble[flatArrayIndex++] = (double) arrayDataInt[j];
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						arrayDataByte = (byte[]) ndObject[1];
						for (int j = 0; j < arrayDataByte.length; j++)
							flatArrayDouble[flatArrayIndex++] = (double) arrayDataByte[j];
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to a numeric n-dimensional array.", dataTypeCodeInt));
					}
				}
				break;
			}
			flatArray = flatArrayDouble;
			break;
		case INTEGER:
			int[] flatArrayInt = new int[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						arrayDataInt = (int[]) ndObject[1];
						for (int j = 0; j < arrayDataInt.length; j++)
							flatArrayInt[flatArrayIndex + j * objects.length] = arrayDataInt[j];
						flatArrayIndex++;
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						arrayDataByte = (byte[]) ndObject[1];
						for (int j = 0; j < arrayDataByte.length; j++)
							flatArrayInt[flatArrayIndex + j * objects.length] = (int) arrayDataByte[j];
						flatArrayIndex++;
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to an integer n-dimensional array.", dataTypeCodeInt));
					}
				}
				break;
			case COLUMN_MAJOR:
			case COLUMN_MINOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					dataTypeCodeInt = compositeTypes[i] & 0xFF;
					if (dataTypeCodeInt == RdataTypeCode.INTEGER.value) {
						arrayDataInt = (int[]) ndObject[1];
						for (int j = 0; j < arrayDataInt.length; j++)
							flatArrayInt[flatArrayIndex++] = arrayDataInt[j];
					} else if (dataTypeCodeInt == RdataTypeCode.RAW.value) {
						arrayDataByte = (byte[]) ndObject[1];
						for (int j = 0; j < arrayDataByte.length; j++)
							flatArrayInt[flatArrayIndex++] = (int) arrayDataByte[j];
					} else {
						throw new RuntimeException(String.format("The R data type code 0x%X is unsupported when converting a collection of arrays to an integer n-dimensional array.", dataTypeCodeInt));
					}
				}
				break;
			}
			flatArray = flatArrayInt;
			break;
		case CHARACTER:
			String[] flatArrayString = new String[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					arrayDataString = (String[]) ndObject[1];
					for (int j = 0; j < arrayDataString.length; j++)
						flatArrayString[flatArrayIndex + j * objects.length] = arrayDataString[j];
					flatArrayIndex++;
				}
				break;
			case COLUMN_MAJOR:
			case COLUMN_MINOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					arrayDataString = (String[]) ndObject[1];
					for (int j = 0; j < arrayDataString.length; j++)
						flatArrayString[flatArrayIndex++] = arrayDataString[j];
				}
				break;
			}
			flatArray = flatArrayString;
			break;
		case LOGICAL:
			boolean[] flatArrayBoolean = new boolean[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					arrayDataBoolean = (boolean[]) ndObject[1];
					for (int j = 0; j < arrayDataBoolean.length; j++)
						flatArrayBoolean[flatArrayIndex + j * objects.length] = arrayDataBoolean[j];
					flatArrayIndex++;
				}
				break;
			case COLUMN_MAJOR:
			case COLUMN_MINOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					arrayDataBoolean = (boolean[]) ndObject[1];
					for (int j = 0; j < arrayDataBoolean.length; j++)
						flatArrayBoolean[flatArrayIndex++] = arrayDataBoolean[j];
				}
				break;
			}
			flatArray = flatArrayBoolean;
			break;
		case RAW:
			byte[] flatArrayByte = new byte[flatArrayLength];
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					arrayDataByte = (byte[]) ndObject[1];
					for (int j = 0; j < arrayDataByte.length; j++)
						flatArrayByte[flatArrayIndex + j * objects.length] = arrayDataByte[j];
					flatArrayIndex++;
				}
				break;
			case COLUMN_MAJOR:
			case COLUMN_MINOR:
				for (int i = 0; i < objects.length; i++) {
					Object[] ndObject = (Object[]) objects[i];
					arrayDataByte = (byte[]) ndObject[1];
					for (int j = 0; j < arrayDataByte.length; j++)
						flatArrayByte[flatArrayIndex++] = arrayDataByte[j];
				}
				break;
			}
			flatArray = flatArrayByte;
			break;
		default:
			throw new RuntimeException(String.format("The R data type code %s is unsupported when converting collections to n-dimensional arrays.", maybeNdimensionalArray.getTypeCode()));
		}
		
		this.value = new Object[] {this.dimensions, flatArray};
		this.rDataTypeCode = maybeNdimensionalArray.getTypeCode();
		this.rDataStructureCode = RdataStructureCode.ND_ARRAY;
		return;
	}
	
	/*
	 * This function is called only from within convertCollection.
	 * 
	 * Converts a collection of named lists (i.e. rows or records) to a data
	 * frame. The data is assumed to be validated ahead of time.
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
		int flatArrayLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatArrayLength *= this.dimensions[i];
		boolean[] flatArray = new boolean[flatArrayLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatArrayLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for COLUMN_MINOR
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
			if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
				this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
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
			case COLUMN_MINOR:
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
		case COLUMN_MINOR:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalByteArray() {
		int flatArrayLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatArrayLength *= this.dimensions[i];
		byte[] flatArray = new byte[flatArrayLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatArrayLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for COLUMN_MINOR
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
			if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
				this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
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
			case COLUMN_MINOR:
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
		case COLUMN_MINOR:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalDoubleArray() {
		int flatArrayLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatArrayLength *= this.dimensions[i];
		double[] flatArray = new double[flatArrayLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatArrayLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for COLUMN_MINOR
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
			if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
				this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
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
			case COLUMN_MINOR:
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
		case COLUMN_MINOR:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalIntArray() {
		int flatArrayLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatArrayLength *= this.dimensions[i];
		int[] flatArray = new int[flatArrayLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatArrayLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for COLUMN_MINOR
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
			if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
				this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
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
			case COLUMN_MINOR:
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
		case COLUMN_MINOR:
			int swap = this.dimensions[this.dimensions.length - 1];
			this.dimensions[this.dimensions.length - 1] = this.dimensions[this.dimensions.length - 2];
			this.dimensions[this.dimensions.length - 2] = swap;
			Utility.reverseArray(this.dimensions);
			break;
		}
		this.value = new Object[] {this.dimensions, flatArray};
	}
	
	private void convertNdimensionalStringArray() {
		int flatArrayLength = this.dimensions[0];
		for (int i = 1; i < this.dimensions.length; i++)
			flatArrayLength *= this.dimensions[i];
		String[] flatArray = new String[flatArrayLength];
		int currentFlatArrayIndex = 0;
		int subarrayLength = this.dimensions[this.dimensions.length - 1];
		int subarrayCount = 0;
		if (subarrayLength != 0)
			subarrayCount = flatArrayLength / subarrayLength;
		int[] currentSubarrayIndex = new int[this.dimensions.length - 1];
		// These three variables are used only for COLUMN_MINOR
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
			if (j2r.rDataExceptionCode != RdataExceptionCode.NONE)
				this.rDataExceptionCode = j2r.rDataExceptionCode;
			switch (this.arrayOrder) {
			case ROW_MAJOR:
				for (int j = 0; j < subarrayLength; j++)
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
			case COLUMN_MINOR:
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
		case COLUMN_MINOR:
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
	 * is not a scalar (e.g. native JavaScript arrays). The JS arrays are
	 * converted to vectors, n-dimensional arrays, or lists via
	 * convertCollection().
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
					this.value = coerceToDoubleArray1D((float[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Long.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToDoubleArray1D((long[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Short.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.INTEGER;
				if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToIntegerArray1D((short[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalIntArray();
				return;
			}
			if (cls.equals(Character.TYPE)) {
				this.rDataTypeCode = RdataTypeCode.CHARACTER;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = Character.toString((char) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToStringArray1D((char[]) this.value);
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
					this.value = coerceToDoubleArray1D((Float[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Long.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((Long) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToDoubleArray1D((Long[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(Short.class)) {
				this.rDataTypeCode = RdataTypeCode.INTEGER;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((Short) this.value).intValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToIntegerArray1D((Short[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalIntArray();
				return;
			}
			if (cls.equals(BigDecimal.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((BigDecimal) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToDoubleArray1D((BigDecimal[]) this.value);
				else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
					convertNdimensionalDoubleArray();
				return;
			}
			if (cls.equals(BigInteger.class)) {
				this.rDataTypeCode = RdataTypeCode.NUMERIC;
				if (this.rDataStructureCode == RdataStructureCode.SCALAR)
					this.value = ((BigInteger) this.value).doubleValue();
				else if (this.rDataStructureCode == RdataStructureCode.VECTOR)
					this.value = coerceToDoubleArray1D((BigInteger[]) this.value);
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
				this.value = coerceToStringArray1D((Character[]) this.value);
			else if (this.rDataStructureCode == RdataStructureCode.ND_ARRAY)
				convertNdimensionalStringArray();
			return;
		}
		this.rDataTypeCode = RdataTypeCode.UNSUPPORTED;
	}
	
	public ArrayOrder getArrayOrder() {
		return arrayOrder;
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
	 * constructor. This is used for performance optimization in R; creating new
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
		 * Map Java n-dimensional arrays of simple types to R arrays. Ragged
		 * arrays and arrays of non-simple types are converted to lists of
		 * appropriate R objects, if possible.
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
