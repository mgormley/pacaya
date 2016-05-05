package edu.jhu.pacaya.sch.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public static <T> void testEquals(T[][] equivClasses) {
        for (T[] c1 : equivClasses) {
            for (T[] c2 : equivClasses) {
                for (T e1 : c1) {
                    if (e1 != null) {
                        for (T e2 : c2) {
                            if (c1 == c2) {
                                assertTrue(e1.equals(e2));
                            } else {
                                assertFalse(e1.equals(e2));
                            }
                        }
                    }
                }
            }
        }
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
}
