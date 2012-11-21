package edu.jhu.hltcoe.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Utilities {

    private static final double DEFAULT_DELTA = 1e-13;

    private Utilities() {
        // private constructor
    }
    
    private static final Integer INTEGER_ZERO = Integer.valueOf(0);
    private static final Double DOUBLE_ZERO = Double.valueOf(0.0);
    public static final double LOG2 = log(2);
    
    public static <X> Integer safeGet(Map<X,Integer> map, X key) {
        Integer value = map.get(key);
        if (value == null) {
            return INTEGER_ZERO;
        }
        return value;
    }
    
    public static <X> Double safeGet(Map<X,Double> map, X key) {
        Double value = map.get(key);
        if (value == null) {
            return DOUBLE_ZERO;
        }
        return value;
    }
    
    public static boolean safeEquals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        } else {
            return o1.equals(o2);
        }
    }
    
    public static <X> void increment(Map<X,Integer> map, X key, Integer incr) {
        if (map.containsKey(key)) {
            Integer value = map.get(key);
            map.put(key, value + incr);
        } else {
            map.put(key, incr);
        }
    }
    
    public static <X> void increment(Map<X,Double> map, X key, Double incr) {
        if (map.containsKey(key)) {
            Double value = map.get(key);
            map.put(key, value + incr);
        } else {
            map.put(key, incr);
        }
    }

    public static <X,Y> void increment(Map<X,Map<Y,Integer>> map, X key1, Y key2, Integer incr) {
        if (map.containsKey(key1)) {
            Map<Y,Integer> subMap = map.get(key1);
            increment(subMap, key2, incr);
        } else {
            Map<Y,Integer> subMap = new HashMap<Y,Integer>();
            increment(subMap, key2, incr);
            map.put(key1, subMap);
        }
    }
    
    /**
     * @return The resulting set
     */
    public static <X,Y> Set<Y> addToSet(Map<X,Set<Y>> map, X key, Y value) {
        Set<Y> values;
        if (map.containsKey(key)) {
            values = map.get(key);
            values.add(value);
        } else {
            values = new HashSet<Y>();
            values.add(value);
            map.put(key, values);
        }
        return values;
    }
    
    /**
     * Choose the <X> with the greatest number of votes.
     * @param <X> The type of the thing being voted for.
     * @param votes Maps <X> to a Double representing the number of votes
     *              it received.
     * @return The <X> that received the most votes.
     */
    public static <X> List<X> mostVotedFor(Map<X, Double> votes) {
        // Choose the label with the greatest number of votes
        // If there is a tie, choose the one with the least index
        double maxTalley = Double.NEGATIVE_INFINITY;
        List<X> maxTickets = new ArrayList<X>();
        for(Entry<X,Double> entry : votes.entrySet()) {
            X ticket = entry.getKey();
            double talley = entry.getValue();
            if (talley > maxTalley) {
                maxTickets = new ArrayList<X>();
                maxTickets.add(ticket);
                maxTalley = talley;
            } else if (talley == maxTalley) {
                maxTickets.add(ticket);
            }
        }
        return maxTickets;
    }
    
    public static int factorial(int n)
    {
        if( n <= 1 ) {
            return 1;
        } else {
            return n * factorial(n - 1);
        }
    }
    
    /**
     * Adds two probabilities that are stored as log probabilities.
     * @param x log(p)
     * @param y log(q)
     * @return log(p + q) = log(exp(x) + exp(y))
     */
    public static double logAdd(double x, double y) {
        return logAddExact(x,y);
        //return LogAddTable.logAdd(x,y);
    }
    
    /**
     * Subtracts two probabilities that are stored as log probabilities. 
     * Note that x >= y.
     * 
     * @param x log(p)
     * @param y log(q)
     * @return log(p - q) = log(exp(x) - exp(y))
     * @throws IllegalStateException if x < y
     */
    public static double logSubtract(double x, double y) {
        return logSubtractExact(x,y);
        //return LogAddTable.logSubtract(x,y);
    }
    
    public static double logAddExact(double x, double y) {

        // p = 0 or q = 0, where x = log(p), y = log(q)
        if (Double.NEGATIVE_INFINITY == x) {
            return y;
        } else if (Double.NEGATIVE_INFINITY == y) {
            return x;
        }

        // p != 0 && q != 0
        if (y <= x) {
            return x + Math.log1p(exp(y - x));
        } else {
            return y + Math.log1p(exp(x - y));
        }
    }
    
    /**
     * Subtracts two probabilities that are stored as log probabilities. 
     * Note that x >= y.
     * 
     * @param x log(p)
     * @param y log(q)
     * @return log(p - q) = log(exp(p) + exp(q))
     * @throws IllegalStateException if x < y
     */
    public static double logSubtractExact(double x, double y) {
        if (x < y) {
            throw new IllegalStateException("x must be >= y. x=" + x + " y=" + y);
        }
        
        // p = 0 or q = 0, where x = log(p), y = log(q)
        if (Double.NEGATIVE_INFINITY == y) {
            return x;
        } else if (Double.NEGATIVE_INFINITY == x) {
            return y;
        }

        // p != 0 && q != 0
        return x + Math.log1p(-exp(y - x));
    }

    public static double log(double d) {
        return Math.log(d);
    }
    
    public static double exp(double d) {
        return Math.exp(d);
    }
    
    public static double log2(double d) {
        return log(d) / LOG2;
    }

    public static double[][][][] copyOf(double[][][][] array) {
        double[][][][] clone = new double[array.length][][][];
        for (int i = 0; i < clone.length; i++) {
            clone[i] = copyOf(array[i]);
        }
        return clone;
    }

    public static double[][][] copyOf(double[][][] array) {
        double[][][] clone = new double[array.length][][];
        for (int i = 0; i < clone.length; i++) {
            clone[i] = copyOf(array[i]);
        }
        return clone;
    }

    public static double[][] copyOf(double[][] array) {
        double[][] newArray = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = Utilities.copyOf(array[i], array[i].length);
        }
        return newArray;
    }

    public static int[][] copyOf(int[][] array) {
        int[][] newArray = new int[array.length][];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = Utilities.copyOf(array[i], array[i].length);
        }
        return newArray;
    }

    public static boolean[] copyOf(boolean[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static int[] copyOf(int[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static int[] copyOf(int[] original) {
        return Arrays.copyOf(original, original.length);
    }

    public static double[] copyOf(double[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static double[] copyOf(double[] original) {
        return Arrays.copyOf(original, original.length);
    }

    public static void copy(int[][] array, int[][] clone) {
        assert (array.length == clone.length);
        for (int i = 0; i < array.length; i++) {
            copy(array[i], clone[i]);
        }
    }

    public static void copy(int[] array, int[] clone) {
        assert (array.length == clone.length);
        System.arraycopy(array, 0, clone, 0, array.length);
    }

    public static void copy(double[] array, double[] clone) {
        assert (array.length == clone.length);
        System.arraycopy(array, 0, clone, 0, array.length);
    }

    public static void copy(double[][] array, double[][] clone) {
        assert (array.length == clone.length);
        for (int i = 0; i < clone.length; i++) {
            copy(array[i], clone[i]);
        }
    }

    public static void copy(double[][][] array, double[][][] clone) {
        assert (array.length == clone.length);
        for (int i = 0; i < clone.length; i++) {
            copy(array[i], clone[i]);
        }
    }

    public static void copy(double[][][][] array, double[][][][] clone) {
        assert (array.length == clone.length);
        for (int i = 0; i < clone.length; i++) {
            copy(array[i], clone[i]);
        }
    }

    public static double infinityNorm(double[][] gradient) {
        double maxIN = 0.0;
        for (int i=0; i<gradient.length; i++) {
            double tempVal = infinityNorm(gradient[i]);
            if (tempVal > maxIN) {
                maxIN = tempVal;
            }
        }
        return maxIN;
    }

    private static double infinityNorm(double[] gradient) {
        double maxAbs = 0.0;
        for (int i=0; i<gradient.length; i++) {
            double tempVal = Math.abs(gradient[i]);
            if (tempVal > maxAbs) {
                maxAbs = tempVal;
            }
        }
        return maxAbs;
    }

    /**
     * Fisher-Yates shuffle randomly reorders the elements in array. This
     * is O(n) in the length of the array.
     */
    public static void shuffle(int[] array) {
      for (int i=array.length-1; i > 0; i--) {
        int j = Prng.nextInt(i+1);
        // Swap array[i] and array[j]
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
      }
    }
    
    public static void assertDoubleEquals(double a, double b) {
        assert(Math.abs(a - b) < 0.000000000001);
    }

    public static void assertSameSize(double[][] newLogPhi, double[][] logPhi) {
        assert(newLogPhi.length == logPhi.length);
        for (int k=0; k<logPhi.length; k++) {
            assert(newLogPhi[k].length == logPhi[k].length); 
        }
    }

    public static double logForIlp(double weight) {
        if (weight == 0.0 || weight == -0.0) {
            // CPLEX doesn't accept exponents larger than 37 -- it seems to be
            // cutting off at something close to the 32-bit float limit of
            // 3.4E38.
            // 
            // Before, we used -1E25 since we could add 1 trillion of these
            // together and stay in in the coefficient limit.
            //
            // Now, we use -1E10 because -1E25 causes numerical stability issues
            // for the simplex algorithm
            return -1E10;
        }
        return log(weight);
    }

    public static boolean equals(double a, double b, double delta) {
        return Math.abs(a - b) < delta;
    }

    public static int compare(double a, double b, double delta) {
        if (equals(a, b, delta)) {
            return 0;
        }
        return Double.compare(a, b);
    }

    /**
     * Gets the argmax breaking ties randomly.
     */
    public static IntTuple getArgmax(double[][] array) {
        return getArgmax(array, DEFAULT_DELTA);
    }

    /**
     * Gets the argmax breaking ties randomly.
     */
    public static IntTuple getArgmax(double[][] array, double delta) {
        double maxValue = Double.NEGATIVE_INFINITY;
        int maxX = -1;
        int maxY = -1;
        double numMax = 1;
        for (int x=0; x<array.length; x++) {
            for (int y=0; y<array[x].length; y++) {
                double diff = Utilities.compare(array[x][y], maxValue, delta);
                if (diff == 0 && Prng.nextDouble() < 1.0 / numMax) {
                    maxValue = array[x][y];
                    maxX = x;
                    maxY = y;
                    numMax++;
                } else if (diff > 0) {
                    maxValue = array[x][y];
                    maxX = x;
                    maxY = y;
                    numMax = 1;
                }
            }
        }
        return new IntTuple(maxX, maxY);
    }

    public static boolean lte(double a, double b) {
        return a <= b + DEFAULT_DELTA;
    }
    
    public static boolean lte(double a, double b, double delta) {
        return a <= b + delta;
    }

    public static boolean gte(double a, double b) {
        return a + DEFAULT_DELTA >= b;
    }
    
    public static boolean gte(double a, double b, double delta) {
        return a + delta >= b;
    }    
    
    public static String deepToString(double[][] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (double[] arr : array) {
            sb.append("[");
            for (double a : arr) {
                sb.append(String.format("%10.3g, ", a));
            }
            sb.append("], ");
        }
        sb.append("]");
        return sb.toString();
    }
    
}
