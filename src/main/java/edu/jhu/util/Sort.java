package edu.jhu.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.prim.list.IntArrayList;
import edu.jhu.util.math.Vectors;

public class Sort {

    public Sort() {
        // private constructor
    }

    /* ------------------- Doubles only --------------- */
    
    /**
     * Performs an in-place quick sort on array. Sorts in descending order.
     */
    public static void sortDesc(double[] array) {
        Vectors.scale(array, -1.0);
        sortAsc(array);
        Vectors.scale(array, -1.0);
    }
    
    /**
     * Performs an in-place quick sort on array. Sorts in acscending order.
     */
    public static void sortAsc(double[] array) {
        quicksort(array, 0, array.length-1);
    }
    
    private static void quicksort(double[] array, int left, int right) {
        if (left < right) {
            // Choose a pivot index.
            // --> Here we choose the rightmost element which does the least
            // amount of work if the array is already sorted.
            int pivotIndex = right;
            // Partition the array so that everything less than
            // values[pivotIndex] is on the left of pivotNewIndex and everything
            // greater than or equal is on the right.
            int pivotNewIndex = partition(array, left, right, pivotIndex);
            // Recurse on the left and right sides.
            quicksort(array, left, pivotNewIndex - 1);
            quicksort(array, pivotNewIndex + 1, right);
        }
    }
    
    private static int partition(double[] array, int left, int right, int pivotIndex) {
        double pivotValue = array[pivotIndex];
        // Move the pivot value to the rightmost position.
        swap(array, pivotIndex, right);
        // For each position between left and right, swap all the values less
        // than or equal to the pivot value to the left side.
        int storeIndex = left;
        for (int i=left; i<right; i++) {
            if (array[i] <= pivotValue) {
                swap(array, i, storeIndex);
                storeIndex++;
            }
        }
        // Move the pivot value back to the split point.
        swap(array, storeIndex, right);
        return storeIndex;
    }
    
    public static int[] getIndexArray(double[] values) {
        return Utilities.getIndexArray(values.length);
    }

    /* ------------------- Ints and Doubles --------------- */        
    
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

    private static void quicksortValues(double[] array, int[] index, int left, int right) {
        if (left < right) {
            // Choose a pivot index.
            // --> Here we choose the rightmost element which does the least
            // amount of work if the array is already sorted.
            int pivotIndex = right;
            // Partition the array so that everything less than
            // values[pivotIndex] is on the left of pivotNewIndex and everything
            // greater than or equal is on the right.
            int pivotNewIndex = partitionValues(array, index, left, right, pivotIndex);
            // Recurse on the left and right sides.
            quicksortValues(array, index, left, pivotNewIndex - 1);
            quicksortValues(array, index, pivotNewIndex + 1, right);
        }
    }
    
    private static int partitionValues(double[] array, int[] index, int left, int right, int pivotIndex) {
        double pivotValue = array[pivotIndex];
        // Move the pivot value to the rightmost position.
        swap(array, index, pivotIndex, right);
        // For each position between left and right, swap all the values less
        // than or equal to the pivot value to the left side.
        int storeIndex = left;
        for (int i=left; i<right; i++) {
            if (array[i] <= pivotValue) {
                swap(array, index, i, storeIndex);
                storeIndex++;
            }
        }
        // Move the pivot value back to the split point.
        swap(array, index, storeIndex, right);
        return storeIndex;
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

    private static void quicksortIndex(int[] array, double[] values, int left, int right) {
        if (left < right) {
            // Choose a pivot index.
            // --> Here we choose the rightmost element which does the least
            // amount of work if the array is already sorted.
            int pivotIndex = right;
            // Partition the array so that everything less than
            // values[pivotIndex] is on the left of pivotNewIndex and everything
            // greater than or equal is on the right.
            int pivotNewIndex = partitionIndex(array, values, left, right, pivotIndex);
            // Recurse on the left and right sides.
            quicksortIndex(array, values, left, pivotNewIndex - 1);
            quicksortIndex(array, values, pivotNewIndex + 1, right);
        }
    }
    
    private static int partitionIndex(int[] array, double[] values, int left, int right, int pivotIndex) {
        int pivotValue = array[pivotIndex];
        // Move the pivot value to the rightmost position.
        swap(values, array, pivotIndex, right);
        // For each position between left and right, swap all the values less
        // than or equal to the pivot value to the left side.
        int storeIndex = left;
        for (int i=left; i<right; i++) {
            if (array[i] <= pivotValue) {
                swap(values, array, i, storeIndex);
                storeIndex++;
            }
        }
        // Move the pivot value back to the split point.
        swap(values, array, storeIndex, right);
        return storeIndex;
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

    public static long[] getLongIndexArray(double[] values) {
        return Utilities.getLongIndexArray(values.length);
    }
    
    /* ------------------- Longs and Doubles --------------- */
    
    /**
     * Performs an in-place quick sort on values. All the sorting operations on values
     * are mirrored in index. Sorts in descending order.
     */
    public static void sortValuesDesc(double[] values, long[] index) {
        Vectors.scale(values, -1.0);
        sortValuesAsc(values, index);
        Vectors.scale(values, -1.0);
    }
    
    /**
     * Performs an in-place quick sort on values. All the sorting operations on values
     * are mirrored in index. Sorts in ascending order.
     */
    public static void sortValuesAsc(double[] values, long[] index) {
        quicksortValues(values, index, 0, index.length - 1);
    }

    private static void quicksortValues(double[] array, long[] index, int left, int right) {
        if (left < right) {
            // Choose a pivot index.
            // --> Here we choose the rightmost element which does the least
            // amount of work if the array is already sorted.
            int pivotIndex = right;
            // Partition the array so that everything less than
            // values[pivotIndex] is on the left of pivotNewIndex and everything
            // greater than or equal is on the right.
            int pivotNewIndex = partitionValues(array, index, left, right, pivotIndex);
            // Recurse on the left and right sides.
            quicksortValues(array, index, left, pivotNewIndex - 1);
            quicksortValues(array, index, pivotNewIndex + 1, right);
        }
    }
    
    private static int partitionValues(double[] array, long[] index, int left, int right, int pivotIndex) {
        double pivotValue = array[pivotIndex];
        // Move the pivot value to the rightmost position.
        swap(array, index, pivotIndex, right);
        // For each position between left and right, swap all the values less
        // than or equal to the pivot value to the left side.
        int storeIndex = left;
        for (int i=left; i<right; i++) {
            if (array[i] <= pivotValue) {
                swap(array, index, i, storeIndex);
                storeIndex++;
            }
        }
        // Move the pivot value back to the split point.
        swap(array, index, storeIndex, right);
        return storeIndex;
    }

    /**
     * Performs an in-place quick sort on index. All the sorting operations on index
     * are mirrored in values. Sorts in descending order.
     */
    public static void sortIndexDesc(long[] index, double[] values) {
        Vectors.scale(index, -1);
        sortIndexAsc(index, values);
        Vectors.scale(index, -1);
    }
    
    /**
     * Performs an in-place quick sort on index. All the sorting operations on index
     * are mirrored in values. Sorts in ascending order.
     * @return index - sorted.
     */
    public static long[] sortIndexAsc(long[] index, double[] values) {
        quicksortIndex(index, values, 0, index.length - 1);
        return index;
    }

    private static void quicksortIndex(long[] array, double[] values, int left, int right) {
        if (left < right) {
            // Choose a pivot index.
            // --> Here we choose the rightmost element which does the least
            // amount of work if the array is already sorted.
            int pivotIndex = right;
            // Partition the array so that everything less than
            // values[pivotIndex] is on the left of pivotNewIndex and everything
            // greater than or equal is on the right.
            int pivotNewIndex = partitionIndex(array, values, left, right, pivotIndex);
            // Recurse on the left and right sides.
            quicksortIndex(array, values, left, pivotNewIndex - 1);
            quicksortIndex(array, values, pivotNewIndex + 1, right);
        }
    }
    
    private static int partitionIndex(long[] array, double[] values, int left, int right, int pivotIndex) {
        long pivotValue = array[pivotIndex];
        // Move the pivot value to the rightmost position.
        swap(values, array, pivotIndex, right);
        // For each position between left and right, swap all the values less
        // than or equal to the pivot value to the left side.
        int storeIndex = left;
        for (int i=left; i<right; i++) {
            if (array[i] <= pivotValue) {
                swap(values, array, i, storeIndex);
                storeIndex++;
            }
        }
        // Move the pivot value back to the split point.
        swap(values, array, storeIndex, right);
        return storeIndex;
    }
        
    /**
     * Swaps the elements at positions i and j in both the values and index array, which must be the same length.
     * @param values An array of values.
     * @param index An array of indices.
     * @param i The position of the first element to swap.
     * @param j The position of the second element to swap.
     */
    private static void swap(double[] values, long[] index, int i, int j) {
        swap(values, i, j);
        swap(index, i, j);
    }
    
    /* ----------------------------------------------------- */

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

    /**
     * Swaps the elements at positions i and j.
     */
    private static void swap(long[] array, int i, int j) {
        long valAtI = array[i];
        array[i] = array[j];
        array[j] = valAtI;
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

    public static <T extends Comparable<T>> void mergeSortedLists(List<T> list1, List<T> list2, List<T> newList) {
        int i=0; 
        int j=0;
        while(i < list1.size() && j < list2.size()) {
            T e1 = list1.get(i);
            T e2 = list2.get(j);
            int diff = e1.compareTo(e2);
            if (diff == 0) {
                // Elements are equal. Only add one.
                newList.add(e1);
                i++;
                j++;
            } else if (diff < 0) {
                // e1 is less than e2, so only add e1 this round.
                newList.add(e1);
                i++;
            } else {
                // e2 is less than e1, so only add e2 this round.
                newList.add(e2);
                j++;
            }
        }

        // If there is a list that we didn't get all the way through, add all
        // the remaining elements. There will never be more than one such list. 
        assert (!(i < list1.size() && j < list2.size()));
        for (; i < list1.size(); i++) {
            newList.add(list1.get(i));
        }
        for (; j < list2.size(); j++) {
            newList.add(list2.get(j));
        }
    }

    public static <T extends Comparable<T>> ArrayList<T> getMergedList(List<T> list1,
            List<T> list2) {
        ArrayList<T> newList = new ArrayList<T>();
        mergeSortedLists(list1, list2, newList);
        return newList;
    }

    public static int[] getMergedSortedArray(int[] list1, int[] list2) {
        IntArrayList newList = new IntArrayList();
        int i=0; 
        int j=0;
        while(i < list1.length && j < list2.length) {
            int e1 = list1[i];
            int e2 = list2[j];
            int diff = e1 - e2;
            if (diff == 0) {
                // Elements are equal. Only add one.
                newList.add(e1);
                i++;
                j++;
            } else if (diff < 0) {
                // e1 is less than e2, so only add e1 this round.
                newList.add(e1);
                i++;
            } else {
                // e2 is less than e1, so only add e2 this round.
                newList.add(e2);
                j++;
            }
        }

        // If there is a list that we didn't get all the way through, add all
        // the remaining elements. There will never be more than one such list. 
        assert (!(i < list1.length && j < list2.length));
        for (; i < list1.length; i++) {
            newList.add(list1[i]);
        }
        for (; j < list2.length; j++) {
            newList.add(list2[j]);
        }
        return newList.toNativeArray();
    }

}
