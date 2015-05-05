package edu.jhu.pacaya.hypergraph.depparse;

import edu.jhu.pacaya.hypergraph.AbstractHypergraphTest;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class O2AllGraDpHypergraphMultiRootTest extends AbstractHypergraphTest {

    RealAlgebra s = RealAlgebra.REAL_ALGEBRA;

    protected O2AllGraDpHypergraph getHypergraph() {
        return new O2AllGraDpHypergraph(O2AllGraDpHypergraphTest.getScorer(), s, false);
    }

}
