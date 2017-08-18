package org.fgilbert.jdx.test;

import static org.junit.Assert.*;

import org.fgilbert.jdx.Utility;
import org.junit.Test;

public class UtilityTest {

	@Test
	public void testCreateNdimensionalArrayRowMajorJava() {
		int max; int[] dimensions; int[] data;
		
		/*
		 * This testing is weak. It is here mainly to step-through
		 * createNdimensionalArray* functions. The primary unit testing is done
		 * via R.
		 */
		
		max = 0;
		data = new int[max];
		for (int i = 0; i < max; i++)
			data[i] = i + 1;
		dimensions = new int[] {max};
		assertArrayEquals(data, (int[]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		max = 9;
		data = new int[max];
		for (int i = 0; i < max; i++)
			data[i] = i + 1;
		dimensions = new int[] {max};
		assertArrayEquals(data, (int[]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		data = new int[] {};
		dimensions = new int[] {1, 0};
		assertArrayEquals(new int[][] {{}}, (int[][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		data = new int[] {};
		dimensions = new int[] {2, 0};
		assertArrayEquals(new int[][] {{}, {}}, (int[][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		data = new int[] {1, 4, 7, 2, 5, 8, 3, 6, 9};
		dimensions = new int[] {3, 3};
		assertArrayEquals(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}, (int[][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		data = new int[] {1, 4, 2, 5, 3, 6};
		dimensions = new int[] {2, 3};
		assertArrayEquals(new int[][] {{1, 2, 3}, {4, 5, 6}}, (int[][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		data = new int[] {1, 3, 5, 2, 4, 6};
		dimensions = new int[] {3, 2};
		assertArrayEquals(new int[][] {{1, 2}, {3, 4}, {5, 6}}, (int[][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));

		data = new int[] {};
		dimensions = new int[] {2, 0, 0};
		assertArrayEquals(new int[][][] {{}, {}}, (int[][][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));
		
		data = new int[] {};
		dimensions = new int[] {2, 1, 0};
		assertArrayEquals(new int[][][] {{{}}, {{}}}, (int[][][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions));
		
		data = new int[] {1, 4, 2, 5, 3, 6, 7, 10, 8, 11, 9, 12, 13, 16, 14, 17, 15, 18};
		dimensions = new int[] {3, 2, 3};
		Object[] arrayExpected = new int[][][] {{{1, 2, 3}, {4, 5, 6}}, {{7, 8, 9}, {10, 11, 12}}, {{13, 14, 15}, {16, 17, 18}}};
		Object[] arrayActual = (int[][][]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions); 
		assertArrayEquals(arrayExpected, arrayActual);

		data = new int[] {1, 3, 5, 2, 4, 6, 7, 9, 11, 8, 10, 12, 13, 15, 17, 14, 16, 18};
		dimensions = new int[] {3, 3, 2};
		arrayExpected = new int[][][] {{{1, 2}, {3, 4}, {5, 6}}, {{7, 8}, {9, 10}, {11, 12}}, {{13, 14}, {15, 16}, {17, 18}}};
		arrayActual = (Object[]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions); 
		assertArrayEquals(arrayExpected, arrayActual);

		data = new int[] {1, 4, 2, 5, 3, 6, 7, 10, 8, 11, 9, 12, 13, 16, 14, 17, 15, 18, 1, 4, 2, 5, 3, 6, 7, 10, 8, 11, 9, 12, 13, 16, 14, 17, 15, 18};
		dimensions = new int[] {2, 3, 2, 3};
		arrayExpected = new int[][][][] {{{{1, 2, 3}, {4, 5, 6}}, {{7, 8, 9}, {10, 11, 12}}, {{13, 14, 15}, {16, 17, 18}}}, {{{1, 2, 3}, {4, 5, 6}}, {{7, 8, 9}, {10, 11, 12}}, {{13, 14, 15}, {16, 17, 18}}}};
		arrayActual = (Object[]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions); 
		assertArrayEquals(arrayExpected, arrayActual);

		data = new int[] {1, 3, 5, 2, 4, 6, 7, 9, 11, 8, 10, 12, 13, 15, 17, 14, 16, 18, 1, 3, 5, 2, 4, 6, 7, 9, 11, 8, 10, 12, 13, 15, 17, 14, 16, 18};
		dimensions = new int[] {2, 3, 3, 2};
		arrayExpected = new int[][][][] {{{{1, 2}, {3, 4}, {5, 6}}, {{7, 8}, {9, 10}, {11, 12}}, {{13, 14}, {15, 16}, {17, 18}}}, {{{1, 2}, {3, 4}, {5, 6}}, {{7, 8}, {9, 10}, {11, 12}}, {{13, 14}, {15, 16}, {17, 18}}}};
		arrayActual = (Object[]) Utility.createNdimensionalArrayRowMajorJava(data, dimensions); 
		assertArrayEquals(arrayExpected, arrayActual);

	}
	
	@Test
	public void testGetRectangularArrayDimensions() {
		assertEquals(null, Utility.getRectangularArrayDimensions(null));
		assertEquals(null, Utility.getRectangularArrayDimensions(1));
		assertArrayEquals(new int[1], Utility.getRectangularArrayDimensions(new int[0]));
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][][][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][][][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][0][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][0][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][0][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][0][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][1][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][1][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][1][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][1][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][2][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][2][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][2][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][0][2][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][0][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][0][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][0][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][0][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][1][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][1][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][1][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][1][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][2][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][2][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][2][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][1][2][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][0][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][0][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][0][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][0][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][1][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][1][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][1][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][1][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][2][]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][2][0]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][2][1]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[0][2][2][2]), new int[] {0, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][][][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][][]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][0][]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][0][0]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][0][1]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][0][2]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][1][]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][1][0]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][1][1]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][1][2]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][2][]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][2][0]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][2][1]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][0][2][2]), new int[] {1, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][0][]), new int[] {1, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][0][0]), new int[] {1, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][0][1]), new int[] {1, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][0][2]), new int[] {1, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][1][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][1][0]), new int[] {1, 1, 1, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][1][1]), new int[] {1, 1, 1, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][1][2]), new int[] {1, 1, 1, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][2][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][2][0]), new int[] {1, 1, 2, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][2][1]), new int[] {1, 1, 2, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][1][2][2]), new int[] {1, 1, 2, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][0][]), new int[] {1, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][0][0]), new int[] {1, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][0][1]), new int[] {1, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][0][2]), new int[] {1, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][1][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][1][0]), new int[] {1, 2, 1, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][1][1]), new int[] {1, 2, 1, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][1][2]), new int[] {1, 2, 1, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][2][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][2][0]), new int[] {1, 2, 2, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][2][1]), new int[] {1, 2, 2, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[1][2][2][2]), new int[] {1, 2, 2, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][][][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][][]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][0][]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][0][0]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][0][1]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][0][2]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][1][]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][1][0]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][1][1]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][1][2]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][2][]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][2][0]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][2][1]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][0][2][2]), new int[] {2, 0, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][0][]), new int[] {2, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][0][0]), new int[] {2, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][0][1]), new int[] {2, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][0][2]), new int[] {2, 1, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][1][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][1][0]), new int[] {2, 1, 1, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][1][1]), new int[] {2, 1, 1, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][1][2]), new int[] {2, 1, 1, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][2][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][2][0]), new int[] {2, 1, 2, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][2][1]), new int[] {2, 1, 2, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][1][2][2]), new int[] {2, 1, 2, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][0][]), new int[] {2, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][0][0]), new int[] {2, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][0][1]), new int[] {2, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][0][2]), new int[] {2, 2, 0, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][1][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][1][0]), new int[] {2, 2, 1, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][1][1]), new int[] {2, 2, 1, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][1][2]), new int[] {2, 2, 1, 2});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][2][]), null);
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][2][0]), new int[] {2, 2, 2, 0});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][2][1]), new int[] {2, 2, 2, 1});
		assertArrayEquals(Utility.getRectangularArrayDimensions(new int[2][2][2][2]), new int[] {2, 2, 2, 2});
		
		// Ragged arrays
		int[][] a = null;
		assertArrayEquals(Utility.getRectangularArrayDimensions(a), null);
		a = new int[][] {null, null};
		assertArrayEquals(Utility.getRectangularArrayDimensions(a), null);
		a = new int[][] {{0, 1}, null};
		assertArrayEquals(Utility.getRectangularArrayDimensions(a), null);
		a = new int[][] {null, {0, 1}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(a), null);
		int[][][] b = null;
		b = new int[][][] {null, null, null};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{}, null, null};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{}, {}, null};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{{1}, {2}, {3}}, {}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{{1}, {2}, {3}}, {{1}, {2}}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{{1}, {2}, {3}}, {{1}, {2}, {}}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{{1}, {2}, {}}, {{1}, {2}, {3}}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{{1}, {2}, {3}}, {{1}, {2}, {3, 3}}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		b = new int[][][] {{{1}, {2}, {3, 3}}, {{1}, {2}, {3}}};
		assertArrayEquals(Utility.getRectangularArrayDimensions(b), null);
		
	}

}
