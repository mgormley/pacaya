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
import edu.jhu.prim.util.sort.IntSort;

public class Utilities {
    
    private Utilities() {
        // private constructor
    }
    
    private static final Integer INTEGER_ZERO = Integer.valueOf(0);
    private static final Double DOUBLE_ZERO = Double.valueOf(0.0);
    public static final double LOG2 = log(2);

    public static <X> Integer safeGetInt(Map<X, Integer> map, X key) {
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
            return logAddExact(x,y);
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
        
        int[] array = IntSort.getIndexArray(n);
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
