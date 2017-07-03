package edu.jhu.pacaya.sch.util;

import java.util.List;

public class TestUtils {

    /**
     * Tests the equals method on the given equivalence classes. All pairs of
     * objects are tested for equality except pairs where the first element is
     * null.
     * 
     * @param equivClasses
     *            An array of arrays where all objects in each inner array
     *            should be equivalent and objects from different inner arrays
     *            should be different;
     * 
     */
    public static <T> boolean checkEquals(T[][] equivClasses) {
        for (T[] c1 : equivClasses) {
            for (T[] c2 : equivClasses) {
                for (T e1 : c1) {
                    if (e1 != null) {
                        for (T e2 : c2) {
                            if (c1 == c2) {
                                if (!e1.equals(e2)) {
                                    return false;
                                }
                            } else {
                                if (e1.equals(e2)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Performs a junit4 assertion that an instance of exceptionToCatch is thrown when performing f
     */
    public static boolean checkThrows(Runnable f, Class<?> exceptionToCatch) {
        try {
            f.run();
            return false;
        } catch (Throwable e) {
            return exceptionToCatch.isInstance(e);
        }
    }
    
    /**
     * Convert a list of numbers into equivalent array of ints
     */
    public static int[] toIntArray(List<? extends Number> list) {
        return list.stream().mapToInt(Number::intValue).toArray();
    }

    /**
     * Convert a list of Double into equivalent array of double
     */
    public static double[] toArray(List<? extends Number> list) {
        return list.stream().mapToDouble(Number::doubleValue).toArray();
    }

}
