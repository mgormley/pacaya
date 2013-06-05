package edu.jhu.hltcoe.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class SortTest {
    
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
        double[] values = new double[]{ 1.0f, 3.0f, 2.0f, -1.0, 5.0};
        for (int i=0; i<10; i++) {            
            for (int j=0; j<values.length; j++) {
                values[j] = Prng.nextDouble();
            }
            Sort.sortAsc(values);
            System.out.println(Arrays.toString(values));
            assertTrue(Sort.isSortedAsc(values));
        }
        
        for (int i=0; i<10; i++) {            
            for (int j=0; j<values.length; j++) {
                values[j] = Prng.nextDouble();
            }
            Sort.sortDesc(values);
            System.out.println(Arrays.toString(values));
            assertTrue(Sort.isSortedDesc(values));
        }
    }
    
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
    
}
