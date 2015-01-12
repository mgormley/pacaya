package edu.jhu.parse.dep;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.hypergraph.depparse.HyperDepParser;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Timer;

public class ProjectiveDependencyParserTest {
    
    @Test
    public void testParseSingleRoot1() {
        double[] root = new double[] {1, 2.2, 3}; 
        double[][] child = new double[][]{ {0, 5, 6}, {80, 0, 90}, {11, 12, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.parseSingleRoot(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(172.2, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testParseSingleRoot2() {
        // [0.0, 0.75, 0.25]
        // [[0.0, 0.0, 0.75], [1.0, 0.0, 0.0], [0.0, 0.25, 0.0]]
        double[] root = new double[] {0.0, 0.75, 0.25};
        double[][] child = new double[][]{ {0.0, 0.0, 0.75}, {1.0, 0.0, 0.0}, {0.0, 0.25, 0.0} };
        int[] parents = new int[3];    
        double score = ProjectiveDependencyParser.parseSingleRoot(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(0.0 + 1.0 + 0.75, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testParseMultiRoot1() {
        double[] root = new double[] {1, 20, 3}; 
        double[][] child = new double[][]{ {0, 5, 6}, {80, 0, 90}, {11, 12, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.parseMultiRoot(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(190, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testParseMultiRoot2() {
        // [0.0, 0.75, 0.25]
        // [[0.0, 0.0, 0.75], [1.0, 0.0, 0.0], [0.0, 0.25, 0.0]]
        double[] root = new double[] {0.0, 0.75, 0.25};
        double[][] child = new double[][]{ {0.0, 0.0, 0.75}, {1.0, 0.0, 0.0}, {0.0, 0.25, 0.0} };
        int[] parents = new int[3];    
        double score = ProjectiveDependencyParser.parseMultiRoot(root, child, parents);
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
    public void testInsideOutsideSingleRoot1() {
        double[] root = new double[] {1, 2, 3}; 
        double[][] child = new double[][]{ {0, 4, 5}, {6, 0, 7}, {8, 9, 0} };
        
        DoubleArrays.log(root);
        DoubleArrays.log(child);
        
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideSingleRoot(root,  child);
        
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
    
    @Test
    public void testInsideOutsideSingleRoot2() {
        double[] root = new double[] {1, 1, 1}; 
        double[][] child = new double[][]{ {0, 1, 1}, {1, 0, 1}, {1, 1, 0} };
        
        DoubleArrays.log(root);
        DoubleArrays.log(child);
        
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideSingleRoot(root,  child);

        // Check partition function.
        assertEquals(7, FastMath.exp(chart.getLogPartitionFunction()), 1e-3);
        
        // Check inside scores.
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(1, 2)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(2, 1)), 1e-13);
        assertEquals(1+1, FastMath.exp(chart.getLogInsideScore(0, 2)), 1e-13);
        assertEquals(1+1+1, FastMath.exp(chart.getLogInsideScore(-1, 0)), 1e-10);
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(-1, 1)), 1e-13);
        assertEquals(1+1+1, FastMath.exp(chart.getLogInsideScore(-1, 2)), 1e-3);
        
        // Check outside scores.
        assertEquals(1+1, FastMath.exp(chart.getLogOutsideScore(1, 2)), 1e-13);
        assertEquals(1+1+1, FastMath.exp(chart.getLogOutsideScore(2, 1)), 1e-13); // why is this 3*6 and not just 3?
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(0, 2)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 0)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 1)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 2)), 1e-3);
        assertEquals(1+1, FastMath.exp(chart.getLogOutsideScore(1, 0)), 1e-3);

        // Check sums.
        assertEquals(1+1, FastMath.exp(chart.getLogSumOfPotentials(1, 2)), 1e-3);
        assertEquals(1+1+1, FastMath.exp(chart.getLogSumOfPotentials(2, 1)), 1e-3);
        assertEquals(1+1+1, FastMath.exp(chart.getLogSumOfPotentials(0, 1)), 1e-3);
        assertEquals(1+1, FastMath.exp(chart.getLogSumOfPotentials(2, 0)), 1e-3);
        
        // Check expected counts.
        double Z = 7;
        assertEquals((1+1)/Z, FastMath.exp(chart.getLogExpectedCount(1, 2)), 1e-3);
        assertEquals((1+1+1)/Z, FastMath.exp(chart.getLogExpectedCount(2, 1)), 1e-3);
        assertEquals((1+1+1)/Z, FastMath.exp(chart.getLogExpectedCount(0, 1)), 1e-3);
        assertEquals((1+1)/Z, FastMath.exp(chart.getLogExpectedCount(2, 0)), 1e-3);        
    }

    @Test
    public void testInsideOutsideMultiRoot1() {
        double[] root = new double[] {1, 2, 3}; 
        double[][] child = new double[][]{ {0, 4, 5}, {6, 0, 7}, {8, 9, 0} };
        
        DoubleArrays.log(root);
        DoubleArrays.log(child);
        
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideMultiRoot(root,  child);

        // Check partition function.
        double multiTrees = 6+14+12+36+27;
        double Z = 45+28+20+84+162+216+96+multiTrees;
        assertEquals(Z, FastMath.exp(chart.getLogPartitionFunction()), 1e-3);
                        
        // Check inside scores.
        assertEquals(7, FastMath.exp(chart.getLogInsideScore(1, 2)), 1e-13);
        assertEquals(9, FastMath.exp(chart.getLogInsideScore(2, 1)), 1e-13);
        assertEquals(45+20, FastMath.exp(chart.getLogInsideScore(0, 2)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(-1, 0)), 1e-10);
        assertEquals(14, FastMath.exp(chart.getLogInsideScore(-1, 1)), 1e-13);
        assertEquals(8*9+8*4, FastMath.exp(chart.getLogInsideScore(2, 0)), 1e-10);
        assertEquals(555, FastMath.exp(chart.getLogInsideScore(-1, 2)), 1e-3);

        // Check outside scores.
        assertEquals(18, FastMath.exp(chart.getLogOutsideScore(1, 2)), 1e-13);
        assertEquals(50, FastMath.exp(chart.getLogOutsideScore(2, 1)), 1e-13); // why is this 3*6 and not just 3?
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(0, 2)), 1e-13);
        assertEquals(152, FastMath.exp(chart.getLogOutsideScore(-1, 0)), 1e-13);
        assertEquals(10, FastMath.exp(chart.getLogOutsideScore(-1, 1)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 2)), 1e-3);
        assertEquals(47, FastMath.exp(chart.getLogOutsideScore(1, 0)), 1e-3);

        // Check sums.
        assertEquals(6+14+12+27+28+45+20, FastMath.exp(chart.getLogSumOfPotentials(-1, 0)), 1e-10);
        assertEquals(6+14+36+84, FastMath.exp(chart.getLogSumOfPotentials(-1, 1)), 1e-13);
        assertEquals(6+12+36+27+54*3+32*3+72*3, FastMath.exp(chart.getLogSumOfPotentials(-1, 2)), 1e-3);
        assertEquals(126, FastMath.exp(chart.getLogSumOfPotentials(1, 2)), 1e-3);
        assertEquals(27+45+54*3+72*3, FastMath.exp(chart.getLogSumOfPotentials(2, 1)), 1e-3);
        assertEquals(12+28+20+32*3, FastMath.exp(chart.getLogSumOfPotentials(0, 1)), 1e-3);
        assertEquals(312, FastMath.exp(chart.getLogSumOfPotentials(2, 0)), 1e-3);
        
        // Check expected counts.
        assertEquals((126)/Z, FastMath.exp(chart.getLogExpectedCount(1, 2)), 1e-3);
        assertEquals((27+45+54*3+72*3)/Z, FastMath.exp(chart.getLogExpectedCount(2, 1)), 1e-3);
        assertEquals((12+28+20+32*3)/Z, FastMath.exp(chart.getLogExpectedCount(0, 1)), 1e-3);
        assertEquals((312)/Z, FastMath.exp(chart.getLogExpectedCount(2, 0)), 1e-3);        
    }    

    @Test
    public void testInsideOutsideMultiRoot2() {
        double[] root = new double[] {1, 1, 1}; 
        double[][] child = new double[][]{ {0, 1, 1}, {1, 0, 1}, {1, 1, 0} };
        
        DoubleArrays.log(root);
        DoubleArrays.log(child);
        
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideMultiRoot(root,  child);

        // Check partition function.
        assertEquals(7+5, FastMath.exp(chart.getLogPartitionFunction()), 1e-3);
                       
        // Check inside scores.
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(1, 2)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(2, 1)), 1e-13);
        assertEquals(2, FastMath.exp(chart.getLogInsideScore(0, 2)), 1e-13);
        assertEquals(1, FastMath.exp(chart.getLogInsideScore(-1, 0)), 1e-10);
        assertEquals(2, FastMath.exp(chart.getLogInsideScore(-1, 1)), 1e-13);
        assertEquals(7, FastMath.exp(chart.getLogInsideScore(-1, 2)), 1e-3);
        
        // Check outside scores.
        assertEquals(3, FastMath.exp(chart.getLogOutsideScore(1, 2)), 1e-13);
        assertEquals(4, FastMath.exp(chart.getLogOutsideScore(2, 1)), 1e-13);  // checked
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(0, 2)), 1e-13);
        assertEquals(7, FastMath.exp(chart.getLogOutsideScore(-1, 0)), 1e-13); // checked
        assertEquals(2, FastMath.exp(chart.getLogOutsideScore(-1, 1)), 1e-13); // checked
        assertEquals(1, FastMath.exp(chart.getLogOutsideScore(-1, 2)), 1e-3);  // checked
        assertEquals(3, FastMath.exp(chart.getLogOutsideScore(1, 0)), 1e-3);   // checked

        // Check sums.
        assertEquals(3, FastMath.exp(chart.getLogSumOfPotentials(1, 2)), 1e-3);
        assertEquals(4, FastMath.exp(chart.getLogSumOfPotentials(2, 1)), 1e-3); // checked
        assertEquals(4, FastMath.exp(chart.getLogSumOfPotentials(0, 1)), 1e-3); // checked
        assertEquals(2, FastMath.exp(chart.getLogSumOfPotentials(2, 0)), 1e-3); // checked
        
        // Check expected counts.
        double Z = 7+5;
        assertEquals((3)/Z, FastMath.exp(chart.getLogExpectedCount(1, 2)), 1e-3);
        assertEquals((4)/Z, FastMath.exp(chart.getLogExpectedCount(2, 1)), 1e-3);
        assertEquals((4)/Z, FastMath.exp(chart.getLogExpectedCount(0, 1)), 1e-3);
        assertEquals((2)/Z, FastMath.exp(chart.getLogExpectedCount(2, 0)), 1e-3);        
    }
    
    @Test
    public void testInsideOutsideMultiRoot3() {
        double[] root = new double[] {1, 1}; 
        double[][] child = new double[][]{ {0, 1}, {1, 0} };
        
        DoubleArrays.log(root);
        DoubleArrays.log(child);
        
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideMultiRoot(root,  child);

        // Check partition function.
        assertEquals(3, FastMath.exp(chart.getLogPartitionFunction()), 1e-3);        
    }
    
}
