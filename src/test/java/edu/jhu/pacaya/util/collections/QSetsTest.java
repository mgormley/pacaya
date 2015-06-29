package edu.jhu.pacaya.util.collections;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;


public class QSetsTest {


    @Test
    public void testChoose() {
        Assert.assertEquals(3, QSets.choose(3,2));
        Assert.assertEquals(4, QSets.choose(4,3));
        Assert.assertEquals(6, QSets.choose(4,2));
    }

    @Test
    public void testBinomCoefficient() {
        Assert.assertEquals(3, QSets.binomialCoefficient(3,2));
        Assert.assertEquals(4, QSets.binomialCoefficient(4,3));
    }
    
    @Test
    public void testCreateSmallSets() {
        List<int[]> sets = QSets.getSets(2, 4);
        int i=0;
        for(int[] set : sets) {
            System.out.println(i++ + " " + Arrays.toString(set));
        }
    }

    @Test
    public void testCreateSmallSets2() {
        List<int[]> sets = QSets.getSets(3, 5);
        int i=0;
        for(int[] set : sets) {
            System.out.println(i++ + " " + Arrays.toString(set));
        }
    }
    
}
