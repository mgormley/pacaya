package edu.jhu.hltcoe.parse.dep;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.hltcoe.util.JUnitUtils;

public class ProjectiveDependencyParserTest {

    @Test
    public void testParse() {
        double[] root = new double[] {1, 2.2, 3}; 
        double[][] child = new double[][]{ {0, 5, 6}, {80, 0, 90}, {11, 12, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.parse(root, child, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(172.2, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testMaxProjSpanTree1() {
        double[][] scores = new double[][]{ {0, 1, 2}, {3, 0, 5}, {70, 90, 0} };
        int[] parents = new int[3];
        double score = ProjectiveDependencyParser.parse(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(160.0, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{2, 2, -1}, parents);
    }    

    @Test
    public void testMaxProjSpanTree2() {
        double[][] scores = new double[][]{ {0, 1.1, 2}, {30, 0, 50}, {7, 9, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.parse(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(51.1, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1}, parents);
    }

    @Test
    public void testMaxProjSpanTree3() {
        double[][] scores = new double[][]{ {0, 10, 2}, {3, 0, 50}, {7, 9, 0} };
        int[] parents = new int[3];        
        double score = ProjectiveDependencyParser.parse(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(60.0, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1}, parents);
    }    

    @Test
    public void testMaxProjSpanTree4() {
        double[][] scores = new double[][]{ {0, 1, 20, 3}, {4, 0, 5, 6}, {7, 80, 0, 90}, {10, 11, 12, 0} };
        int[] parents = new int[4];        
        double score = ProjectiveDependencyParser.parse(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(190.0, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{-1, 2, 0, 2}, parents);
    }
    
    @Test
    public void testMaxProjSpanTree5() {
        double[][] scores = new double[][]{ {0, 1, 2.2, 3}, {4, 0, 5, 6}, {70, 80, 0, 90}, {10, 11, 12, 0} };
        int[] parents = new int[4];        
        double score = ProjectiveDependencyParser.parse(scores, parents);
        System.out.println(Arrays.toString(parents));        
        assertEquals(172.2, score, 1e-13);
        JUnitUtils.assertArrayEquals(new int[]{-1, 2, 0, 2}, parents);
    }
}
