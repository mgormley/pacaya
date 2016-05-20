package edu.jhu.pacaya.hypergraph.depparse;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.hypergraph.AbstractHypergraphTest;
import edu.jhu.pacaya.hypergraph.Hyperalgo;
import edu.jhu.pacaya.hypergraph.Hyperalgo.Scores;
import edu.jhu.pacaya.hypergraph.Hypernode;
import edu.jhu.pacaya.hypergraph.Hyperpotential;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;

public class O2AllGraDpHypergraphTest extends AbstractHypergraphTest {

    RealAlgebra s = RealAlgebra.getInstance();

    @Test
    public void testInsideOutsideSingleRoot1Word() {
        boolean singleRoot = true;
        checkInsideOutside1Word(singleRoot);        
    }
    
    @Test
    public void testInsideOutsideMultiRoot1Word() {
        boolean singleRoot = false;
        checkInsideOutside1Word(singleRoot);        
    }

    protected void checkInsideOutside1Word(boolean singleRoot) {
        double[][][] scores = new double[2][2][2];//{{{1.0, 1.0}, {1.0, 1.0}}, {{2.0, 1.0}, {2.0, 1.0}}};
        DoubleArrays.fill(scores, 1.0);
        scores[0][1][0] = 2.0;
        DependencyScorer scorer = new ExplicitDependencyScorer(scores, 1);
        O2AllGraDpHypergraph graph = new O2AllGraDpHypergraph(scorer, s, singleRoot);
        for (Hypernode node : graph.getNodes()) {
            System.out.println(node.getLabel());
        }
        
        Scores sc = new Scores();
        Hyperpotential w = graph.getPotentials();
        System.out.println("Inside:");
        Hyperalgo.insideAlgorithm(graph, w, s, sc);
        System.out.println("Outside:");
        Hyperalgo.outsideAlgorithm(graph, w, s, sc);
        System.out.println("Marginals:");
        Hyperalgo.marginals(graph, w, s, sc);
        
        assertEquals(2, sc.beta[graph.getRoot().getId()], 1e-13);
        
        int id = graph.getChart()[0][1][0][O2AllGraDpHypergraph.INCOMPLETE].getId();
        assertEquals(2, sc.beta[id], 1e-13);
        assertEquals(1, sc.alpha[id], 1e-13);
        assertEquals(1, sc.marginal[id], 1e-13);
    }
    
    @Test
    public void testTreeCountSingleRoot() {
        assertEquals(1, countTrees(1, true, 1.0), 1e-13);
        assertEquals(2, countTrees(2, true, 1.0), 1e-13);
        assertEquals(7, countTrees(3, true, 1.0), 1e-13);
        assertEquals(30, countTrees(4, true, 1.0), 1e-13);
        assertEquals(143, countTrees(5, true, 1.0), 1e-13);
        assertEquals(728, countTrees(6, true, 1.0), 1e-13);        
    }

    @Test
    public void testTreeCountMultiRoot() {
        assertEquals(1, countTrees(1, false, 1.0), 1e-13);
        assertEquals(3, countTrees(2, false, 1.0), 1e-13);
        assertEquals(12, countTrees(3, false, 1.0), 1e-13);
        assertEquals(55, countTrees(4, false, 1.0), 1e-13);
        assertEquals(273, countTrees(5, false, 1.0), 1e-13);
        assertEquals(1428, countTrees(6, false, 1.0), 1e-13);
    }

    @Test
    public void testTreeCountSingleRootWeight2() {
        double edgeWeight = 2.0;
        assertEquals(1 * Math.pow(edgeWeight, 1), countTrees(1, true, edgeWeight), 1e-13);
        assertEquals(2 * Math.pow(edgeWeight, 2), countTrees(2, true, edgeWeight), 1e-13);
        assertEquals(7 * Math.pow(edgeWeight, 3), countTrees(3, true, edgeWeight), 1e-13);
        assertEquals(30 * Math.pow(edgeWeight, 4), countTrees(4, true, edgeWeight), 1e-10);
        assertEquals(143 * Math.pow(edgeWeight, 5), countTrees(5, true, edgeWeight), 1e-10);
        assertEquals(728 * Math.pow(edgeWeight, 6), countTrees(6, true, edgeWeight), 1e-8);
    }
    
    @Test
    public void testTreeCountMultiRootWeight2() {
        double edgeWeight = 2.0;
        assertEquals(1 * Math.pow(edgeWeight, 1), countTrees(1, false, edgeWeight), 1e-13);
        assertEquals(3 * Math.pow(edgeWeight, 2), countTrees(2, false, edgeWeight), 1e-13);
        assertEquals(12 * Math.pow(edgeWeight, 3), countTrees(3, false, edgeWeight), 1e-13);
        assertEquals(55 * Math.pow(edgeWeight, 4), countTrees(4, false, edgeWeight), 1e-10);
        assertEquals(273 * Math.pow(edgeWeight, 5), countTrees(5, false, edgeWeight), 1e-10);
        assertEquals(1428 * Math.pow(edgeWeight, 6), countTrees(6, false, edgeWeight), 1e-8);
    }
    
    /** Returns the partition function from running the inside algorithm on a sentence of length n with edge weights given.*/
    protected double countTrees(int n, boolean singleRoot, double edgeWeight) {
        double[][][] scores = new double[n+1][n+1][n+1];
        DoubleArrays.fill(scores, edgeWeight);
        DependencyScorer scorer = new ExplicitDependencyScorer(scores, n);
        O2AllGraDpHypergraph graph = new O2AllGraDpHypergraph(scorer, s, singleRoot);
        for (Hypernode node : graph.getNodes()) {
            System.out.println(node.getLabel());
        }
        
        Scores sc = new Scores();
        Hyperpotential w = graph.getPotentials();
        System.out.println("Inside:");
        Hyperalgo.insideAlgorithm(graph, w, s, sc);
        System.out.println("Outside:");
        Hyperalgo.outsideAlgorithm(graph, w, s, sc);
        System.out.println("Marginals:");
        Hyperalgo.marginals(graph, w, s, sc);
        
        return sc.beta[graph.getRoot().getId()];        
    }
    
    @Test
    public void testInsideOutsideSingleRoot() {
        O2AllGraDpHypergraph graph = getHypergraph();
        for (Hypernode node : graph.getNodes()) {
            System.out.println(node.getLabel());
        }
        
        Scores sc = new Scores();
        Hyperpotential w = graph.getPotentials();
        System.out.println("Inside:");
        Hyperalgo.insideAlgorithm(graph, w, s, sc);
        System.out.println("Outside:");
        Hyperalgo.outsideAlgorithm(graph, w, s, sc);
        System.out.println("Marginals:");
        Hyperalgo.marginals(graph, w, s, sc);
        
        assertEquals(279, sc.beta[graph.getRoot().getId()], 1e-13);
        
        Hypernode[][][][] c = graph.getChart();
        int id;
        id = c[1][3][0][0].getId();
        assertEquals(3*4 + 2*3, sc.beta[id], 1e-13);
        assertEquals(3*4 + 2*3, sc.alpha[id] * sc.beta[id], 1e-13);
        assertEquals((3*4 + 2*3)/279., sc.marginal[id], 1e-13);
        
        id = c[2][3][1][0].getId();
        assertEquals(11, sc.beta[id], 1e-13);
        assertEquals(2*11, sc.alpha[id] * sc.beta[id], 1e-13);
        assertEquals((2*11)/279., sc.marginal[id], 1e-13);
    }

    protected O2AllGraDpHypergraph getHypergraph() {
        return new O2AllGraDpHypergraph(getScorer(), s, true);
    }

    public static DependencyScorer getScorer() {
        final double[][][] scores = new double[4][4][4];
        DoubleArrays.fill(scores, Double.NaN);
        scores[0][1][2] = 2;
        scores[1][2][3] = 11;
        scores[0][1][3] = 3;
        scores[1][3][2] = 4;
        scores[0][2][1] = 5;
        scores[0][2][3] = 6;
        scores[0][3][2] = 7;
        scores[3][2][1] = 8;
        scores[0][3][1] = 9;
        scores[3][1][2] = 10;
        for (int c=1; c<4; c++) {
            scores[0][0][c] = 1.0;
        }
        
        DependencyScorer scorer = new DependencyScorer() {            
            @Override
            public double getScore(int p, int c, int g) {
                System.out.printf("Score request: g=%d p=%d c=%d score=%f\n", g, p, c, scores[g][p][c]);
                return scores[g][p][c];
            }
            @Override
            public int getNumTokens() {
                return 3;
            }
        };
        return scorer;
    }

}
