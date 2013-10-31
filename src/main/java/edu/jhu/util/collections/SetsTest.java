package edu.jhu.util.collections;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;


public class SetsTest {


    @Test
    public void testChoose() {
        Assert.assertEquals(3, Sets.choose(3,2));
        Assert.assertEquals(4, Sets.choose(4,3));
        Assert.assertEquals(6, Sets.choose(4,2));
    }

    @Test
    public void testBinomCoefficient() {
        Assert.assertEquals(3, Sets.binomialCoefficient(3,2));
        Assert.assertEquals(4, Sets.binomialCoefficient(4,3));
    }
    
    @Test
    public void testCreateSmallSets() {
        List<int[]> sets = Sets.getSets(2, 4);
        int i=0;
        for(int[] set : sets) {
            System.out.println(i++ + " " + Arrays.toString(set));
        }
    }

    @Test
    public void testCreateSmallSets2() {
        List<int[]> sets = Sets.getSets(3, 5);
        int i=0;
        for(int[] set : sets) {
            System.out.println(i++ + " " + Arrays.toString(set));
        }
    }
    
}
