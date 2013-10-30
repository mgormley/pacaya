package edu.jhu.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.map.IntObjectHashMap;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.math.LogAddTable;

public class Utilities {

    @Opt(hasArg = true, description = "Whether to use a log-add table or log-add exact.")
    public static boolean useLogAddTable = false;
    
    private static final double DEFAULT_DELTA = 1e-13;

    private Utilities() {
        // private constructor
    }
    
    private static final Integer INTEGER_ZERO = Integer.valueOf(0);
    private static final Double DOUBLE_ZERO = Double.valueOf(0.0);
    public static final double LOG2 = log(2);
    
     public static <X> Integer safeGetInt(Map<X,Integer> map, X key) {
         Integer value = map.get(key);
         if (value == null) {
             return INTEGER_ZERO;
         }
         return value;
     }
    
    public static <X> Double safeGetDouble(Map<X,Double> map, X key) {
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
     * @return The resulting list.
     */
    public static <X,Y> List<Y> addToList(Map<X,List<Y>> map, X key, Y value) {
        List<Y> values;
        if (map.containsKey(key)) {
            values = map.get(key);
            values.add(value);
        } else {
            values = new ArrayList<Y>();
            values.add(value);
            map.put(key, values);
        }
        return values;
    }

    public static <X,Y> List<Y> safeGetList(Map<X, List<Y>> map, X key) {
        List<Y> list = map.get(key);
        if (list == null) {
            return Collections.emptyList();
        } else {
            return list;
        }
    }
    
    /**
     * @return The resulting list.
     */
    public static IntArrayList addToList(IntObjectHashMap<IntArrayList> map, int key, int value) {
        IntArrayList values;
        if (map.containsKey(key)) {
            values = map.get(key);
            values.add(value);
        } else {
            values = new IntArrayList();
            values.add(value);
            map.put(key, values);
        }
        return values;
    }

    public static IntArrayList safeGetList(IntObjectHashMap<IntArrayList> map, int key) {
        IntArrayList list = map.get(key);
        if (list == null) {
            return new IntArrayList();
        } else {
            return list;
        }
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

    /**
     * Creates a new list containing a slice of the original list.
     * @param list The original list.
     * @param start The index of the first element of the slice (inclusive).
     * @param end The index of the last element of the slice (exclusive).
     * @return A sublist containing elements [start, end).
     */
    public static <X> ArrayList<X> sublist(List<X> list, int start, int end) {
        ArrayList<X> sublist = new ArrayList<X>();
        for (int i=start; i<end; i++) {
            sublist.add(list.get(i));
        }
        return sublist;
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
        if (useLogAddTable) {
            return LogAddTable.logAdd(x,y);
        } else {
            return logAddExact(x,y);
        }
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
        if (useLogAddTable) {
            return LogAddTable.logSubtract(x,y);
        } else {
            return logSubtractExact(x,y);
        }
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

    public static boolean[][][][] copyOf(boolean[][][][] array) {
        boolean[][][][] clone = new boolean[array.length][][][];
        for (int i = 0; i < clone.length; i++) {
            clone[i] = copyOf(array[i]);
        }
        return clone;
    }

    public static boolean[][][] copyOf(boolean[][][] array) {
        boolean[][][] clone = new boolean[array.length][][];
        for (int i = 0; i < clone.length; i++) {
            clone[i] = copyOf(array[i]);
        }
        return clone;
    }

    public static boolean[][] copyOf(boolean[][] array) {
        boolean[][] newArray = new boolean[array.length][];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = Utilities.copyOf(array[i], array[i].length);
        }
        return newArray;
    }

    public static boolean[] copyOf(boolean[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static boolean[] copyOf(boolean[] original) {
        return Arrays.copyOf(original, original.length);
    }

    public static int[][] copyOf(int[][] array) {
        int[][] newArray = new int[array.length][];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = Utilities.copyOf(array[i], array[i].length);
        }
        return newArray;
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

    public static long[] copyOf(long[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static long[] copyOf(long[] original) {
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

    /**
     * Samples a set of m integers without replacement from the range [0,...,n-1]. 
     * @param m The number of integers to return.
     * @param n The number of integers from which to sample.
     * @return The sample as an unsorted integer array.
     */
    public static int[] sampleWithoutReplacement(int m, int n) {
        // This implements a modified form of the genshuf() function from
        // Programming Pearls pg. 129.
        
        // TODO: Design a faster method that only generates min(m, n-m) integers.
        
        int[] array = getIndexArray(n);
        for (int i=0; i<m; i++) {
            int j = Prng.nextInt(n - i) + i;
            // Swap array[i] and array[j]
            int tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
        return Arrays.copyOf(array, m);
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

    public static boolean equals(int a, int b) {
        return a == b;
    }

    public static boolean equals(long a, long b) {
        return a == b;
    }
    
    public static boolean equals(double a, double b, double delta) {
        return Math.abs(a - b) < delta;
    }

    /**
     * Compares two double values up to some delta.
     * 
     * @param a
     * @param b
     * @param delta
     * @return The value 0 if a equals b, a value greater than 0 if if a > b, and a value less than 0 if a < b.  
     */
    public static int compare(double a, double b, double delta) {
        if (equals(a, b, delta)) {
            return 0;
        }
        return Double.compare(a, b);
    }
    
    public static int compare(int a, int b) {
        return a - b;
    }
    
    public static int compare(int[] x, int[] y) {
        for (int i=0; i<Math.min(x.length, y.length); i++) {
            int diff = x[i] - y[i];
            if (diff != 0) {
                return diff;
            }
        }
        
        if (x.length < y.length) {
            return -1;
        } else if (x.length > y.length) {
            return 1;
        } else {
            return 0;
        }
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
    
    /**
     * Gets an array where array[i] = i.
     * @param length The length of the array.
     * @return The new index array.
     */
    public static int[] getIndexArray(int length) {
        int[] index = new int[length];
        for (int i=0; i<index.length; i++) {
            index[i] = i;
        }
        return index;
    }
    
    /**
     * Gets an array where array[i] = i.
     * @param length The length of the array.
     * @return The new index array.
     */
    public static long[] getLongIndexArray(int length) {
        long[] index = new long[length];
        for (int i=0; i<index.length; i++) {
            index[i] = i;
        }
        return index;
    }

    public static List<File> getMatchingFiles(File file, String regexStr) {
        Pattern regex = Pattern.compile(regexStr);
        return getMatchingFiles(file, regex);
    }

    private static List<File> getMatchingFiles(File file, Pattern regex) {
        ArrayList<File> files = new ArrayList<File>();
        if (file.exists()) {
            if (file.isFile()) {
                if (regex.matcher(file.getName()).matches()) {
                    files.add(file);
                }
            } else {
                for (File subFile : file.listFiles()) {
                    files.addAll(getMatchingFiles(subFile, regex));
                }
            }
        }
        return files;
    }

    public static <T> void addAll(ArrayList<T> list, T[] array) {
        if (array == null) {
            return;
        }
        list.addAll(Arrays.asList(array));
    }
    
    public static <T> void addAll(ArrayList<T> list, T[][] array) {
        for (int i=0; i<array.length; i++) {
            addAll(list, array[i]);
        }
    }
    
    public static <T> void addAll(ArrayList<T> list, T[][][] array) {
        for (int i=0; i<array.length; i++) {
            addAll(list, array[i]);
        }
    }
    
    public static <T> void addAll(ArrayList<T> list, T[][][][] array) {
        for (int i=0; i<array.length; i++) {
            addAll(list, array[i]);
        }
    }
    
    public static void fill(double[][] array, double value) {
        for (int i=0; i<array.length; i++) {
            Arrays.fill(array[i], value);
        }
    }
    
    public static void fill(double[][][] array, double value) {
        for (int i=0; i<array.length; i++) {
            Utilities.fill(array[i], value);
        }
    }
    
    public static void fill(double[][][][] array, double value) {
        for (int i=0; i<array.length; i++) {
            Utilities.fill(array[i], value);
        }
    }
    
    public static void fill(int[][] array, int value) {
        for (int i=0; i<array.length; i++) {
            Arrays.fill(array[i], value);
        }
    }
    
    public static void fill(int[][][] array, int value) {
        for (int i=0; i<array.length; i++) {
            Utilities.fill(array[i], value);
        }
    }
    
    public static void fill(int[][][][] array, int value) {
        for (int i=0; i<array.length; i++) {
            Utilities.fill(array[i], value);
        }
    }
    
    public static void fill(boolean[][] array, boolean value) {
        for (int i=0; i<array.length; i++) {
            Arrays.fill(array[i], value);
        }
    }
    
    public static void fill(boolean[][][] array, boolean value) {
        for (int i=0; i<array.length; i++) {
            Utilities.fill(array[i], value);
        }
    }
    
    public static void fill(boolean[][][][] array, boolean value) {
        for (int i=0; i<array.length; i++) {
            Utilities.fill(array[i], value);
        }
    }

    public static void fill(Object[][] array, Object value) {
        for (int i=0; i<array.length; i++) {
            Arrays.fill(array[i], value);
        }
    }
    
    /**
     * Faster version of Arrays.fill(). That standard version does NOT use
     * memset, and only iterates over the array filling each value. This method
     * works out to be much faster and seems to be using memset as appropriate.
     */
    // TODO: Iterating is still the fastest way to fill an array.
    public static void fill(final double[] array, final double value) {
        //        final int n = array.length;
        //        if (n > 0) {
        //            array[0] = value;
        //        }
        //        for (int i = 1; i < n; i += i) {
        //           System.arraycopy(array, 0, array, i, ((n - i) < i) ? (n - i) : i);
        //        }
        for (int i=0; i<array.length; i++) {
            array[i] = value;
        }
    }
    
    /**
     * Faster version of Arrays.fill(). That standard version does NOT use
     * memset, and only iterates over the array filling each value. This method
     * works out to be much faster and seems to be using memset as appropriate.
     */
    public static void fill(final boolean[] array, final boolean value) {
        //        final int n = array.length;
        //        if (n > 0) {
        //            array[0] = value;
        //        }
        //        for (int i = 1; i < n; i += i) {
        //           System.arraycopy(array, 0, array, i, ((n - i) < i) ? (n - i) : i);
        //        }
        for (int i=0; i<array.length; i++) {
            array[i] = value;
        }
    }

    /**
     * Faster version of Arrays.fill(). That standard version does NOT use
     * memset, and only iterates over the array filling each value. This method
     * works out to be much faster and seems to be using memset as appropriate.
     */
    public static void fill(final Object[] array, final Object value) {
        //        final int n = array.length;
        //        if (n > 0) {
        //            array[0] = value;
        //        }
        //        for (int i = 1; i < n; i += i) {
        //           System.arraycopy(array, 0, array, i, ((n - i) < i) ? (n - i) : i);
        //        }
        for (int i=0; i<array.length; i++) {
            array[i] = value;
        }        
    }

    public static <T> List<T> getList(T... args) {
        return Arrays.asList(args);
    }

    public static String toString(double[] array, String formatString) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i=0; i<array.length; i++) {
            sb.append(String.format(formatString, array[i]));
            if (i < array.length -1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static <T> ArrayList<T> copyOf(List<T> list) {
        return list == null ? null : new ArrayList<T>(list);
    }

    public static <T> Iterable<T> asIterable(final Iterator<T> iterator) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return iterator;
            }            
        };
    }

    /** Gets a new list of Strings that have been interned. */
    public static ArrayList<String> getInternedList(List<String> oldList) {
        ArrayList<String> newList = new ArrayList<String>(oldList.size());
        for (String elem : oldList) {
            newList.add(elem.intern());
        }
        return newList;
    }
}
