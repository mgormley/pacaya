package edu.jhu.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.prim.list.IntArrayList;

public class SortTest {
    
    /* ---------- Doubles only --------------*/
    
    @Test
    public void testQuicksortAsc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        Sort.sortAsc(values);
        System.out.println(Arrays.toString(values));
        JUnitUtils.assertArrayEquals(new double[]{ -1.0, 1.0f, 2.0, 3.0f, 5.0}, values, 1e-13);
    }
    
    @Test
    public void testQuicksortDesc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        Sort.sortDesc(values);
        System.out.println(Arrays.toString(values));
        JUnitUtils.assertArrayEquals(new double[]{5.0, 3.0, 2.0, 1.0, -1.0}, values, 1e-13);
    }
    
    @Test
    public void testQuicksortOnRandomInput() {
        Random random = new Random();
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        for (int i=0; i<10; i++) {            
            for (int j=0; j<values.length; j++) {
                values[j] = random.nextDouble();
            }
            Sort.sortAsc(values);
            System.out.println(Arrays.toString(values));
            assertTrue(Sort.isSortedAsc(values));
        }
        
        for (int i=0; i<10; i++) {            
            for (int j=0; j<values.length; j++) {
                values[j] = random.nextDouble();
            }
            Sort.sortDesc(values);
            System.out.println(Arrays.toString(values));
            assertTrue(Sort.isSortedDesc(values));
        }
    }
    
    /* ---------- Ints and Doubles --------------*/
    
    @Test
    public void testSortValuesAsc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = Sort.getIndexArray(values);
        Sort.sortValuesAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ -1.0, 1.0f, 2.0, 3.0f, 5.0}, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 3, 0, 2, 1, 4}, index);
    }
    
    @Test
    public void testSortValuesDesc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = Sort.getIndexArray(values);
        Sort.sortValuesDesc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ 5.0, 3.0, 2.0, 1.0, -1.0}, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 4, 1, 2, 0, 3}, index);
    }
    
    @Test
    public void testSortValuesInfinitiesAsc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        int[] index = Sort.getIndexArray(values);
        Sort.sortValuesAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));

        JUnitUtils.assertArrayEquals(new double[]{Double.NEGATIVE_INFINITY, -1.0, 1.0, 2.0, 5.0, Double.POSITIVE_INFINITY}, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 4, 3, 0, 2, 5, 1 }, index);
    }
    
    @Test
    public void testSortValuesInfinitiesDesc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        int[] index = Sort.getIndexArray(values);
        Sort.sortValuesDesc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{Double.POSITIVE_INFINITY,  5.0, 2.0, 1.0, -1.0, Double.NEGATIVE_INFINITY}, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 1, 5, 2, 0, 3, 4 }, index);
    }    

    @Test
    public void testSortIndexAsc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = new int[] { 1, 4, 5, 8, 3};
        Sort.sortIndexAsc(index, values);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ 1.0, 5.0, 3.0, 2.0, -1.0 }, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 1, 3, 4, 5, 8 }, index);
    }

    @Test
    public void testSortIndexDesc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        int[] index = new int[] { 1, 4, 5, 8, 3};
        Sort.sortIndexDesc(index, values);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ -1.0, 2.0, 3.0, 5.0, 1.0 }, values, 1e-13);
        Assert.assertArrayEquals(new int[]{ 8, 5, 4, 3, 1 }, index);
    }
    
    /* ---------- Longs and Doubles --------------*/
    
    @Test
    public void testLongDoubleSortValuesAsc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        long[] index = Sort.getLongIndexArray(values);
        Sort.sortValuesAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ -1.0, 1.0f, 2.0, 3.0f, 5.0}, values, 1e-13);
        Assert.assertArrayEquals(new long[]{ 3, 0, 2, 1, 4}, index);
    }
    
    @Test
    public void testLongDoubleSortValuesDesc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        long[] index = Sort.getLongIndexArray(values);
        Sort.sortValuesDesc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ 5.0, 3.0, 2.0, 1.0, -1.0}, values, 1e-13);
        Assert.assertArrayEquals(new long[]{ 4, 1, 2, 0, 3}, index);
    }
    
    @Test
    public void testLongDoubleSortValuesInfinitiesAsc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        long[] index = Sort.getLongIndexArray(values);
        Sort.sortValuesAsc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));

        JUnitUtils.assertArrayEquals(new double[]{Double.NEGATIVE_INFINITY, -1.0, 1.0, 2.0, 5.0, Double.POSITIVE_INFINITY}, values, 1e-13);
        Assert.assertArrayEquals(new long[]{ 4, 3, 0, 2, 5, 1 }, index);
    }
    
    @Test
    public void testLongDoubleSortValuesInfinitiesDesc() {
        double[] values = new double[]{ 1.0f, Double.POSITIVE_INFINITY, 2.0f, -1.0, Double.NEGATIVE_INFINITY, 5.0};
        long[] index = Sort.getLongIndexArray(values);
        Sort.sortValuesDesc(values, index);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{Double.POSITIVE_INFINITY,  5.0, 2.0, 1.0, -1.0, Double.NEGATIVE_INFINITY}, values, 1e-13);
        Assert.assertArrayEquals(new long[]{ 1, 5, 2, 0, 3, 4 }, index);
    }    

    @Test
    public void testLongDoubleSortIndexAsc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        long[] index = new long[] { 1, 4, 5, 8, 3};
        Sort.sortIndexAsc(index, values);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ 1.0, 5.0, 3.0, 2.0, -1.0 }, values, 1e-13);
        Assert.assertArrayEquals(new long[]{ 1, 3, 4, 5, 8 }, index);
    }

    @Test
    public void testLongDoubleSortIndexDesc() {
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        long[] index = new long[] { 1, 4, 5, 8, 3};
        Sort.sortIndexDesc(index, values);
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(index));
        
        JUnitUtils.assertArrayEquals(new double[]{ -1.0, 2.0, 3.0, 5.0, 1.0 }, values, 1e-13);
        Assert.assertArrayEquals(new long[]{ 8, 5, 4, 3, 1 }, index);
    }
    
    @Test
    public void testMergeSortedLists() {
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        ArrayList<Integer> l2 = new ArrayList<Integer>();
        // Add some fib nums
        l1.add(1);
        l1.add(2);
        l1.add(3);
        l1.add(5);
        l1.add(8);
        // Add odd numbers
        l2.add(3);
        l2.add(5);
        l2.add(7);
        l2.add(9);
        l2.add(11);
        
        ArrayList<Integer> l3 = Sort.getMergedList(l1, l2);
        Assert.assertArrayEquals(new Integer[]{ 1, 2, 3, 5, 7, 8, 9, 11 }, l3.toArray(new Integer[]{}));
    }
    
    @Test
    public void testMergeSortedArrays() {
        IntArrayList l1 = new IntArrayList();
        IntArrayList l2 = new IntArrayList();
        // Add some fib nums
        l1.add(1);
        l1.add(2);
        l1.add(3);
        l1.add(5);
        l1.add(8);
        // Add odd numbers
        l2.add(3);
        l2.add(5);
        l2.add(7);
        l2.add(9);
        l2.add(11);
        
        int[] l3 = Sort.getMergedSortedArray(l1.toNativeArray(), l2.toNativeArray());
        Assert.assertArrayEquals(new int[]{ 1, 2, 3, 5, 7, 8, 9, 11 }, l3);
    }

    @Test
    public void testDiffSortedLists1() {
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        ArrayList<Integer> l2 = new ArrayList<Integer>();
        // Add some fib nums
        l1.add(1);
        l1.add(2);
        l1.add(3);
        l1.add(5);
        l1.add(8);
        // Add odd numbers
        l2.add(3);
        l2.add(5);
        l2.add(7);
        l2.add(9);
        l2.add(11);
        
        ArrayList<Integer> l3 = Sort.getDiffOfSortedLists(l1, l2);
        System.out.println(l3);
        Assert.assertArrayEquals(new Integer[]{ 1, 2, 8 }, l3.toArray(new Integer[]{}));
    }

    @Test
    public void testDiffSortedLists2() {
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        ArrayList<Integer> l2 = new ArrayList<Integer>();
        // Add some fib nums
        l1.add(1);
        l1.add(2);
        l1.add(3);
        l1.add(5);
        l1.add(8);
        // Add odd numbers
        l2.add(1);
        l2.add(3);
        l2.add(7);
        l2.add(8);
        
        ArrayList<Integer> l3 = Sort.getDiffOfSortedLists(l1, l2);
        System.out.println(l3);
        Assert.assertArrayEquals(new Integer[]{ 2, 5 }, l3.toArray(new Integer[]{}));
    }
    
    @Test
    public void testDiffSortedLists3() {
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        ArrayList<Integer> l2 = new ArrayList<Integer>();
        // Add some fib nums
        l1.add(1);
        l1.add(2);
        l1.add(3);
        // Add odd numbers
        l2.add(1);
        l2.add(2);
        l2.add(3);
        
        ArrayList<Integer> l3 = Sort.getDiffOfSortedLists(l1, l2);
        System.out.println(l3);
        Assert.assertArrayEquals(new Integer[]{ }, l3.toArray(new Integer[]{}));
    }
        
}
