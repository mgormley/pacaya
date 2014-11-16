package edu.jhu.hypergraph.depparse;

import edu.jhu.hypergraph.AbstractHypergraphTest;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.RealAlgebra;

public class O1DpHypergraphMultiRootTest extends AbstractHypergraphTest {

    RealAlgebra s = Algebras.REAL_ALGEBRA;

    protected O1DpHypergraph getHypergraph() {
        double[] root = new double[] {1, 2, 3}; 
        double[][] child = new double[][]{ {0, 4, 5}, {6, 0, 7}, {8, 9, 0} };        
        O1DpHypergraph graph = new O1DpHypergraph(root, child, s, false);
        return graph;
    }

}
