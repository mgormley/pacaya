package edu.jhu.hypergraph;

import java.util.Arrays;

import edu.jhu.hypergraph.Hypergraph.HyperedgeFn;
import edu.jhu.util.semiring.Semiring;

public class Hyperalgo {

    private Hyperalgo() {
        // Private constructor.
    }
    
    /**
     * Runs the inside algorithm on a hypergraph.
     * 
     * @param graph The hypergraph.
     * @return The beta value for each Hypernode. Where beta[i] is the inside
     *         score for the i'th node in the Hypergraph:
     *         graph.getNodes().get(i).
     */
    public double[] insideAlgorithm(Hypergraph graph, Semiring s) {
        final int n = graph.getNodes().size();
        final double[] beta = new double[n];
        Arrays.fill(beta, s.zero());
        graph.applyTopoSort(new HyperedgeFn() {

            @Override
            public void apply(Hyperedge e) {
                // TODO Auto-generated method stub
                
            }
            
        });
        return beta;
    }
    
}
