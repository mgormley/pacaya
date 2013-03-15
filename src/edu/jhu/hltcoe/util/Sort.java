package edu.jhu.hltcoe.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.util.math.Vectors;

public class Sort {

    public Sort() {
        // private constructor
    }

    
    @Test
    public void testSortValues() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = getIndexArray(values);
        Sort.sortValuesAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        JUnitUtils.assertArrayEquals(new double[]{ -1.0, 1.0f, 2.0, 3.0f, 5.0}, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 3, 0, 2, 1, 4}, index);
    }
    
    @Test
    public void testSortValuesInfinitiesAsc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        int[] index = getIndexArray(values);
        Sort.sortValuesAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        // TODO: add assertions 
    }
    
    @Test
    public void testSortValuesInfinitiesDesc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        int[] index = getIndexArray(values);
        Sort.sortValuesDesc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        // TODO: add assertions 
    }    

    @Test
    public void testSortIndex() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = new int[] { 1, 4, 5, 8, 3};
        Sort.sortIndexAsc(index, values);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        JUnitUtils.assertArrayEquals(new double[]{ 1.0, 5.0, 3.0, 2.0, -1.0 }, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 1, 3, 4, 5, 8 }, index);
    }
    
    public static int[] getIndexArray(double[] main) {
        return Utilities.getIndexArray(main.length);
    }

    // Copied from: http://stackoverflow.com/questions/951848/java-array-sort-quick-way-to-get-a-sorted-list-of-indices-of-an-array
    /**
     * Performs an in-place quick sort on main. All the sorting operations on main
     * are mirrored in index. Sorts in descending order.
     */
    public static void sortValuesDesc(double[] main, int[] index) {
        Vectors.scale(main, -1.0);
        sortValuesAsc(main, index);
        Vectors.scale(main, -1.0);
    }
    
    /**
     * Performs an in-place quick sort on main. All the sorting operations on main
     * are mirrored in index. Sorts in ascending order.
     */
    public static void sortValuesAsc(double[] main, int[] index) {
        quicksortValues(main, index, 0, index.length - 1);
    }

    // quicksort a[left] to a[right]
    public static void quicksortValues(double[] a, int[] index, int left, int right) {
        if (right <= left) return;
        int i = partitionValues(a, index, left, right);
        quicksortValues(a, index, left, i-1);
        quicksortValues(a, index, i+1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int partitionValues(double[] a, int[] index, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (less(a[++i], a[right]))      // find item on left to swap
                ;                               // a[right] acts as sentinel
            while (less(a[right], a[--j]))      // find item on right to swap
                if (j == left) break;           // don't go out-of-bounds
            if (i >= j) break;                  // check if pointers cross
            exch(a, index, i, j);               // swap two elements into place
        }
        exch(a, index, i, right);               // swap with partition element
        return i;
    }
    
    /**
     * Performs an in-place quick sort on index. All the sorting operations on index
     * are mirrored in main. Sorts in descending order.
     */
    public static void sortIndexDesc(int[] index, double[] main) {
        Vectors.scale(index, -1);
        sortIndexAsc(index, main);
        Vectors.scale(index, -1);
    }
    
    /**
     * Performs an in-place quick sort on index. All the sorting operations on index
     * are mirrored in main. Sorts in ascending order.
     * @return index - sorted.
     */
    public static int[] sortIndexAsc(int[] index, double[] main) {
        quicksortIndex(index, main, 0, index.length - 1);
        return index;
    }

    // quicksort index[left] to index[right]
    public static void quicksortIndex(int[] index, double[] a, int left, int right) {
        if (right <= left) return;
        int i = partitionIndex(index, a, left, right);
        quicksortIndex(index, a, left, i-1);
        quicksortIndex(index, a, i+1, right);
    }

    // partition index[left] to index[right], assumes left < right
    private static int partitionIndex(int[] index, double[] a, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (less(index[++i], index[right]))      // find item on left to swap
                ;                               // a[right] acts as sentinel
            while (less(index[right], index[--j]))      // find item on right to swap
                if (j == left) break;           // don't go out-of-bounds
            if (i >= j) break;                  // check if pointers cross
            exch(a, index, i, j);               // swap two elements into place
        }
        exch(a, index, i, right);               // swap with partition element
        return i;
    }

    // is x > y ?
    private static boolean less(int x, int y) {
        return (x < y);
    }
    
    // is x > y ?
    private static boolean less(double x, double y) {
        return (x < y);
    }

    // exchange a[i] and a[j]
    private static void exch(double[] a, int[] index, int i, int j) {
        double swap = a[i];
        a[i] = a[j];
        a[j] = swap;
        int b = index[i];
        index[i] = index[j];
        index[j] = b;
    }
    

    public static boolean isSortedAsc(int[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] > array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedAscAndUnique(int[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] >= array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedDesc(int[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] < array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedAsc(double[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] > array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedDesc(double[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] < array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedAsc(long[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] > array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedAscAndUnique(long[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] >= array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public static boolean isSortedDesc(long[] array) {
    	for (int i=0; i<array.length-1; i++) {
    		if (array[i] < array[i+1]) {
    			return false;
    		}
    	}
    	return true;
    }


}
