package edu.jhu.hypergraph.depparse;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.Hypergraph;
import edu.jhu.hypergraph.Hyperpotential;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraph.DependencyScorer;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.RealAlgebra;

public class O2AllGraDpHypergraphTest extends AbstractHypergraphTest {

    RealAlgebra s = Algebras.REAL_ALGEBRA;

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
        
        O2AllGraDpHypergraph graph = new O2AllGraDpHypergraph(scorer, s, true);
        return graph;
    }
    
    public static class ExplicitDependencyScorer implements DependencyScorer {

        private double[][][] scores;
        private int n;
                
        public ExplicitDependencyScorer(double[][][] scores, int n) {
            super();
            this.scores = scores;
            this.n = n;
        }

        @Override
        public double getScore(int p, int c, int g) {
            return scores[p][c][g];
        }

        @Override
        public int getNumTokens() {
            return n;
        }
        
    }

}
