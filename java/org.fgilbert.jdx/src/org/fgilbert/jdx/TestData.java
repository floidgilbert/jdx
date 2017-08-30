package org.fgilbert.jdx;

/*
 * This class provides data for testing Java types and structures. It is used only for testing in the jsr223 project.
 */

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestData {

	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// CONSTANTS
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static double getInfinityNegative() {
		return Double.NEGATIVE_INFINITY;
	}
	
	public static double getInfinityPositive() {
		return Double.POSITIVE_INFINITY;
	}
	
	public static double getNaN() {
		return Double.NaN;
	}
	
	public static Object getNull() {
		return null;
	}
	
	public static boolean getFalse() {
		return false;
	}

	public static boolean getTrue() {
		return true;
	}

	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// PRIMITIVE SCALARS
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static boolean getBooleanMin() {
		return false;
	}
	
	public static boolean getBooleanMax() {
		return true;
	}
	
	
	
	public static byte getByteMin() {
		return Byte.MIN_VALUE;
	}
	
	public static byte getByteMax() {
		return Byte.MAX_VALUE;
	}
	
	
	
	public static char getCharacterLow(int codePoint) {
		return Character.toChars(codePoint)[0];
	}
	
	public static char getCharacterHigh(int codePoint) {
		char[] c = Character.toChars(codePoint);
		if (c.length == 1)
			return '\u0000';
		return c[1];
	}
	
	public static char getCharacterMin() {
		return Character.MIN_VALUE;
	}
	
	public static char getCharacterMax() {
		return Character.MAX_VALUE;
	}
	
	
	
	public static double getDoubleMin() {
		return Double.MIN_NORMAL;
	}
	
	public static double getDoubleMax() {
		return Double.MAX_VALUE;
	}
	
	
	
	public static float getFloatMin() {
		return Float.MIN_VALUE;
	}
	
	public static float getFloatMax() {
		return Float.MAX_VALUE;
	}
	
	
	
	public static int getIntMin() {
		return Integer.MIN_VALUE;
	}

	public static int getIntMax() {
		return Integer.MAX_VALUE;
	}
	
	
	
	public static long getLongMin() {
		return Long.MIN_VALUE;
	}
	
	public static long getLongMax() {
		return Long.MAX_VALUE;
	}
	
	
	
	public static short getShortMin() {
		return Short.MIN_VALUE;
	}
	
	public static short getShortMax() {
		return Short.MAX_VALUE;
	}
	
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// BOXED SCALARS
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	// Intended to overflow Double
	public static BigDecimal getBigDecimalLarge() {
		BigDecimal v = new BigDecimal(Long.MAX_VALUE);
		return new BigDecimal(v.pow(21).toString());
	}
	
	// Intended to overflow Double
	public static BigDecimal getBigDecimalSmall() {
		BigDecimal v = new BigDecimal(Long.MAX_VALUE * -1);
		return new BigDecimal(v.pow(21).toString());
	}
	
	public static BigDecimal getBigDecimalTen() {
		return BigDecimal.TEN;
	}
	
	public static BigDecimal getBigDecimalZero() {
		return BigDecimal.ZERO;
	}
	
	
	
	// Intended to overflow Double
	public static BigInteger getBigIntegerLarge() {
		return getBigDecimalLarge().toBigInteger();
	}
	
	// Intended to overflow Double
	public static BigInteger getBigIntegerSmall() {
		return getBigDecimalSmall().toBigInteger();
	}
	
	public static BigInteger getBigIntegerTen() {
		return BigInteger.TEN;
	}
	
	public static BigInteger getBigIntegerZero() {
		return BigInteger.ZERO;
	}
	
	
	
	public static Boolean getBoxedBooleanMin() {
		return false;
	}
	
	public static Boolean getBoxedBooleanMax() {
		return true;
	}
	
	
	
	public static Byte getBoxedByteMin() {
		return Byte.MIN_VALUE;
	}
	
	public static Byte getBoxedByteMax() {
		return Byte.MAX_VALUE;
	}
	
	
	
	public static Character getBoxedCharacterLow(int codePoint) {
		return Character.toChars(codePoint)[0];
	}
	
	public static Character getBoxedCharacterHigh(int codePoint) {
		char[] c = Character.toChars(codePoint);
		if (c.length == 1)
			return '\u0000';
		return c[1];
	}
	
	public static Character getBoxedCharacterMin() {
		return Character.MIN_VALUE;
	}
	
	public static Character getBoxedCharacterMax() {
		return Character.MAX_VALUE;
	}
	
	
	
	public static Double getBoxedDoubleMin() {
		return Double.MIN_NORMAL;
	}
	
	public static Double getBoxedDoubleMax() {
		return Double.MAX_VALUE;
	}
	
	
	
	public static Float getBoxedFloatMin() {
		return Float.MIN_VALUE;
	}
	
	public static Float getBoxedFloatMax() {
		return Float.MAX_VALUE;
	}
	
	
	
	public static Integer getBoxedIntegerMin() {
		return Integer.MIN_VALUE;
	}
	
	public static Integer getBoxedIntegerMax() {
		return Integer.MAX_VALUE;
	}
	
	
	
	public static Long getBoxedLongMin() {
		return Long.MIN_VALUE;
	}
	
	public static Long getBoxedLongMax() {
		return Long.MAX_VALUE;
	}
	
	
	
	public static Short getBoxedShortMin() {
		return Short.MIN_VALUE;
	}
	
	public static Short getBoxedShortMax() {
		return Short.MAX_VALUE;
	}
	
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// STRING SCALARS
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static String getStringAlphabetLower() {
		return "abcdefghijklmnopqrstuvwxyz";
	}
	
	public static String getStringEmpty() {
		return "";
	}
	
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// PRIMITIVE ARRAYS 1D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static boolean[] getBooleanArray1d0x0() {
		return new boolean[] {};
	}
	
	public static boolean[] getBooleanArray1d1x1() {
		return new boolean[] {true};
	}
	
	public static boolean[] getBooleanArray1d1x2() {
		return new boolean[] {false, true};
	}
	
	
	
	public static byte[] getByteArray1d0x0() {
		return new byte[] {};
	}
	
	public static byte[] getByteArray1d1x1() {
		return new byte[] {Byte.MIN_VALUE};
	}
	
	public static byte[] getByteArray1dLowZeroHigh() {
		return new byte[] {Byte.MIN_VALUE, 0, Byte.MAX_VALUE};
	}
	
	
	
	public static char[] getCharacterArray1d0x0() {
		return new char[] {};
	}
	
	public static char[] getCharacterArray1d1x1() {
		return new char[] {'\u0031'};
	}
	
	public static char[] getCharacterArray1d1x3() {
		return new char[] {'\u0031', '\u0032', '\u0033'};
	}
	
	
	
	public static double[] getDoubleArray1d0x0() {
		return new double[] {};
	}
	
	public static double[] getDoubleArray1d1x1() {
		return new double[] {Double.MIN_NORMAL};
	}
	
	public static double[] getDoubleArray1dLowZeroHigh() {
		return new double[] {Double.MIN_NORMAL, 0, Double.MAX_VALUE};
	}
	
	
	
	public static float[] getFloatArray1d0x0() {
		return new float[] {};
	}
	
	public static float[] getFloatArray1d1x1() {
		return new float[] {Float.MIN_VALUE};
	}
	
	public static float[] getFloatArray1dLowZeroHigh() {
		return new float[] {Float.MIN_VALUE, 0, Float.MAX_VALUE};
	}
	
	
	
	public static int[] getIntArray1d0x0() {
		return new int[] {};
	}
	
	public static int[] getIntArray1d1x1() {
		return new int[] {Integer.MIN_VALUE};
	}
	
	public static int[] getIntArray1dLowZeroHigh() {
		return new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
	}
	
	
	
	public static long[] getLongArray1d0x0() {
		return new long[] {};
	}
	
	public static long[] getLongArray1d1x1() {
		return new long[] {Long.MIN_VALUE};
	}
	
	public static long[] getLongArray1dLowZeroHigh() {
		return new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE};
	}
	
	
	
	public static short[] getShortArray1d0x0() {
		return new short[] {};
	}
	
	public static short[] getShortArray1d1x1() {
		return new short[] {Short.MIN_VALUE};
	}
	
	public static short[] getShortArray1dLowZeroHigh() {
		return new short[] {Short.MIN_VALUE, 0, Short.MAX_VALUE};
	}
	
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// BOXED ARRAYS 1D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static BigDecimal[] getBigDecimalArray1d0x0() {
		return new BigDecimal[] {};
	}
	
	public static BigDecimal[] getBigDecimalArray1d1x1() {
		return new BigDecimal[] {new BigDecimal(1)};
	}
	
	public static BigDecimal[] getBigDecimalArray1d1x2() {
		return new BigDecimal[] {new BigDecimal(Double.MIN_NORMAL), new BigDecimal(Double.MAX_VALUE)};
	}
	
	public static BigDecimal[] getBigDecimalArray1dNulls() {
		return new BigDecimal[] {null, new BigDecimal(-1), null, new BigDecimal(1), null};
	}
	
	
	
	public static BigInteger[] getBigIntegerArray1d0x0() {
		return new BigInteger[] {};
	}
	
	public static BigInteger[] getBigIntegerArray1d1x1() {
		return new BigInteger[] {(new BigDecimal(1)).toBigInteger()};
	}
	
	public static BigInteger[] getBigIntegerArray1d1x2() {
		return new BigInteger[] {(new BigDecimal(Long.MIN_VALUE)).toBigInteger(), (new BigDecimal(Long.MAX_VALUE)).toBigInteger()};
	}
	
	public static BigInteger[] getBigIntegerArray1dNulls() {
		return new BigInteger[] {null, (new BigDecimal(-1)).toBigInteger(), null, (new BigDecimal(1)).toBigInteger(), null};
	}
	
	
	
	public static Boolean[] getBoxedBooleanArray1d0x0() {
		return new Boolean[] {};
	}
	
	public static Boolean[] getBoxedBooleanArray1d1x1() {
		return new Boolean[] {true};
	}
	
	public static Boolean[] getBoxedBooleanArray1d1x2() {
		return new Boolean[] {false, true};
	}
	
	public static Boolean[] getBoxedBooleanArray1dNulls() {
		return new Boolean[] {null, false, null, true, null};
	}
	
	
	
	public static Byte[] getBoxedByteArray1d0x0() {
		return new Byte[] {};
	}
	
	public static Byte[] getBoxedByteArray1d1x1() {
		return new Byte[] {Byte.MIN_VALUE};
	}
	
	public static Byte[] getBoxedByteArray1dLowZeroHigh() {
		return new Byte[] {Byte.MIN_VALUE, 0, Byte.MAX_VALUE};
	}
	
	public static Byte[] getBoxedByteArray1dNulls() {
		return new Byte[] {null, Byte.MIN_VALUE, null, Byte.MAX_VALUE, null};
	}
	
	
	
	public static Character[] getBoxedCharacterArray1d0x0() {
		return new Character[] {};
	}
	
	public static Character[] getBoxedCharacterArray1d1x1() {
		return new Character[] {'\u0031'};
	}
	
	public static Character[] getBoxedCharacterArray1d1x3() {
		return new Character[] {'\u0031', '\u0032', '\u0033'};
	}
	
	public static Character[] getBoxedCharacterArray1dNulls() {
		return new Character[] {null, '\u0031', null, '\u0033', null};
	}
	
	
	
	public static Double[] getBoxedDoubleArray1d0x0() {
		return new Double[] {};
	}
	
	public static Double[] getBoxedDoubleArray1d1x1() {
		return new Double[] {Double.MIN_NORMAL};
	}
	
	public static Double[] getBoxedDoubleArray1dLowZeroHigh() {
		return new Double[] {Double.MIN_NORMAL, 0d, Double.MAX_VALUE};
	}
	
	public static Double[] getBoxedDoubleArray1dNulls() {
		return new Double[] {null, Double.MIN_NORMAL, null, Double.MAX_VALUE, null};
	}
	
	
	
	public static Float[] getBoxedFloatArray1d0x0() {
		return new Float[] {};
	}
	
	public static Float[] getBoxedFloatArray1d1x1() {
		return new Float[] {Float.MIN_VALUE};
	}
	
	public static Float[] getBoxedFloatArray1dLowZeroHigh() {
		return new Float[] {Float.MIN_VALUE, 0f, Float.MAX_VALUE};
	}
	
	public static Float[] getBoxedFloatArray1dNulls() {
		return new Float[] {null, Float.MIN_VALUE, null, Float.MAX_VALUE, null};
	}
	
	
	
	public static Integer[] getBoxedIntegerArray1d0x0() {
		return new Integer[] {};
	}
	
	public static Integer[] getBoxedIntegerArray1d1x1() {
		return new Integer[] {Integer.MIN_VALUE};
	}
	
	public static Integer[] getBoxedIntegerArray1dLowZeroHigh() {
		return new Integer[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
	}
	
	public static Integer[] getBoxedIntegerArray1dNulls() {
		return new Integer[] {null, Integer.MIN_VALUE, null, Integer.MAX_VALUE, null};
	}
	
	
	
	public static Long[] getBoxedLongArray1d0x0() {
		return new Long[] {};
	}
	
	public static Long[] getBoxedLongArray1d1x1() {
		return new Long[] {Long.MIN_VALUE};
	}
	
	public static Long[] getBoxedLongArray1dLowZeroHigh() {
		return new Long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE};
	}
	
	public static Long[] getBoxedLongArray1dNulls() {
		return new Long[] {null, Long.MIN_VALUE, null, Long.MAX_VALUE, null};
	}
	
	
	
	public static Short[] getBoxedShortArray1d0x0() {
		return new Short[] {};
	}
	
	public static Short[] getBoxedShortArray1d1x1() {
		return new Short[] {Short.MIN_VALUE};
	}
	
	public static Short[] getBoxedShortArray1dLowZeroHigh() {
		return new Short[] {Short.MIN_VALUE, 0, Short.MAX_VALUE};
	}
	
	public static Short[] getBoxedShortArray1dNulls() {
		return new Short[] {null, Short.MIN_VALUE, null, Short.MAX_VALUE, null};
	}
	
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// STRING ARRAYS 1D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static String[] getStringArray1d0x0() {
		return new String[] {};
	}
	
	public static String[] getStringArray1d1x1() {
		return new String[] {"A"};
	}
	
	public static String[] getStringArray1dAlphabetLower() {
		return new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
	}
	
	public static String[] getStringArray1dNulls() {
		return new String[] {null, "b", null, "d", null};
	}
	
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// PRIMITIVE ARRAYS 2D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static boolean[][] getBooleanArray2d0x0() {
		return new boolean[][] {};
	}
	
	public static boolean[][] getBooleanArray2d2x0() {
		return new boolean[][] {{}, {}};
	}
	
	public static boolean[][] getBooleanArray2d2x1() {
		return new boolean[][] {{false}, {true}};
	}
	
	public static boolean[][] getBooleanArray2d2x2() {
		return new boolean[][] {{false, true}, {true, false}};
	}
	
	public static boolean[][] getBooleanArray2dRagged1() {
		return new boolean[][] {{}, {true, false}, {}, {false}, {true}, {}};
	}
	
	public static boolean[][] getBooleanArray2dRagged2() {
		return new boolean[][] {{true, false}, {}, {false}, {true}};
	}
	

	
	public static byte[][] getByteArray2d0x0() {
		return new byte[][] {};
	}
	
	public static byte[][] getByteArray2d2x0() {
		return new byte[][] {{}, {}};
	}
	
	public static byte[][] getByteArray2d2x1() {
		return new byte[][] {{Byte.MIN_VALUE}, {Byte.MAX_VALUE}};
	}
	
	public static byte[][] getByteArray2d2x2() {
		return new byte[][] {{Byte.MIN_VALUE, -1}, {0, Byte.MAX_VALUE}};
	}
	
	public static byte[][] getByteArray2dRagged1() {
		return new byte[][] {{}, {Byte.MIN_VALUE}, {}, {0, Byte.MAX_VALUE}, {}};
	}
	
	public static byte[][] getByteArray2dRagged2() {
		return new byte[][] {{Byte.MIN_VALUE}, {}, {0, Byte.MAX_VALUE}};
	}
	
	
	
	public static char[][] getCharacterArray2d0x0() {
		return new char[][] {};
	}
	
	public static char[][] getCharacterArray2d2x0() {
		return new char[][] {{}, {}};
	}
	
	public static char[][] getCharacterArray2d2x1() {
		return new char[][] {{'\u0031'}, {'\u0032'}};
	}
	
	public static char[][] getCharacterArray2d2x2() {
		return new char[][] {{'\u0031', '\u0032'}, {'\u0033', '\u0034'}};
	}
	
	public static char[][] getCharacterArray2dRagged1() {
		return new char[][] {{}, {'\u0031'}, {}, {'\u0032', '\u0033'}, {}};
	}
	
	public static char[][] getCharacterArray2dRagged2() {
		return new char[][] {{'\u0031'}, {}, {'\u0032', '\u0033'}};
	}
	
	
	
	public static double[][] getDoubleArray2d0x0() {
		return new double[][] {};
	}

	public static double[][] getDoubleArray2d2x0() {
		return new double[][] {{}, {}};
	}

	public static double[][] getDoubleArray2d2x1() {
		return new double[][] {{Double.MIN_NORMAL}, {Double.MAX_VALUE}};
	}

	public static double[][] getDoubleArray2d2x2() {
		return new double[][] {{Double.MIN_NORMAL, -1}, {0, Double.MAX_VALUE}};
	}

	public static double[][] getDoubleArray2dRagged1() {
		return new double[][] {{}, {Double.MIN_NORMAL}, {}, {0, Double.MAX_VALUE}, {}};
	}

	public static double[][] getDoubleArray2dRagged2() {
		return new double[][] {{Double.MIN_NORMAL}, {}, {0, Double.MAX_VALUE}};
	}



	public static float[][] getFloatArray2d0x0() {
		return new float[][] {};
	}

	public static float[][] getFloatArray2d2x0() {
		return new float[][] {{}, {}};
	}

	public static float[][] getFloatArray2d2x1() {
		return new float[][] {{Float.MIN_VALUE}, {Float.MAX_VALUE}};
	}

	public static float[][] getFloatArray2d2x2() {
		return new float[][] {{Float.MIN_VALUE, -1}, {0, Float.MAX_VALUE}};
	}

	public static float[][] getFloatArray2dRagged1() {
		return new float[][] {{}, {Float.MIN_VALUE}, {}, {0, Float.MAX_VALUE}, {}};
	}

	public static float[][] getFloatArray2dRagged2() {
		return new float[][] {{Float.MIN_VALUE}, {}, {0, Float.MAX_VALUE}};
	}



	public static int[][] getIntArray2d0x0() {
		return new int[][] {};
	}

	public static int[][] getIntArray2d2x0() {
		return new int[][] {{}, {}};
	}

	public static int[][] getIntArray2d2x1() {
		return new int[][] {{Integer.MIN_VALUE}, {Integer.MAX_VALUE}};
	}

	public static int[][] getIntArray2d2x2() {
		return new int[][] {{Integer.MIN_VALUE, -1}, {0, Integer.MAX_VALUE}};
	}

	public static int[][] getIntArray2dRagged1() {
		return new int[][] {{}, {Integer.MIN_VALUE}, {}, {0, Integer.MAX_VALUE}, {}};
	}

	public static int[][] getIntArray2dRagged2() {
		return new int[][] {{Integer.MIN_VALUE}, {}, {0, Integer.MAX_VALUE}};
	}



	public static long[][] getLongArray2d0x0() {
		return new long[][] {};
	}

	public static long[][] getLongArray2d2x0() {
		return new long[][] {{}, {}};
	}

	public static long[][] getLongArray2d2x1() {
		return new long[][] {{Long.MIN_VALUE}, {Long.MAX_VALUE}};
	}

	public static long[][] getLongArray2d2x2() {
		return new long[][] {{Long.MIN_VALUE, -1}, {0, Long.MAX_VALUE}};
	}

	public static long[][] getLongArray2dRagged1() {
		return new long[][] {{}, {Long.MIN_VALUE}, {}, {0, Long.MAX_VALUE}, {}};
	}

	public static long[][] getLongArray2dRagged2() {
		return new long[][] {{Long.MIN_VALUE}, {}, {0, Long.MAX_VALUE}};
	}



	public static short[][] getShortArray2d0x0() {
		return new short[][] {};
	}

	public static short[][] getShortArray2d2x0() {
		return new short[][] {{}, {}};
	}

	public static short[][] getShortArray2d2x1() {
		return new short[][] {{Short.MIN_VALUE}, {Short.MAX_VALUE}};
	}

	public static short[][] getShortArray2d2x2() {
		return new short[][] {{Short.MIN_VALUE, -1}, {0, Short.MAX_VALUE}};
	}

	public static short[][] getShortArray2dRagged1() {
		return new short[][] {{}, {Short.MIN_VALUE}, {}, {0, Short.MAX_VALUE}, {}};
	}

	public static short[][] getShortArray2dRagged2() {
		return new short[][] {{Short.MIN_VALUE}, {}, {0, Short.MAX_VALUE}};
	}



	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// BOXED ARRAYS 2D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
	
	public static BigDecimal[][] getBigDecimalArray2d0x0() {
		return new BigDecimal[][] {};
	}

	public static BigDecimal[][] getBigDecimalArray2d2x0() {
		return new BigDecimal[][] {{}, {}};
	}

	public static BigDecimal[][] getBigDecimalArray2d2x1() {
		return new BigDecimal[][] {{new BigDecimal(Double.MIN_NORMAL)}, {new BigDecimal(Double.MAX_VALUE)}};
	}

	public static BigDecimal[][] getBigDecimalArray2d2x2() {
		return new BigDecimal[][] {{BigDecimal.ZERO, BigDecimal.TEN}, {new BigDecimal(Double.MIN_NORMAL), new BigDecimal(Double.MAX_VALUE)}};
	}

	public static BigDecimal[][] getBigDecimalArray2dNulls() {
	    return new BigDecimal[][] {{null, BigDecimal.ZERO, null}, {BigDecimal.ONE, null, BigDecimal.TEN}};
	}

	public static BigDecimal[][] getBigDecimalArray2dRagged1() {
		return new BigDecimal[][] {{}, {BigDecimal.ZERO, BigDecimal.ONE}, {}, {BigDecimal.TEN}, {BigDecimal.ONE}, {}};
	}

	public static BigDecimal[][] getBigDecimalArray2dRagged2() {
		return new BigDecimal[][] {{BigDecimal.ZERO, BigDecimal.ONE}, {}, {BigDecimal.TEN}, {BigDecimal.ZERO}};
	}



	public static BigInteger[][] getBigIntegerArray2d0x0() {
		return new BigInteger[][] {};
	}

	public static BigInteger[][] getBigIntegerArray2d2x0() {
		return new BigInteger[][] {{}, {}};
	}

	public static BigInteger[][] getBigIntegerArray2d2x1() {
		return new BigInteger[][] {{new BigDecimal(Long.MIN_VALUE).toBigInteger()}, {new BigDecimal(Long.MAX_VALUE).toBigInteger()}};
	}

	public static BigInteger[][] getBigIntegerArray2d2x2() {
		return new BigInteger[][] {{BigInteger.ZERO, BigInteger.TEN}, {new BigDecimal(Long.MIN_VALUE).toBigInteger(), new BigDecimal(Long.MAX_VALUE).toBigInteger()}};
	}

	public static BigInteger[][] getBigIntegerArray2dNulls() {
	    return new BigInteger[][] {{null, BigInteger.ZERO, null}, {BigInteger.ONE, null, BigInteger.TEN}};
	}

	public static BigInteger[][] getBigIntegerArray2dRagged1() {
		return new BigInteger[][] {{}, {BigInteger.ZERO, BigInteger.ONE}, {}, {BigInteger.TEN}, {BigInteger.ONE}, {}};
	}

	public static BigInteger[][] getBigIntegerArray2dRagged2() {
		return new BigInteger[][] {{BigInteger.ZERO, BigInteger.ONE}, {}, {BigInteger.TEN}, {BigInteger.ZERO}};
	}



	public static Boolean[][] getBoxedBooleanArray2d0x0() {
		return new Boolean[][] {};
	}

	public static Boolean[][] getBoxedBooleanArray2d2x0() {
		return new Boolean[][] {{}, {}};
	}

	public static Boolean[][] getBoxedBooleanArray2d2x1() {
		return new Boolean[][] {{false}, {true}};
	}

	public static Boolean[][] getBoxedBooleanArray2d2x2() {
		return new Boolean[][] {{false, true}, {true, false}};
	}

	public static Boolean[][] getBoxedBooleanArray2dNulls1() {
	    return new Boolean[][] {{null, false, null}, {true, null, true}};
	}

	public static Boolean[][] getBoxedBooleanArray2dNulls2() {
	    return new Boolean[][] {{false, true}, {true, null}, {false, true}};
	}

	public static Boolean[][] getBoxedBooleanArray2dRagged1() {
		return new Boolean[][] {{}, {true, false}, {}, {false}, {true}, {}};
	}

	public static Boolean[][] getBoxedBooleanArray2dRagged2() {
		return new Boolean[][] {{true, false}, {}, {false}, {true}};
	}



	public static Byte[][] getBoxedByteArray2d0x0() {
		return new Byte[][] {};
	}

	public static Byte[][] getBoxedByteArray2d2x0() {
		return new Byte[][] {{}, {}};
	}

	public static Byte[][] getBoxedByteArray2d2x1() {
		return new Byte[][] {{Byte.MIN_VALUE}, {Byte.MAX_VALUE}};
	}

	public static Byte[][] getBoxedByteArray2d2x2() {
		return new Byte[][] {{Byte.MIN_VALUE, -1}, {0, Byte.MAX_VALUE}};
	}

	public static Byte[][] getBoxedByteArray2dNulls1() {
	    return new Byte[][] {{null, Byte.MIN_VALUE, null}, {Byte.MIN_VALUE, null, Byte.MAX_VALUE}};
	}

	public static Byte[][] getBoxedByteArray2dNulls2() {
	    return new Byte[][] {{Byte.MIN_VALUE, Byte.MAX_VALUE}, {Byte.MIN_VALUE, null}, {Byte.MIN_VALUE, Byte.MAX_VALUE}};
	}

	public static Byte[][] getBoxedByteArray2dRagged1() {
		return new Byte[][] {{}, {Byte.MIN_VALUE}, {}, {0, Byte.MAX_VALUE}, {}};
	}

	public static Byte[][] getBoxedByteArray2dRagged2() {
		return new Byte[][] {{Byte.MIN_VALUE}, {}, {0, Byte.MAX_VALUE}};
	}



	public static Character[][] getBoxedCharacterArray2d0x0() {
		return new Character[][] {};
	}

	public static Character[][] getBoxedCharacterArray2d2x0() {
		return new Character[][] {{}, {}};
	}

	public static Character[][] getBoxedCharacterArray2d2x1() {
		return new Character[][] {{'\u0031'}, {'\u0032'}};
	}

	public static Character[][] getBoxedCharacterArray2d2x2() {
		return new Character[][] {{'\u0031', '\u0032'}, {'\u0033', '\u0034'}};
	}

	public static Character[][] getBoxedCharacterArray2dNulls() {
	    return new Character[][] {{null, '\u0031', null}, {'\u0032', null, '\u0033'}};
	}

	public static Character[][] getBoxedCharacterArray2dRagged1() {
		return new Character[][] {{}, {'\u0031'}, {}, {'\u0032', '\u0033'}, {}};
	}

	public static Character[][] getBoxedCharacterArray2dRagged2() {
		return new Character[][] {{'\u0031'}, {}, {'\u0032', '\u0033'}};
	}



	public static Double[][] getBoxedDoubleArray2d0x0() {
		return new Double[][] {};
	}

	public static Double[][] getBoxedDoubleArray2d2x0() {
		return new Double[][] {{}, {}};
	}

	public static Double[][] getBoxedDoubleArray2d2x1() {
		return new Double[][] {{Double.MIN_NORMAL}, {Double.MAX_VALUE}};
	}

	public static Double[][] getBoxedDoubleArray2d2x2() {
		return new Double[][] {{Double.MIN_NORMAL, -1d}, {0d, Double.MAX_VALUE}};
	}

	public static Double[][] getBoxedDoubleArray2dNulls() {
	    return new Double[][] {{null, Double.MIN_NORMAL, null}, {Double.MIN_NORMAL, null, Double.MAX_VALUE}};
	}

	public static Double[][] getBoxedDoubleArray2dRagged1() {
		return new Double[][] {{}, {Double.MIN_NORMAL}, {}, {0d, Double.MAX_VALUE}, {}};
	}

	public static Double[][] getBoxedDoubleArray2dRagged2() {
		return new Double[][] {{Double.MIN_NORMAL}, {}, {0d, Double.MAX_VALUE}};
	}



	public static Float[][] getBoxedFloatArray2d0x0() {
		return new Float[][] {};
	}

	public static Float[][] getBoxedFloatArray2d2x0() {
		return new Float[][] {{}, {}};
	}

	public static Float[][] getBoxedFloatArray2d2x1() {
		return new Float[][] {{Float.MIN_VALUE}, {Float.MAX_VALUE}};
	}

	public static Float[][] getBoxedFloatArray2d2x2() {
		return new Float[][] {{Float.MIN_VALUE, -1f}, {0f, Float.MAX_VALUE}};
	}

	public static Float[][] getBoxedFloatArray2dNulls() {
	    return new Float[][] {{null, Float.MIN_VALUE, null}, {Float.MIN_VALUE, null, Float.MAX_VALUE}};
	}

	public static Float[][] getBoxedFloatArray2dRagged1() {
		return new Float[][] {{}, {Float.MIN_VALUE}, {}, {0f, Float.MAX_VALUE}, {}};
	}

	public static Float[][] getBoxedFloatArray2dRagged2() {
		return new Float[][] {{Float.MIN_VALUE}, {}, {0f, Float.MAX_VALUE}};
	}



	public static Integer[][] getBoxedIntegerArray2d0x0() {
		return new Integer[][] {};
	}

	public static Integer[][] getBoxedIntegerArray2d2x0() {
		return new Integer[][] {{}, {}};
	}

	public static Integer[][] getBoxedIntegerArray2d2x1() {
		return new Integer[][] {{Integer.MIN_VALUE}, {Integer.MAX_VALUE}};
	}

	public static Integer[][] getBoxedIntegerArray2d2x2() {
		return new Integer[][] {{Integer.MIN_VALUE, -1}, {0, Integer.MAX_VALUE}};
	}

	public static Integer[][] getBoxedIntegerArray2dNulls() {
	    return new Integer[][] {{null, Integer.MIN_VALUE, null}, {Integer.MIN_VALUE, null, Integer.MAX_VALUE}};
	}

	public static Integer[][] getBoxedIntegerArray2dRagged1() {
		return new Integer[][] {{}, {Integer.MIN_VALUE}, {}, {0, Integer.MAX_VALUE}, {}};
	}

	public static Integer[][] getBoxedIntegerArray2dRagged2() {
		return new Integer[][] {{Integer.MIN_VALUE}, {}, {0, Integer.MAX_VALUE}};
	}



	public static Long[][] getBoxedLongArray2d0x0() {
		return new Long[][] {};
	}

	public static Long[][] getBoxedLongArray2d2x0() {
		return new Long[][] {{}, {}};
	}

	public static Long[][] getBoxedLongArray2d2x1() {
		return new Long[][] {{Long.MIN_VALUE}, {Long.MAX_VALUE}};
	}

	public static Long[][] getBoxedLongArray2d2x2() {
		return new Long[][] {{Long.MIN_VALUE, -1L}, {0L, Long.MAX_VALUE}};
	}

	public static Long[][] getBoxedLongArray2dNulls() {
	    return new Long[][] {{null, Long.MIN_VALUE, null}, {Long.MIN_VALUE, null, Long.MAX_VALUE}};
	}

	public static Long[][] getBoxedLongArray2dRagged1() {
		return new Long[][] {{}, {Long.MIN_VALUE}, {}, {0L, Long.MAX_VALUE}, {}};
	}

	public static Long[][] getBoxedLongArray2dRagged2() {
		return new Long[][] {{Long.MIN_VALUE}, {}, {0L, Long.MAX_VALUE}};
	}



	public static Short[][] getBoxedShortArray2d0x0() {
		return new Short[][] {};
	}

	public static Short[][] getBoxedShortArray2d2x0() {
		return new Short[][] {{}, {}};
	}

	public static Short[][] getBoxedShortArray2d2x1() {
		return new Short[][] {{Short.MIN_VALUE}, {Short.MAX_VALUE}};
	}

	public static Short[][] getBoxedShortArray2d2x2() {
		return new Short[][] {{Short.MIN_VALUE, -1}, {0, Short.MAX_VALUE}};
	}

	public static Short[][] getBoxedShortArray2dNulls() {
	    return new Short[][] {{null, Short.MIN_VALUE, null}, {Short.MIN_VALUE, null, Short.MAX_VALUE}};
	}

	public static Short[][] getBoxedShortArray2dRagged1() {
		return new Short[][] {{}, {Short.MIN_VALUE}, {}, {0, Short.MAX_VALUE}, {}};
	}

	public static Short[][] getBoxedShortArray2dRagged2() {
		return new Short[][] {{Short.MIN_VALUE}, {}, {0, Short.MAX_VALUE}};
	}

	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// STRING ARRAYS 2D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 

	public static String[][] getStringArray2d0x0() {
		return new String[][] {};
	}
	
	public static String[][] getStringArray2d2x0() {
		return new String[][] {{}, {}};
	}

	public static String[][] getStringArray2d2x1() {
		return new String[][] {{""}, {""}};
	}

	public static String[][] getStringArray2d2x2() {
		return new String[][] {{"", " "}, {"a", "Z"}};
	}

	public static String[][] getStringArray2dNulls() {
		return new String[][] {{null, "", null}, {"a", null, "Z"}};
	}

	public static String[][] getStringArray2dRagged1() {
		return new String[][] {{}, {""}, {}, {"a", "Z"}, {}};
	}

	public static String[][] getStringArray2dRagged2() {
		return new String[][] {{""}, {}, {"a", "Z"}};
	}
	

	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// BOXED ARRAYS 3D
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

	public static Boolean[][][] getBoxedBooleanArray3dNulls() {
	    return new Boolean[][][] {{{false, true}, {true, null}, {false, true}}, {{false, true}, {true, true}, {false, true}}};
	}

	public static Boolean[][][] getBoxedBooleanArray3dRagged() {
		return new Boolean[][][] {{{true, false}, {}, {false}, {true}}, {{true, false}, {true}, {false}, {true}}};
	}


	public static Byte[][][] getBoxedByteArray3dNulls() {
	    return new Byte[][][] {{{Byte.MIN_VALUE, Byte.MAX_VALUE}, {Byte.MIN_VALUE, null}, {Byte.MIN_VALUE, Byte.MAX_VALUE}}, {{Byte.MIN_VALUE, Byte.MAX_VALUE}, {Byte.MIN_VALUE, Byte.MIN_VALUE}, {Byte.MIN_VALUE, Byte.MAX_VALUE}}};
	}

	public static Byte[][][] getBoxedByteArray3dRagged() {
		return new Byte[][][] {{{Byte.MIN_VALUE}, {}, {0, Byte.MAX_VALUE}}, {{Byte.MIN_VALUE}, {1}, {0, Byte.MAX_VALUE}}};
	}
	
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// COLLECTION
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
	
	// Collections are tested from R using JavaScript.
	
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // 
	// MAP
	// // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
	
	// Maps are tested from R using JavaScript.
	
}
