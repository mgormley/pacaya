package edu.jhu.hltcoe.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.util.math.Vectors;

public class Sort {

    public Sort() {
        // private constructor
    }

    public static int[] getIndexArray(double[] values) {
        return Utilities.getIndexArray(values.length);
    }

    /**
     * Performs an in-place quick sort on values. All the sorting operations on values
     * are mirrored in index. Sorts in descending order.
     */
    public static void sortValuesDesc(double[] values, int[] index) {
        Vectors.scale(values, -1.0);
        sortValuesAsc(values, index);
        Vectors.scale(values, -1.0);
    }
    
    /**
     * Performs an in-place quick sort on values. All the sorting operations on values
     * are mirrored in index. Sorts in ascending order.
     */
    public static void sortValuesAsc(double[] values, int[] index) {
        quicksortValues(values, index, 0, index.length - 1);
    }

    public static void quicksortValues(double[] a, int[] index, int left, int right) {
    }

    /**
     * Performs an in-place quick sort on index. All the sorting operations on index
     * are mirrored in values. Sorts in descending order.
     */
    public static void sortIndexDesc(int[] index, double[] values) {
        Vectors.scale(index, -1);
        sortIndexAsc(index, values);
        Vectors.scale(index, -1);
    }
    
    /**
     * Performs an in-place quick sort on index. All the sorting operations on index
     * are mirrored in values. Sorts in ascending order.
     * @return index - sorted.
     */
    public static int[] sortIndexAsc(int[] index, double[] values) {
        quicksortIndex(index, values, 0, index.length - 1);
        return index;
    }

    public static void quicksortIndex(int[] index, double[] values, int left, int right) {

    }

    /**
     * Returns whether x is less than y.
     */
    private static boolean ltInt(int x, int y) {
        return (x < y);
    }
    
    /**
     * Returns whether x is less than y.
     */
    private static boolean ltDouble(double x, double y) {
        return (x < y);
    }

    /**
     * Swaps the elements at positions i and j in both the values and index array, which must be the same length.
     * @param values An array of values.
     * @param index An array of indices.
     * @param i The position of the first element to swap.
     * @param j The position of the second element to swap.
     */
    private static void swap(double[] values, int[] index, int i, int j) {
        swap(values, i, j);
        swap(index, i, j);
    }

    /**
     * Swaps the elements at positions i and j.
     */
    private static void swap(double[] array, int i, int j) {
        double valAtI = array[i];
        array[i] = array[j];
        array[j] = valAtI;
    }

    /**
     * Swaps the elements at positions i and j.
     */
    private static void swap(int[] array, int i, int j) {
        int valAtI = array[i];
        array[i] = array[j];
        array[j] = valAtI;
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
