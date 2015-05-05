package edu.jhu.pacaya.hypergraph.depparse;

import edu.jhu.pacaya.hypergraph.AbstractHypergraphTest;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class O1DpHypergraphMultiRootTest extends AbstractHypergraphTest {

    RealAlgebra s = RealAlgebra.REAL_ALGEBRA;

    protected O1DpHypergraph getHypergraph() {
        double[] root = new double[] {1, 2, 3}; 
        double[][] child = new double[][]{ {0, 4, 5}, {6, 0, 7}, {8, 9, 0} };        
        O1DpHypergraph graph = new O1DpHypergraph(root, child, s, false);
        return graph;
    }

}
