package edu.jhu.hltcoe.util;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.hltcoe.math.Vectors;

public class Sort {

    public Sort() {
        // private constructor
    }

    
    @Test
    public void testSorting() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = getIndexArray(values);
        Sort.quicksortAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
    }
    
    @Test
    public void testInfinitiesAsc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        int[] index = getIndexArray(values);
        Sort.quicksortAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
    }
    
    @Test
    public void testInfinitiesDesc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        int[] index = getIndexArray(values);
        Sort.quicksortDesc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
    }
    
    public static int[] getIndexArray(double[] main) {
        int[] index = new int[main.length];
        for (int i=0; i<index.length; i++) {
            index[i] = i;
        }
        return index;
    }

    // Copied from: http://stackoverflow.com/questions/951848/java-array-sort-quick-way-to-get-a-sorted-list-of-indices-of-an-array
    /**
     * Performs an in-place quick sort on main. All the sorting operations on main
     * are mirrored in index. Sorts in descending order.
     */
    public static void quicksortDesc(double[] main, int[] index) {
        Vectors.scale(main, -1.0);
        quicksortAsc(main, index);
        Vectors.scale(main, -1.0);
    }
    
    /**
     * Performs an in-place quick sort on main. All the sorting operations on main
     * are mirrored in index. Sorts in ascending order.
     */
    public static void quicksortAsc(double[] main, int[] index) {
        quicksort(main, index, 0, index.length - 1);
    }

    // quicksort a[left] to a[right]
    public static void quicksort(double[] a, int[] index, int left, int right) {
        if (right <= left) return;
        int i = partition(a, index, left, right);
        quicksort(a, index, left, i-1);
        quicksort(a, index, i+1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int partition(double[] a, int[] index, 
    int left, int right) {
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

}
