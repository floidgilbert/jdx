package org.fgilbert.jdx;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utility {

	/*
	 * IMPORTANT: This method does not validate the incoming data.
	 */
	public static List<Object> createList(Object[] objects) {
		List<Object> lst = new ArrayList<Object>(objects.length);
		for (int i = 0; i < objects.length; i++) {
			lst.add(objects[i]);
		}
		return lst;
	}

	/*
	 * Used to convert an R data frame to a list of records (a row-major
	 * structure). IMPORTANT: This method does not validate the incoming data.
	 */
	public static List<Map<String, Object>> createListOfRecords(String[] columnNames, Object[] columns) {
		int rows = 0;
		if (columns.length > 0)
			rows = Array.getLength(columns[0]);
		
		List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>(rows);
		for (int i = 0; i < rows; i++) {
			lst.add(new LinkedHashMap<String, Object>(columns.length));
		}

		for (int i = 0; i < columns.length; i++) {
			Class<?> ct = columns[i].getClass().getComponentType();
			String name = columnNames[i];
			if (ct.equals(Double.TYPE)) {
				double[] a = (double[]) columns[i];
				for (int j = 0; j < rows; j++) {
					lst.get(j).put(name, a[j]);
				}
			} else if (ct.equals(String.class)) {
				String[] a = (String[]) columns[i];
				for (int j = 0; j < rows; j++) {
					lst.get(j).put(name, a[j]);
				}
			} else if (ct.equals(Integer.TYPE)) {
				int[] a = (int[]) columns[i];
				for (int j = 0; j < rows; j++) {
					lst.get(j).put(name, a[j]);
				}
			} else if (ct.equals(Boolean.TYPE)) {
				boolean[] a = (boolean[]) columns[i];
				for (int j = 0; j < rows; j++) {
					lst.get(j).put(name, a[j]);
				}
			} else if (ct.equals(Byte.TYPE)) {
				byte[] a = (byte[]) columns[i];
				for (int j = 0; j < rows; j++) {
					lst.get(j).put(name, a[j]);
				}
			} else {
				throw new RuntimeException(String.format("'%s' is not a supported column type.", ct.getName()));
			}
		}
		return lst;
	}

	/*
	 * IMPORTANT: This method does not validate the incoming data.
	 */
	public static Map<String, Object> createMap(String[] names, Object[] objects) {
		Map<String, Object> m = new LinkedHashMap<String, Object>(names.length, 1);
		for (int i = 0; i < names.length; i++) {
			m.put(names[i], objects[i]);
		}
		return m;
	}
	
	/*
	 * Creates an n-dimensional array from a 1-dimensional array (vector) where
	 * the right-most index changes the fastest. In a three-dimensional
	 * structure, this would be [matrix, column, row].
	 * 
	 * IMPORTANT: This method does not validate the incoming data.
	 */
	///test both/all n-dim array functions with single dimensional array
	///create methods for the other data types after testing
	public static Object createNdimensionalArrayColumnMajor(int[] data, int[] dimensions) {
		if (dimensions.length == 1)
			return Arrays.copyOfRange(data, 0, dimensions[0]);
		Object array = (Object[]) Array.newInstance(int.class, dimensions);
		int subarrayCount = dimensions[0];
		for (int i = 1; i < dimensions.length - 1; i++)
			subarrayCount *= dimensions[i];
		int subarrayLength = dimensions[dimensions.length - 1];
		int currentDataIndex = 0;
		int[] currentSubarrayIndex = new int[dimensions.length - 1];
		for (int i = 0; i < subarrayCount; i++) {
			Object o = array;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			int[] n = (int[]) Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]);			
			for (int j = 0; j < subarrayLength; j++)
				n[j] = data[currentDataIndex++];
			for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
				if (currentSubarrayIndex[j] < dimensions[j] - 1) {
					currentSubarrayIndex[j]++;
					break;
				}
				currentSubarrayIndex[j] = 0;
			}
		}
		return array;
	}
	
	/*
	 * Creates an n-dimensional array from a 1-dimensional array (vector) using
	 * R's indexing scheme: the left-most index changes the fastest. In a
	 * three-dimensional structure, this is [row, column, matrix].
	 * 
	 * IMPORTANT: This method does not validate the incoming data.
	 */
	///test both/all n-dim array functions with single dimensional array
	///create methods for the other data types after testing
	public static Object createNdimensionalArrayRowMajor(int[] data, int[] dimensions) {
		if (dimensions.length == 1)
			return Arrays.copyOfRange(data, 0, dimensions[0]);
		Object array = (Object[]) Array.newInstance(int.class, dimensions);
		int subarrayCount = dimensions[0];
		for (int i = 1; i < dimensions.length - 1; i++)
			subarrayCount *= dimensions[i];
		int subarrayLength = dimensions[dimensions.length - 1];
		int currentDataIndex = 0;
		int[] currentSubarrayIndex = new int[dimensions.length - 1];
		for (int i = 0; i < subarrayCount; i++) {
			Object o = array;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			int[] n = (int[]) Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]);			
			for (int j = 0; j < subarrayLength; j++)
				/*
				 *  The offset is the same as subarrayCount because of the way the data structure fills
				 *  to mimic R's indexing scheme. Step through it to see how it works.
				 */
				n[j] = data[currentDataIndex + subarrayCount * j];
			currentDataIndex++; 
			for (int j = 0; j < currentSubarrayIndex.length; j++) {
				if (currentSubarrayIndex[j] < dimensions[j] - 1) {
					currentSubarrayIndex[j]++;
					break;
				}
				currentSubarrayIndex[j] = 0;
			}
		}
		return array;
	}
	
	/*
	 * Creates an n-dimensional array from a 1-dimensional array (vector)
	 * similar to the column-major scheme except that row and column indices are
	 * switched at the matrix level. In a three-dimensional setting, this is
	 * [matrix, row, column]. This would be the row-major structure familiar to
	 * most Java programmers.
	 * 
	 * IMPORTANT: This method does not validate the incoming data.
	 */
	///test both/all n-dim array functions with single dimensional array
	///create methods for the other data types after testing
	public static Object createNdimensionalArrayRowMajorJava(int[] data, int[] dimensions) {
		if (dimensions.length == 1)
			return Arrays.copyOfRange(data, 0, dimensions[0]);
		Object array = (Object[]) Array.newInstance(int.class, dimensions);
		int subarrayCount = dimensions[0];
		for (int i = 1; i < dimensions.length - 1; i++)
			subarrayCount *= dimensions[i];
		int rowCount = dimensions[dimensions.length - 2];
		int columnCount = dimensions[dimensions.length - 1];
///		int dataOffset = rowCount * columnCount;
		int currentDataIndex = 0;
		int[] currentSubarrayIndex = new int[dimensions.length - 1];
		for (int i = 0; i < subarrayCount; i++) {
			Object o = array;
			for (int j = 0; j < currentSubarrayIndex.length - 1; j++)
				o = Array.get(o, currentSubarrayIndex[j]);
			int[] n = (int[]) Array.get(o, currentSubarrayIndex[currentSubarrayIndex.length - 1]);			
			for (int j = 0; j < columnCount; j++)
				n[j] = data[currentDataIndex + rowCount * j];
			if ((i + 1) % rowCount != 0) {
				currentDataIndex++;
			} else {
				currentDataIndex = currentDataIndex - rowCount + rowCount * columnCount;
			}
			for (int j = currentSubarrayIndex.length - 1; j > -1; j--) {
				if (currentSubarrayIndex[j] < dimensions[j] - 1) {
					currentSubarrayIndex[j]++;
					break;
				}
				currentSubarrayIndex[j] = 0;
			}
		}
		return array;
	}
	
	/*
	 * Iterates through all dimensions and returns the base component type of an
	 * array class. Returns null if `cls` does not represent an array.
	 */
	public static Class<?> getArrayBaseComponentType(Class<?> cls) {
		if (!cls.isArray())
			return null;
		Class<?> ct = cls.getComponentType();
		while (ct.getComponentType() != null) {
			ct = ct.getComponentType();
		}
		return ct;
	}
	
	public static int getArrayDimensionCount(Class<?> cls) {
		if (!cls.isArray())
			return 0;
		int dimensions = 1;
		Class<?> ct = cls.getComponentType();
		while (ct.getComponentType() != null) {
			dimensions++;
			ct = ct.getComponentType();
		}
		return dimensions;
	}
	
	/*
	 * Returns dimensions of a rectangular array. If not a rectangular array
	 * (i.e., if it's not an array or if it's a ragged array), the function
	 * returns null. Note that int[0][] is an empty array, and so it is
	 * considered rectangular. However, int[1][] is considered ragged because it
	 * has a null subarray. The dimensions of int[0][0], int[0][1], int[0][] are
	 * all [0, 0].
	 */
	public static int[] getRectangularArrayDimensions(Object array) {
		if (array == null)
			return null;
		/*
		 * It is necessary to get the number of dimensions from the array class
		 * because sometimes it is not possible to do so by traversing the array
		 * itself (e.g. int[2][][]).
		 */
		int dimensionCount = getArrayDimensionCount(array.getClass());
		if (dimensionCount == 0) // Not an array
			return null;
		int[] dimensions = new int[dimensionCount]; 
		/*
		 * Traverse one line of the array to get dimensions.
		 */
		Object o = array;
		for (int i = 0; i < dimensionCount; i++) {
			int length = Array.getLength(o);
			if (length == 0) // All of the remaining dimensions are automatically zero-length.
				break;
			dimensions[i] = length; 
			o = Array.get(o, 0); // Retrieve the first element of current array.
			if (o == null)
				break;
		}
		
		/*
		 * Compare the rest of the lines to see if they have the same
		 * dimensions.
		 */
		if (!isArrayRectangular(array, dimensions, 0))
			return null;
		return dimensions;
	}
	
	/*
	 * Returns false if a ragged array. int[0][] is considered rectangular
	 * because it is an empty array, but int[1][] is considered ragged because
	 * it contains null arrays. int[0][1] is equivalent to int[0]...
	 * 
	 * IMPORTANT: This method does not validate parameters.
	 */
	private static boolean isArrayRectangular(Object array, int[] dimensions, int dimensionIndex) {
		if (dimensionIndex >= dimensions.length - 1)
			return true;
		int subarrayLength = dimensions[dimensionIndex + 1];
		for (int i = 0; i < dimensions[dimensionIndex]; i++) {
			Object subarray = Array.get(array, i);
			if (subarray == null || Array.getLength(subarray) != subarrayLength)
				return false;
			if (dimensionIndex + 1 < dimensions.length - 1) {
				if (!isArrayRectangular(subarray, dimensions, dimensionIndex + 1))
					return false;
			}
		}
		return true;
	}
	
}
