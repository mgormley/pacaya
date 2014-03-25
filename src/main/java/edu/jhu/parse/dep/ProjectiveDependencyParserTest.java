package edu.jhu.parse.dep;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.parse.dep.ProjectiveDependencyParser.DepIoChart;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Timer;

public class ProjectiveDependencyParserTest {
    
    /**
     * Output:
     * SEED=123456789101112
     * Total time: 231.0
     * Sentences per second: 432.90043290043286
     * Total time: 183.0
     * Sentences per second: 546.448087431694
     * Total time: 195.0
     * Sentences per second: 512.8205128205128
     * 
     * After adding Inside/Outside:
     * Total time: 317.0
     * Sentences per second: 315.45741324921136
     * Tokens per second: 8955.223880597014
     */
    @Test
    public void testParseSpeed() {
        int trials = 100;
        int n = 30;

        // Just create one tree.
        double[] root = Multinomials.randomMultinomial(n);
        double[][] child = new double[n][];
        for (int i=0; i<n; i++) {
            child[i] =  Multinomials.randomMultinomial(n);
        }
        
        Timer timer = new Timer();
        timer.start();
        for (int t=0; t<trials; t++) {
            int[] parents = new int[n];
            ProjectiveDependencyParser.parse(root, child, parents);
            //ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);
        }
        timer.stop();
        System.out.println("Total time: " + timer.totMs());
        int numSents = trials;
        int numTokens = n * numSents;
        System.out.println("Sentences per second: " + numSents / timer.totSec());
        System.out.println("Tokens per second: " + numTokens / timer.totSec());
    }

    /**
     * Output:
     * SEED=123456789101112
     * 100 trials: Tokens per second: 5338.078291814946
     * 1000 trials: Tokens per second: 11406.84410646388
     */
    @Test
    public void testInsideOutsideSpeed() {
        FastMath.useLogAddTable = true;

        int trials = 1000;
        int n = 30;

        // Just create one tree.
        double[] root = Multinomials.randomMultinomial(n);
        double[][] child = new double[n][];
        for (int i=0; i<n; i++) {
            child[i] =  Multinomials.randomMultinomial(n);
        }
        
        Timer timer = new Timer();
        timer.start();
        for (int t=0; t<trials; t++) {
            ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);
        }
        timer.stop();
        System.out.println("Total time: " + timer.totMs());
        int numSents = trials;
        int numTokens = n * numSents;
        System.out.println("Sentences per second: " + numSents / timer.totSec());
        System.out.println("Tokens per second: " + numTokens / timer.totSec());
        FastMath.useLogAddTable = false;
    }
        
    @Test
    public void testParse1() {
        double[] root = new double[] {1, 2.2, 3}; 
        double[][] child = new double[][]{ {0, 5, 6}, {80, 0, 90}, {11, 12, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.parse(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(172.2, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testParse2() {
        // [0.0, 0.75, 0.25]
        // [[0.0, 0.0, 0.75], [1.0, 0.0, 0.0], [0.0, 0.25, 0.0]]
        double[] root = new double[] {0.0, 0.75, 0.25};
        double[][] child = new double[][]{ {0.0, 0.0, 0.75}, {1.0, 0.0, 0.0}, {0.0, 0.25, 0.0} };
        int[] parents = new int[3];    
        double score = ProjectiveDependencyParser.parse(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(0.0 + 1.0 + 0.75, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testVineParse1() {
        double[] root = new double[] {1, 20, 3}; 
        double[][] child = new double[][]{ {0, 5, 6}, {80, 0, 90}, {11, 12, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.vineParse(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(190, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testVineParse2() {
        // [0.0, 0.75, 0.25]
        // [[0.0, 0.0, 0.75], [1.0, 0.0, 0.0], [0.0, 0.25, 0.0]]
        double[] root = new double[] {0.0, 0.75, 0.25};
        double[][] child = new double[][]{ {0.0, 0.0, 0.75}, {1.0, 0.0, 0.0}, {0.0, 0.25, 0.0} };
        int[] parents = new int[3];    
        double score = ProjectiveDependencyParser.vineParse(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(0.75 + 1.0 + 0.25, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, -1}, parents);
    }
    
    @Test
    public void testMaxProjSpanTree1() {
        double[][] scores = new double[][]{ {0, 1, 2}, {3, 0, 5}, {70, 90, 0} };
        int[] parents = new int[3];
        double score = ProjectiveDependencyParser.maxProjSpanTree(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(160.0, score, 1e-13);
        // TODO: Note that this wouldn't be a valid dependency tree (assumming
        // the wall is the rightmost node). We will need some special case
        // handling to obtain a proper parse where the wall is the root and has
        // only one child.
        JUnitUtils.assertArrayEquals(new int[] { 2, 2, -1 }, parents);
    }    

    @Test
    public void testMaxProjSpanTree2() {
        double[][] scores = new double[][]{ {0, 1.1, 2}, {30, 0, 50}, {7, 9, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.maxProjSpanTree(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(80, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }

    @Test
    public void testMaxProjSpanTree3() {
        double[][] scores = new double[][]{ {0, 10, 2}, {3, 0, 50}, {7, 9, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.maxProjSpanTree(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(60.0, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1}, parents);
    }    

    @Test
    public void testMaxProjSpanTree4() {
        double[][] scores = new double[][]{ {0, 1, 20, 3}, {4, 0, 5, 6}, {7, 80, 0, 90}, {10, 11, 12, 0} };
        int[] parents = new int[4];        
        double score = ProjectiveDependencyParser.maxProjSpanTree(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(190.0, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{-1, 2, 0, 2}, parents);
    }
    
    @Test
    public void testMaxProjSpanTree5() {
        double[][] scores = new double[][]{ {0, 1, 2, 3}, {4, 0, 5, 6}, {70, 80, 0, 90}, {10, 11, 12, 0} };
        int[] parents = new int[4];        
        double score = ProjectiveDependencyParser.maxProjSpanTree(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(240, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{2, 2, -1, 2}, parents);
    }
    
    @Test
    public void testInsideOutside1() {
        double[] root = new double[] {1, 2, 3}; 
        double[][] child = new double[][]{ {0, 4, 5}, {6, 0, 7}, {8, 9, 0} };
        
        DoubleArrays.log(root);
        DoubleArrays.log(child);
        
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideAlgorithm(root,  child);
        
        // Check inside scores.
        assertEquals(7, FastMath.exp(chart.getLogInsideScore(1, 2)), 1e-13);
        assertEquals(9, FastMath.exp(chart.getLogInsideScore(2, 1)), 1e-13);
        assertEquals(45+20, FastMath.exp(chart.getLogInsideScore(0, 2)), 1e-13);
        assertEquals(45+28+20, FastMath.exp(chart.getLogInsideScore(-1, 0)), 1e-13);
        assertEquals(84, FastMath.exp(chart.getLogInsideScore(-1, 1)), 1e-13);
        assertEquals(162+216+96, FastMath.exp(chart.getLogInsideScore(-1, 2)), 1e-3);
        
        // Check partition function.
        assertEquals(45+28+20+84+162+216+96, FastMath.exp(chart.getLogPartitionFunction()), 1e-3);
        
        // Check outside scores.
        assertEquals(1*4+2*6, FastMath.exp(chart.getLogOutsideScore(1, 2)), 1e-13);
        assertEquals(1*5+3*6+3*8, FastMath.exp(chart.getLogOutsideScore(2, 1)), 1e-13); // why is this 3*6 and not just 3?
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(0, 2)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 0)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 1)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 2)), 1e-3);
        assertEquals(2*7+3*9, FastMath.exp(chart.getLogOutsideScore(1, 0)), 1e-3);

        // Check sums.
        assertEquals(28+84, FastMath.exp(chart.getLogSumOfPotentials(1, 2)), 1e-3);
        assertEquals(45+162+216, FastMath.exp(chart.getLogSumOfPotentials(2, 1)), 1e-3);
        assertEquals(28+20+96, FastMath.exp(chart.getLogSumOfPotentials(0, 1)), 1e-3);
        assertEquals(96+216, FastMath.exp(chart.getLogSumOfPotentials(2, 0)), 1e-3);
        
        // Check expected counts.
        double Z = 45+28+20+84+162+216+96;
        assertEquals((28+84)/Z, FastMath.exp(chart.getLogExpectedCount(1, 2)), 1e-3);
        assertEquals((45+162+216)/Z, FastMath.exp(chart.getLogExpectedCount(2, 1)), 1e-3);
        assertEquals((28+20+96)/Z, FastMath.exp(chart.getLogExpectedCount(0, 1)), 1e-3);
        assertEquals((96+216)/Z, FastMath.exp(chart.getLogExpectedCount(2, 0)), 1e-3);        
    }
    
}
